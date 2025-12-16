package umc.catchy.domain.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.response.CourseRecommendationResponse;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.course.dto.response.PopularCourseInfoResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseRecommendationService {

    private static final int TOTAL_RECOMMENDATION_COUNT = 5;
    private static final int MAX_DIY_COURSE_COUNT = 2;
    private static final String KEY_DELIMITER = ":";

    @Value("${cache.recommended-courses.key}")
    private String CACHE_KEY;

    @Value("${cache.recommended-courses.ttl}")
    private long CACHE_TTL;

    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;
    private final CourseService courseService;
    private final AICourseGenerationService aiCourseGenerationService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<CourseRecommendationResponse> getHomeRecommendedCourses(Long memberId) {
        String userSpecificCacheKey = CACHE_KEY + KEY_DELIMITER + memberId;

        String cachedData = redisTemplate.opsForValue().get(userSpecificCacheKey);
        if (cachedData != null) {
            List<CourseRecommendationResponse> cachedResponse = deserializeCourseRecommendations(cachedData);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        List<CourseRecommendationResponse> recommendedCourses = generateRecommendedCourses(memberId);

        String serializedData = serializeCourseRecommendations(recommendedCourses);
        if (serializedData != null) {
            redisTemplate.opsForValue().set(userSpecificCacheKey, serializedData, CACHE_TTL, TimeUnit.SECONDS);
        }

        return recommendedCourses;
    }

    @Transactional
    public List<CourseRecommendationResponse> generateRecommendedCourses(Long memberId) {
        List<Course> userCourses = courseRepository.findTopNByMemberIdAndCourseTypeOrderByCreatedDateDesc(
                memberId,
                CourseType.DIY,
                PageRequest.of(0, MAX_DIY_COURSE_COUNT)
        );

        int neededAiCount = TOTAL_RECOMMENDATION_COUNT - userCourses.size();
        List<Course> aiCourses = List.of();

        if (neededAiCount > 0) {
            aiCourses = createAndFetchAiCourses(memberId, neededAiCount);
        }

        List<CourseRecommendationResponse> result = new ArrayList<>();
        result.addAll(toResponseList(userCourses));
        result.addAll(toResponseList(aiCourses));

        return result;
    }

    public List<PopularCourseInfoResponse> getPopularCourses() {
        return courseRepository.findPopularCourses().stream()
                .map(PopularCourseInfoResponse::from)
                .toList();
    }

    private List<Course> createAndFetchAiCourses(Long memberId, int count) {
        List<GptCourseInfoResponse> gptResponses = aiCourseGenerationService
                .generateMultipleAICourses(memberId, count).join();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        for (GptCourseInfoResponse gptResponse : gptResponses) {
            courseService.saveCourseAndPlaces(gptResponse, member);
        }

        return courseRepository.findTopNByMemberIdAndCourseTypeOrderByCreatedDateDesc(
                memberId,
                CourseType.AI,
                PageRequest.of(0, count)
        );
    }

    private List<CourseRecommendationResponse> toResponseList(List<Course> courses) {
        return courses.stream()
                .map(CourseRecommendationResponse::from)
                .toList();
    }

    private String serializeCourseRecommendations(List<CourseRecommendationResponse> courses) {
        try {
            return objectMapper.writeValueAsString(courses);
        } catch (JsonProcessingException e) {
            log.error("[Cache Error] 추천 코스 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    private List<CourseRecommendationResponse> deserializeCourseRecommendations(String cachedData) {
        try {
            return objectMapper.readValue(cachedData, new TypeReference<List<CourseRecommendationResponse>>() {});
        } catch (JsonProcessingException e) {
            log.error("[Cache Error] 추천 코스 역직렬화 실패 (Data: {}): {}", cachedData, e.getMessage());
            return null;
        }
    }
}