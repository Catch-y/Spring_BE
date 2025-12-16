package umc.catchy.domain.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseRecommendationService {

    private static final int TOTAL_RECOMMENDATION_COUNT = 5;

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
        String userSpecificCacheKey = CACHE_KEY + ":" + memberId;
        String cachedData = redisTemplate.opsForValue().get(userSpecificCacheKey);

        if (cachedData != null) {
            return deserializeCourseRecommendations(cachedData);
        }

        List<CourseRecommendationResponse> recommendedCourses = generateRecommendedCourses(memberId);
        String serializedData = serializeCourseRecommendations(recommendedCourses);
        redisTemplate.opsForValue().set(userSpecificCacheKey, serializedData, CACHE_TTL, TimeUnit.SECONDS);

        return recommendedCourses;
    }

    @Transactional
    public List<CourseRecommendationResponse> generateRecommendedCourses(Long memberId) {
        List<Course> userCourses = courseRepository.findTop2ByMemberIdAndCourseTypeOrderByCreatedDateDesc(
                memberId, CourseType.DIY
        );

        int userCourseCount = userCourses.size();
        int aiCourseCount = TOTAL_RECOMMENDATION_COUNT - userCourseCount;

        List<CourseRecommendationResponse> recommendedCourses = new ArrayList<>();
        recommendedCourses.addAll(userCourses.stream()
                .map(CourseRecommendationResponse::from)
                .toList());

        if (aiCourseCount > 0) {
            List<GptCourseInfoResponse> gptResponses = aiCourseGenerationService
                    .generateMultipleAICourses(memberId, aiCourseCount).join();

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

            for (GptCourseInfoResponse gptResponse : gptResponses) {
                courseService.saveCourseAndPlaces(gptResponse, member);
            }

            List<Course> aiCourses = courseRepository.findTopNByMemberIdAndCourseTypeOrderByCreatedDateDesc(
                    memberId,
                    CourseType.AI,
                    PageRequest.of(0, aiCourseCount)
            );

            recommendedCourses.addAll(aiCourses.stream()
                    .map(CourseRecommendationResponse::from)
                    .toList());
        }

        return recommendedCourses;
    }

    public List<PopularCourseInfoResponse> getPopularCourses() {
        return courseRepository.findPopularCourses().stream()
                .map(PopularCourseInfoResponse::from)
                .toList();
    }

    private String serializeCourseRecommendations(List<CourseRecommendationResponse> courses) {
        try {
            return objectMapper.writeValueAsString(courses);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize course recommendations", e);
        }
    }

    private List<CourseRecommendationResponse> deserializeCourseRecommendations(String cachedData) {
        try {
            return objectMapper.readValue(cachedData, new TypeReference<List<CourseRecommendationResponse>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize course recommendations", e);
        }
    }
}
