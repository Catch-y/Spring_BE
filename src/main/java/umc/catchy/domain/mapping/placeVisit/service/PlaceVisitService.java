package umc.catchy.domain.mapping.placeVisit.service;

import java.time.LocalDate;
import java.util.Optional;
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
import umc.catchy.domain.mapping.placeVisit.converter.PlaceVisitConverter;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceVisitedResponse;
import umc.catchy.domain.mapping.placeVisit.dto.response.PlaceVisitedDateResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PlaceVisitService {
    private final PlaceVisitRepository placeVisitRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;
    private final CourseRepository courseRepository;
    private final MemberCourseRepository memberCourseRepository;
    private final PlaceCourseRepository placeCourseRepository;

    public PlaceVisitedResponse check(Long courseId, Long placeId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        MemberCourse memberCourse = memberCourseRepository.findByCourseAndMember(course, member)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_INVALID_MEMBER));

        // 이미 오늘 방문 체크를 하였다면 예외 처리
        Optional<PlaceVisit> optionalPlaceVisit = placeVisitRepository.findByPlaceAndMemberAndVisitedDate(place, member, LocalDate.now());
        if (optionalPlaceVisit.isPresent()) {
            throw new GeneralException(ErrorStatus.PLACE_VISIT_ALREADY_CHECK);
        }

        // placeVisit 생성
        PlaceVisit placeVisit = PlaceVisitConverter.toPlaceVisit(course, place, member);

        placeVisitRepository.save(placeVisit);

        // 코스 내의 장소 방문이 과반수 이상이면 코스 방문 체크
        List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourse(course);
        int placeNum = placeCourses.size();
        int visitNum = (int) placeCourses.stream()
                .filter(placeCourse -> {
                    Optional<PlaceVisit> optionalVisit = placeVisitRepository.findByPlaceAndMember(placeCourse.getPlace(), member);
                    return optionalVisit.isPresent();
                }).count();

        if (visitNum > placeNum / 2) {
            memberCourse.setVisited(true);
            memberCourse.setVisitedDate(LocalDate.now());
            course.setParticipantsNumber(course.getParticipantsNumber() + 1);
        }

        return PlaceVisitConverter.toPlaceVisitResponse(placeVisit);
    }

    public PlaceVisitedDateResponse getPlaceVisitDate(Long placeId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        Place place = placeRepository.findById(placeId).orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));
        List<PlaceVisit> placeVisitList = placeVisitRepository.findAllByMemberAndPlaceAndIsVisitedTrue(currentMember,place);
        List<LocalDate> visitedDate = placeVisitList.stream().map(PlaceVisit::getVisitedDate).toList();
        return new PlaceVisitedDateResponse(visitedDate);
    }
}
