package umc.catchy.domain.course.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.course.converter.CourseConverter;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.request.CourseUpdateRequest;
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
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

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
    private final AmazonS3Manager amazonS3Manager;
    private final PlaceRepository placeRepository;

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
        return CourseConverter.toCourseInfoDTO(course, calculateNumberOfReviews(course), getRecommendTimeToString(course), placeListOfCourse);
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

        // 최신순으로 정렬
        return courses.stream()
                .sorted(Comparator.comparing(Course::getCreatedDate).reversed())
                .map(course -> {
                    System.out.println(course.getCreatedDate());
                    List<BigCategory> categories = getCategories(course);
                    return MemberCourseConverter.toMemberCourseResponse(course, categories);
                }).toList();
    }

    // 코스 수정
    public CourseInfoResponse.getCourseInfoDTO updateCourse(Long courseId, CourseUpdateRequest request) {
        Course course = getCourse(courseId);

        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 사용자가 가지고 있는 코스인지 검증
        memberCourseRepository.findByCourseAndMember(course, member)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_INVALID_MEMBER));

        // 코스 이름 수정
        if (!request.getCourseName().isEmpty()) {
            course.setCourseName(request.getCourseName());
        }

        // 코스 설명 수정
        if (!request.getCourseDescription().isEmpty()) {
            course.setCourseDescription(request.getCourseDescription());
        }

        // 코스 이미지 수정
        if (request.getCourseImage() != null) {
            String originCourseImageUrl = course.getCourseImage();

            if (!originCourseImageUrl.isEmpty())
                amazonS3Manager.deleteImage(originCourseImageUrl);

            MultipartFile newProfileImage = request.getCourseImage();

            String keyName = "course-images/" + newProfileImage.getOriginalFilename();
            String newProfileImageUrl = amazonS3Manager.uploadFile(keyName, newProfileImage);

            course.setCourseImage(newProfileImageUrl);
        }

        // 코스 장소 수정
        if (!request.getPlaceIds().isEmpty()) {
            List<Long> placeIds = request.getPlaceIds();

            // 기존의 장소는 제거
            List<PlaceCourse> originPlaces = placeCourseRepository.findAllByCourse(course);
            placeCourseRepository.deleteAll(originPlaces);

            // 코스에 추가
            IntStream.range(0, placeIds.size()).forEach(index -> {
                Long placeId = placeIds.get(index);
                Place place = placeRepository.findById(placeId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

                // List의 Index를 기반으로 코스 순서 결정
                PlaceCourse newPlaceCourse = PlaceCourse.builder()
                        .course(course)
                        .place(place)
                        .placeOrder(index + 1)
                        .build();

                placeCourseRepository.save(newPlaceCourse);
            });
        }

        // 추천 시간대 수정
        if (request.getRecommendTimeStart() != null) {
            course.setRecommendTimeStart(request.getRecommendTimeStart());
        }

        if (request.getRecommendTimeEnd() != null) {
            course.setRecommendTimeEnd(request.getRecommendTimeEnd());
        }

        List<CourseInfoResponse.getPlaceInfoOfCourseDTO> placeListOfCourse = getPlaceListOfCourse(course, member);
        return CourseConverter.toCourseInfoDTO(course, calculateNumberOfReviews(course), getRecommendTimeToString(course), placeListOfCourse);
    }

    public void deleteCourse(Long courseId) {
        Course course = getCourse(courseId);

        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 사용자가 가지고 있는 코스인지 검증
        MemberCourse memberCourse = memberCourseRepository.findByCourseAndMember(course, member)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_INVALID_MEMBER));

        memberCourseRepository.delete(memberCourse);

        // 코스의 장소들 삭제
        List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourse(course);
        placeCourseRepository.deleteAll(placeCourses);

        // 코스 삭제
        courseRepository.delete(course);
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
                }).distinct()
                .toList();
    }

    // 하위 지역에 따라 필터링
    private List<Course> filterByLowerLocation(List<Course> courses, String lowerLocation) {
        if (lowerLocation.equals("all")) return courses;

        // hasLowerLocation이 true인 course만 반환
        return courses.stream()
                .filter(course -> {
                    List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourse(course);
                    return hasLowerLocation(placeCourses, lowerLocation);
                }).distinct()
                .toList();
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
