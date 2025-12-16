package umc.catchy.domain.course.service;

import java.time.LocalTime;
import java.util.UUID;
import java.util.stream.IntStream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.category.dao.CategoryRepository;
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
import umc.catchy.domain.mapping.memberActivetime.dao.MemberActiveTimeRepository;
import umc.catchy.domain.mapping.memberCategory.dao.MemberCategoryRepository;
import umc.catchy.domain.mapping.memberLocation.dao.MemberLocationRepository;
import umc.catchy.domain.mapping.memberStyle.dao.MemberStyleRepository;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;
import umc.catchy.infra.config.fcm.FCMService;

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
    private final MemberLocationRepository memberLocationRepository;
    private final MemberActiveTimeRepository memberActiveTimeRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberStyleRepository memberStyleRepository;
    private final CategoryRepository categoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final GPTCourseService gptCourseService;
    private final PlaceReviewRepository placeReviewRepository;
    private final FCMService fcmService;

    @PersistenceContext
    private EntityManager entityManager;

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }

    //코스의 각 장소 별 간단한 정보 받아오기
    private List<CourseDetailResponse.CoursePlaceInfo> getPlaceListOfCourse(Course course, Member member) {
        return placeCourseRepository.findAllByCourse(course).stream()
                .map(placeCourse -> {
                    // 멤버의 장소 방문 여부 확인
                    Boolean isVisited = placeVisitRepository.findByPlaceAndMember(placeCourse.getPlace(), member)
                            .map(PlaceVisit::isVisited)
                            .orElse(false);

                    return CourseDetailResponse.CoursePlaceInfo.of(placeCourse.getPlace(), isVisited);
                })
                .toList();
    }

    //Course : 리뷰 개수 로직
    private Integer calculateNumberOfReviews(Course course) {
        if (!course.isHasReview()) {
            return 0;
        } else {
            return courseReviewRepository.countAllByCourse(course);
        }
    }

    //Course : 추천 시간대 String 변환
    private String getRecommendTimeToString(Course course) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        LocalTime startTime = course.getRecommendTimeStart() != null ? course.getRecommendTimeStart() : LocalTime.of(9, 0);
        LocalTime endTime = course.getRecommendTimeEnd() != null ? course.getRecommendTimeEnd() : LocalTime.of(21, 0);

        return startTime.format(formatter) + " ~ " + endTime.format(formatter);
    }

    //북마크 여부 가져오기
    private Boolean getBookmarks(Course course, Member member) {
        return memberCourseRepository.findByCourseAndMember(course, member)
                .map(MemberCourse::isBookmark)
                .orElse(false);
    }

    //코스의 상세 정보 받아오기
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

    public CourseDetailResponse createCourse(CourseCreateRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 이미지 업로드
        String courseImageUrl = null;
        if (request.courseImage() != null) {
            String keyName = "course-images/" + UUID.randomUUID();
            courseImageUrl = amazonS3Manager.uploadFile(keyName, request.courseImage());
        }

        Course course = request.toEntity(member, courseImageUrl);

        // 평점 계산 (초기 생성 시)
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

        // 장소 매핑 저장
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

        // MemberCourse 매핑
        MemberCourse memberCourse = MemberCourse.builder()
                .course(course)
                .member(member)
                .build();
        memberCourseRepository.save(memberCourse);

        List<CourseDetailResponse.CoursePlaceInfo> placeListOfCourse = getPlaceListOfCourse(course, member);
        return CourseDetailResponse.from(course, calculateNumberOfReviews(course), false, placeListOfCourse);
    }

    public List<Long> getAllMemberIds() {
        return memberRepository.findAll().stream()
                .map(Member::getId)
                .collect(Collectors.toList());
    }
}
