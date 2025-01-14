package umc.catchy.domain.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.course.converter.CourseConverter;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.dto.response.CourseInfoResponse;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.converter.PlaceConverter;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final PlaceCourseRepository placeCourseRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final MemberRepository memberRepository;

    public Course getCourse(Long courseId){
        return courseRepository.findById(courseId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }

    //코스의 각 장소 별 간단한 정보 받아오기
    public List<CourseInfoResponse.getPlaceInfoOfCourseDTO> getPlaceListOfCourse(Course course, Member member){
        List<PlaceCourse> placeCourseList = placeCourseRepository.findAllByCourse(course);

        List<CourseInfoResponse.getPlaceInfoOfCourseDTO> DTOs = new ArrayList<>();
        for(PlaceCourse placeCourse : placeCourseList){
            //멤버의 장소 방문 여부 확인
            PlaceVisit placeVisit = placeVisitRepository.findByPlaceAndMember(placeCourse.getPlace(), member);  //TODO placeVisit Valid 체크 필요
            Boolean isVisited = placeVisit.isVisited();
            DTOs.add(PlaceConverter.toPlaceInfoOfCourseDTO(placeCourse.getPlace(), isVisited));
        }

        return DTOs;
    }

    //평점 계산 로직
    public Float calculateRatingOfCourse(Long courseId){
        Course course = getCourse(courseId);

        if(!course.isHasReview()){
            return 0.0F;
        }
        else{
            return null;
        }
    }

    //리뷰 개수 로직
    public Long calculateNumberOfReviews(Long courseId){
        return null;
    }

    //코스의 상세 정보 받아오기
    public CourseInfoResponse.getCourseInfoDTO getCourseDetails(Long courseId) {
        Course course = getCourse(courseId);
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<CourseInfoResponse.getPlaceInfoOfCourseDTO> placeListOfCourse = getPlaceListOfCourse(course, member);
        return CourseConverter.toCourseInfoDTO(course, placeListOfCourse);
    }
}
