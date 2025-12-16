package umc.catchy.domain.course.service;

import java.time.LocalTime;
import java.util.UUID;
import java.util.stream.IntStream;
import java.time.format.DateTimeParseException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Slice;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@EnableAsync
@Transactional
@RequiredArgsConstructor
public class CourseService {

    @Value("${cache.recommended-courses.key}")
    private String CACHE_KEY;

    @Value("${cache.recommended-courses.ttl}")
    private long CACHE_TTL;

    private final CourseRepository courseRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final PlaceCourseRepository placeCourseRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final MemberRepository memberRepository;
    private final MemberCourseRepository memberCourseRepository;
    private final AmazonS3Manager amazonS3Manager;
    private final PlaceRepository placeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Long> getAllMemberIds() {
        return memberRepository.findAll().stream()
                .map(Member::getId)
                .toList();
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }

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

        int order = 1;
        double totalRating = 0.0;
        int placeCount = 0;

        for (GptPlaceInfoResponse placeInfo : parsedResponse.placeInfos()) {
            Place place = placeRepository.findById(placeInfo.placeId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

            PlaceCourse placeCourse = PlaceCourse.builder()
                    .course(savedCourse)
                    .place(place)
                    .placeOrder(order++)
                    .build();

            placeCourseRepository.save(placeCourse);

            if (place.getRating() != null && place.getRating() > 0) {
                totalRating += place.getRating();
                placeCount++;
            }
        }

        double courseRating = placeCount > 0 ? totalRating / placeCount : 0.0;
        savedCourse.updateRating(courseRating);

        MemberCourse memberCourse = MemberCourse.builder()
                .course(savedCourse)
                .member(member)
                .build();
        memberCourseRepository.save(memberCourse);
    }

    private Pair<LocalTime, LocalTime> parseRecommendTime(String recommendTime) {
        try {
            String[] times = recommendTime.split("~");
            LocalTime startTime = LocalTime.parse(times[0].trim());
            LocalTime endTime = times[1].equals("24:00") ? LocalTime.MIDNIGHT : LocalTime.parse(times[1].trim());
            return Pair.of(startTime, endTime);
        } catch (DateTimeParseException e) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO);
        }
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
            List<Long> placeIds = request.placeIds();
            List<PlaceCourse> originPlaces = placeCourseRepository.findAllByCourse(course);
            placeCourseRepository.deleteAll(originPlaces);

            IntStream.range(0, placeIds.size()).forEach(index -> {
                Long placeId = placeIds.get(index);
                Place place = placeRepository.findById(placeId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

                PlaceCourse newPlaceCourse = PlaceCourse.builder()
                        .course(course)
                        .place(place)
                        .placeOrder(index + 1)
                        .build();
                placeCourseRepository.save(newPlaceCourse);
            });
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

        List<Long> placeIds = request.placeIds();
        Double averageRating = placeIds.stream()
                .map(placeId -> placeRepository.findById(placeId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND))
                        .getRating())
                .filter(rating -> rating != null && rating > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        course.updateRating(averageRating);
        courseRepository.save(course);

        Course finalCourse = course;
        IntStream.range(0, placeIds.size()).forEach(index -> {
            Long placeId = placeIds.get(index);
            Place place = placeRepository.findById(placeId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

            PlaceCourse newPlaceCourse = PlaceCourse.builder()
                    .course(finalCourse)
                    .place(place)
                    .placeOrder(index + 1)
                    .build();
            placeCourseRepository.save(newPlaceCourse);
        });

        MemberCourse memberCourse = MemberCourse.builder()
                .course(course)
                .member(member)
                .build();
        memberCourseRepository.save(memberCourse);

        List<CourseDetailResponse.CoursePlaceInfo> placeListOfCourse = getPlaceListOfCourse(course, member);
        return CourseDetailResponse.from(course, calculateNumberOfReviews(course), false, placeListOfCourse);
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
        return !course.isHasReview() ? 0 : courseReviewRepository.countAllByCourse(course);
    }

    private Boolean getBookmarks(Course course, Member member) {
        return memberCourseRepository.findByCourseAndMember(course, member)
                .map(MemberCourse::isBookmark)
                .orElse(false);
    }

}
