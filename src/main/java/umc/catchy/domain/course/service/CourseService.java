package umc.catchy.domain.course.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.response.CourseDetailResponse;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.course.dto.response.GptPlaceInfoResponse;
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

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Slice;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private static final String MIDNIGHT_STRING = "24:00";
    private static final String TIME_RANGE_DELIMITER = "~";

    private final CourseRepository courseRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final PlaceCourseRepository placeCourseRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final MemberRepository memberRepository;
    private final MemberCourseRepository memberCourseRepository;
    private final PlaceRepository placeRepository;

    public List<Long> getAllMemberIds() {
        return memberRepository.findAllMemberIds();
    }

    public Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }

    public CourseDetailResponse getCourseDetails(Long courseId) {
        Course course = getCourse(courseId);
        Member member = getCurrentMember();

        List<CourseDetailResponse.CoursePlaceInfo> placeListOfCourse = getPlaceListOfCourse(course, member);

        return CourseDetailResponse.from(
                course,
                calculateNumberOfReviews(course),
                getBookmarks(course, member),
                placeListOfCourse
        );
    }

    public MemberCourseSliceResponse getMemberCourses(CourseType courseType, String upperLocation,
                                                      String lowerLocation, Long lastId) {
        Member member = getCurrentMember();

        Slice<MemberCourseResponse> responses = memberCourseRepository.findCourseByFilters(
                courseType, upperLocation, lowerLocation, member.getId(), lastId
        );

        return MemberCourseSliceResponse.from(responses);
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

    private Pair<LocalTime, LocalTime> parseRecommendTime(String recommendTime) {
        try {
            String[] times = recommendTime.split(TIME_RANGE_DELIMITER);
            LocalTime startTime = LocalTime.parse(times[0].trim());
            LocalTime endTime;

            if (times[1].trim().equals(MIDNIGHT_STRING)) {
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
        List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourseWithPlace(course);

        List<Long> placeIds = placeCourses.stream()
                .map(pc -> pc.getPlace().getId())
                .toList();

        Map<Long, Boolean> visitMap = placeVisitRepository
                .findAllByPlaceIdsAndMember(placeIds, member)
                .stream()
                .collect(Collectors.toMap(
                        pv -> pv.getPlace().getId(),
                        PlaceVisit::isVisited
                ));

        return placeCourses.stream()
                .map(pc -> {
                    Boolean isVisited = visitMap.getOrDefault(pc.getPlace().getId(), false);
                    return CourseDetailResponse.CoursePlaceInfo.of(pc.getPlace(), isVisited);
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

    private Member getCurrentMember() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
