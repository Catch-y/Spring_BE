package umc.catchy.domain.course.service;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.course.converter.CourseConverter;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.response.CourseInfoResponse;
import umc.catchy.domain.courseReview.dao.CourseReviewRepository;
import umc.catchy.domain.mapping.memberCourse.converter.MemberCourseConverter;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.converter.PlaceConverter;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final PlaceCourseRepository placeCourseRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final MemberRepository memberRepository;
    private final MemberCourseRepository memberCourseRepository;

    private Course getCourse(Long courseId){
        return courseRepository.findById(courseId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }

    //코스의 각 장소 별 간단한 정보 받아오기
    private List<CourseInfoResponse.getPlaceInfoOfCourseDTO> getPlaceListOfCourse(Course course, Member member){
        return placeCourseRepository.findAllByCourse(course).stream()
                .map(placeCourse -> {
                    // 멤버의 장소 방문 여부 확인
                    Boolean isVisited = placeVisitRepository.findByPlaceAndMember(placeCourse.getPlace(), member)
                            .map(PlaceVisit::isVisited)
                            .orElse(false); // null -> 기본값 false
                    return PlaceConverter.toPlaceInfoOfCourseDTO(placeCourse.getPlace(), isVisited);
                })
                .collect(Collectors.toList());
    }

    //Course : 평점 계산 로직
    private Float calculateRatingOfCourse(Course course){
        //if(!course.isHasReview()){ return 0.0F; }
        return 0.0F; //TODO placeReview 관련 로직 작성하면서 함께 구현하기
    }

    //Course : 리뷰 개수 로직
    private Integer calculateNumberOfReviews(Course course){
        if(!course.isHasReview()){
            return 0;
        }
        else{
            return courseReviewRepository.countAllByCourse(course);
        }
    }

    //Course : 추천 시간대 String 변환
    private String getRecommendTimeToString(Course course){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return course.getRecommendTimeStart().format(formatter)
                +" ~ "
                +course.getRecommendTimeEnd().format(formatter);
    }

    //코스의 상세 정보 받아오기
    public CourseInfoResponse.getCourseInfoDTO getCourseDetails(Long courseId) {
        Course course = getCourse(courseId);
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<CourseInfoResponse.getPlaceInfoOfCourseDTO> placeListOfCourse = getPlaceListOfCourse(course, member);
        return CourseConverter.toCourseInfoDTO(course, calculateRatingOfCourse(course), calculateNumberOfReviews(course), getRecommendTimeToString(course), placeListOfCourse);
    }

    // 현재 사용자의 코스를 불러옴
    public List<MemberCourseResponse> getMemberCourses(String type, String upperLocation, String lowerLocation) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<MemberCourse> memberCourses = memberCourseRepository.findAllByMember(member);

        CourseType courseType;

        if (type.equals("AI")) courseType = CourseType.AI_GENERATED;
        else if (type.equals("DIY")) courseType = CourseType.USER_CREATED;
        else throw new GeneralException(ErrorStatus.INVALID_COURSE_TYPE);

        List<Course> courses = filterByConditions(memberCourses, courseType, upperLocation, lowerLocation);

        return courses.stream()
                .map(course -> {
                    List<BigCategory> categories = getCategories(course);
                    return MemberCourseConverter.toMemberCourseResponse(course, categories);
                }).toList();
    }

    private List<BigCategory> getCategories(Course course) {
        List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourse(course);

        return placeCourses.stream()
                .map(PlaceCourse::getPlace)
                .map(Place::getCategory)
                .map(Category::getBigCategory)
                .distinct()
                .toList();
    }

    // 통합 필터
    private List<Course> filterByConditions(List<MemberCourse> memberCourses, CourseType courseType, String upperLocation, String lowerLocation) {
        List<Course> filteredByCourseType = filterByCourseType(memberCourses, courseType);
        List<Course> filteredByUpperLocation = filterByUpperLocation(filteredByCourseType, upperLocation);

        return filterByLowerLocation(filteredByUpperLocation, lowerLocation);
    }

    // courseType에 따라 필터링
    private List<Course> filterByCourseType(List<MemberCourse> memberCourses, CourseType courseType) {
        return memberCourses.stream()
                .map(MemberCourse::getCourse)
                .filter(course -> course.getCourseType().equals(courseType))
                .toList();
    }

    // 상위 지역에 따라 필터링
    private List<Course> filterByUpperLocation(List<Course> courses, String upperLocation) {
        if (upperLocation.equals("all")) return courses;

        // hasUpperLocation이 true인 course만 반환
        return courses.stream()
                .filter(course -> {
                    List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourse(course);
                    return hasUpperLocation(placeCourses, upperLocation);
                }).distinct().toList();
    }

    // 하위 지역에 따라 필터링
    private List<Course> filterByLowerLocation(List<Course> courses, String lowerLocation) {
        if (lowerLocation.equals("all")) return courses;

        // hasLowerLocation이 true인 course만 반환
        return courses.stream()
                .filter(course -> {
                    List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourse(course);
                    return hasLowerLocation(placeCourses, lowerLocation);
                }).distinct().toList();
    }

    private boolean hasUpperLocation(List<PlaceCourse> placeCourses, String upperLocation) {
        // 코스에 요청받은 상위 지역에 해당하는 장소가 있으면 true 반환
        return placeCourses.stream()
                .map(PlaceCourse::getPlace)
                .anyMatch(place -> {
                    List<String> roadAddress = Arrays.stream(place.getRoadAddress().split(" ")).toList();

                    // 상위 지역(도) 확인
                    return roadAddress.get(0).equals(upperLocation);
                });
    }

    private boolean hasLowerLocation(List<PlaceCourse> placeCourses, String lowerLocation) {
        // 코스에 요청받은 하위 지역에 해당하는 장소가 있으면 true 반환
        return placeCourses.stream()
                .map(PlaceCourse::getPlace)
                .anyMatch(place -> {
                    List<String> roadAddress = Arrays.stream(place.getRoadAddress().split(" ")).toList();

                    // 하위 지역(시,군,구) 확인
                    return roadAddress.get(1).equals(lowerLocation);
                });
    }
}
