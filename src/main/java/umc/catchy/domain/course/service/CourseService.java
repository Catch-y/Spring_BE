package umc.catchy.domain.course.service;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.time.format.DateTimeParseException;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.course.dto.response.*;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.request.CourseCreateRequest;
import umc.catchy.domain.course.dto.request.CourseUpdateRequest;
import umc.catchy.domain.courseReview.dao.CourseReviewRepository;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseSliceResponse;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.time.format.DateTimeFormatter;

@Service
@EnableAsync
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final PlaceCourseRepository placeCourseRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final MemberRepository memberRepository;
    private final MemberCourseRepository memberCourseRepository;
    private final AmazonS3Manager amazonS3Manager;
    private final PlaceRepository placeRepository;

    public List<Long> getAllMemberIds() {
        return memberRepository.findAll().stream()
                .map(Member::getId)
                .toList();
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }

    @Transactional
    public void saveCourseAndPlaces(GptCourseInfoResponse parsedResponse, Member member) {
        Pair<LocalTime, LocalTime> recommendTime = parseRecommendTime(parsedResponse.recommendTime());

        Course course = Course.builder()
                .courseName(parsedResponse.courseName())
                .courseDescription(parsedResponse.courseDescription())
                .courseType(CourseType.AI)
                .recommendTimeStart(recommendTime.getLeft())
                .recommendTimeEnd(recommendTime.getRight())
                .courseImage(null)
                .participantsNumber(0L)
                .member(member)
                .build();

        Course savedCourse = courseRepository.save(course);

        List<Long> placeIds = parsedResponse.placeInfos().stream()
                .map(GptPlaceInfoResponse::placeId)
                .toList();

        registerPlacesToCourse(savedCourse, placeIds);

        MemberCourse memberCourse = MemberCourse.builder()
                .course(savedCourse)
                .member(member)
                .build();
        memberCourseRepository.save(memberCourse);
    }

    @Transactional
    public CourseDetailResponse createCourse(CourseCreateRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        String courseImageUrl = null;
        if (request.courseImage() != null) {
            String keyName = "course-images/" + UUID.randomUUID();
            courseImageUrl = amazonS3Manager.uploadFile(keyName, request.courseImage());
        }

        Course course = request.toEntity(member, courseImageUrl);
        Course savedCourse = courseRepository.save(course);

        registerPlacesToCourse(savedCourse, request.placeIds());

        MemberCourse memberCourse = MemberCourse.builder()
                .course(savedCourse)
                .member(member)
                .build();
        memberCourseRepository.save(memberCourse);

        List<CourseDetailResponse.CoursePlaceInfo> placeListOfCourse = getPlaceListOfCourse(savedCourse, member);
        return CourseDetailResponse.from(savedCourse, calculateNumberOfReviews(savedCourse), false, placeListOfCourse);
    }

    @Transactional
    public CourseDetailResponse updateCourse(Long courseId, CourseUpdateRequest request) {
        Course course = getCourse(courseId);
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (!course.getMember().equals(member)) {
            throw new GeneralException(ErrorStatus.COURSE_INVALID_MEMBER);
        }

        course.updateCourseName(request.courseName());
        course.updateCourseDescription(request.courseDescription());

        if (request.courseImage() != null) {
            String originCourseImageUrl = course.getCourseImage();
            if (originCourseImageUrl != null && !originCourseImageUrl.isEmpty()) {
                amazonS3Manager.deleteImage(originCourseImageUrl);
            }

            MultipartFile newCourseImage = request.courseImage();
            String keyName = "course-images/" + UUID.randomUUID();
            String newImageUrl = amazonS3Manager.uploadFile(keyName, newCourseImage);
            course.updateCourseImage(newImageUrl);
        }

        if (!request.placeIds().isEmpty()) {
            List<PlaceCourse> originPlaces = placeCourseRepository.findAllByCourse(course);
            placeCourseRepository.deleteAll(originPlaces);
            placeCourseRepository.flush();

            registerPlacesToCourse(course, request.placeIds());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        if (!request.recommendTimeStart().isEmpty() && !request.recommendTimeEnd().isEmpty()) {
            LocalTime startTime = LocalTime.parse(request.recommendTimeStart(), formatter);
            LocalTime endTime = LocalTime.parse(request.recommendTimeEnd(), formatter);
            course.updateRecommendTime(startTime, endTime);
        }

        List<CourseDetailResponse.CoursePlaceInfo> placeListOfCourse = getPlaceListOfCourse(course, member);
        return CourseDetailResponse.from(course, calculateNumberOfReviews(course), getBookmarks(course, member), placeListOfCourse);
    }

    private void registerPlacesToCourse(Course course, List<Long> placeIds) {
        List<Place> places = placeRepository.findAllById(placeIds);

        Map<Long, Place> placeMap = places.stream()
                .collect(Collectors.toMap(Place::getId, p -> p));

        List<Place> sortedPlaces = placeIds.stream()
                .map(placeMap::get)
                .filter(Objects::nonNull)
                .toList();

        List<PlaceCourse> placeCourses = new ArrayList<>();
        double totalRating = 0.0;
        int validRatingCount = 0;

        for (int i = 0; i < sortedPlaces.size(); i++) {
            Place place = sortedPlaces.get(i);

            placeCourses.add(PlaceCourse.builder()
                    .course(course)
                    .place(place)
                    .placeOrder(i + 1)
                    .build());

            if (place.getRating() != null && place.getRating() > 0) {
                totalRating += place.getRating();
                validRatingCount++;
            }
        }

        placeCourseRepository.saveAll(placeCourses);

        double averageRating = 0.0;
        if (validRatingCount > 0) {
            averageRating = totalRating / validRatingCount;
        }
        course.updateRating(averageRating);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = getCourse(courseId);

        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        MemberCourse memberCourse = memberCourseRepository.findByCourseAndMember(course, member)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_INVALID_MEMBER));

        memberCourseRepository.delete(memberCourse);
        List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourse(course);
        placeCourseRepository.deleteAll(placeCourses);
        courseRepository.delete(course);
    }

    public CourseDetailResponse getCourseDetails(Long courseId) {
        Course course = getCourse(courseId);
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<CourseDetailResponse.CoursePlaceInfo> placeListOfCourse = getPlaceListOfCourse(course, member);

        return CourseDetailResponse.from(course, calculateNumberOfReviews(course), getBookmarks(course, member), placeListOfCourse);
    }

    public MemberCourseSliceResponse getMemberCourses(CourseType courseType, String upperLocation, String lowerLocation, Long lastId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Slice<MemberCourseResponse> responses = memberCourseRepository.findCourseByFilters(courseType, upperLocation, lowerLocation, memberId, lastId);

        return MemberCourseSliceResponse.from(responses);
    }

    private Pair<LocalTime, LocalTime> parseRecommendTime(String recommendTime) {
        try {
            String[] times = recommendTime.split("~");
            LocalTime startTime = LocalTime.parse(times[0].trim());
            LocalTime endTime;

            if (times[1].equals("24:00")) {
                endTime = LocalTime.MIDNIGHT;
            } else {
                endTime = LocalTime.parse(times[1].trim());
            }

            return Pair.of(startTime, endTime);
        } catch (DateTimeParseException e) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO);
        }
    }

    private List<CourseDetailResponse.CoursePlaceInfo> getPlaceListOfCourse(Course course, Member member) {
        return placeCourseRepository.findAllByCourse(course).stream()
                .map(placeCourse -> {
                    Boolean isVisited = placeVisitRepository.findByPlaceAndMember(placeCourse.getPlace(), member)
                            .map(PlaceVisit::isVisited)
                            .orElse(false);
                    return CourseDetailResponse.CoursePlaceInfo.of(placeCourse.getPlace(), isVisited);
                })
                .toList();
    }

    private Integer calculateNumberOfReviews(Course course) {
        if (!course.isHasReview()) {
            return 0;
        }
        return courseReviewRepository.countAllByCourse(course);
    }

    private Boolean getBookmarks(Course course, Member member) {
        return memberCourseRepository.findByCourseAndMember(course, member)
                .map(MemberCourse::isBookmark)
                .orElse(false);
    }
}
