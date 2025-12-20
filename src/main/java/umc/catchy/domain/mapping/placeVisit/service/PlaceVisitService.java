package umc.catchy.domain.mapping.placeVisit.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceVisitedResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceVisitService {

    private final PlaceVisitRepository placeVisitRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;
    private final CourseRepository courseRepository;
    private final MemberCourseRepository memberCourseRepository;
    private final PlaceCourseRepository placeCourseRepository;

    @Transactional
    public PlaceVisitedResponse check(Long courseId, Long placeId) {
        Member member = getCurrentMember();
        Course course = getCourse(courseId);
        Place place = getPlace(placeId);
        MemberCourse memberCourse = getMemberCourse(course, member);

        validateNotAlreadyVisitedToday(place, member, course);

        PlaceVisit placeVisit = createPlaceVisit(course, place, member);

        checkAndUpdateCourseCompletion(course, member, memberCourse);

        return PlaceVisitedResponse.of(placeVisit.getId(), placeVisit.isVisited());
    }

    private void validateNotAlreadyVisitedToday(Place place, Member member, Course course) {
        placeVisitRepository
                .findByPlaceAndMemberAndCourseAndVisitedDate(place, member, course, LocalDate.now())
                .ifPresent(pv -> {
                    throw new GeneralException(ErrorStatus.PLACE_VISIT_ALREADY_CHECK);
                });
    }

    private PlaceVisit createPlaceVisit(Course course, Place place, Member member) {
        PlaceVisit placeVisit = PlaceVisit.builder()
                .course(course)
                .place(place)
                .member(member)
                .isVisited(true)
                .visitedDate(LocalDate.now())
                .build();

        return placeVisitRepository.save(placeVisit);
    }

    private void checkAndUpdateCourseCompletion(Course course, Member member, MemberCourse memberCourse) {
        List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourseWithPlace(course);
        List<PlaceVisit> visits = placeVisitRepository.findAllByCourseAndMemberWithPlace(course, member);

        Set<Long> visitedPlaceIds = visits.stream()
                .map(pv -> pv.getPlace().getId())
                .collect(Collectors.toSet());

        long visitedCount = placeCourses.stream()
                .filter(pc -> visitedPlaceIds.contains(pc.getPlace().getId()))
                .count();

        int requiredVisits = (int) Math.round((double) placeCourses.size() / 2);

        if (visitedCount == requiredVisits) {
            memberCourse.markAsVisited(LocalDate.now());
            course.increaseParticipants();
        }
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }

    private Place getPlace(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));
    }

    private MemberCourse getMemberCourse(Course course, Member member) {
        return memberCourseRepository.findByCourseAndMember(course, member)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_INVALID_MEMBER));
    }

    private Member getCurrentMember() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
