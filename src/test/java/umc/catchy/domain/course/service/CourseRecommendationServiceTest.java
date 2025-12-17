package umc.catchy.domain.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.response.CourseRecommendationResponse;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.course.dto.response.PopularCourseInfoResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRecommendationServiceTest {

    @InjectMocks
    private CourseRecommendationService recommendationService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private AICourseGenerationService aiCourseGenerationService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recommendationService, "CACHE_KEY", "test-key");
        ReflectionTestUtils.setField(recommendationService, "CACHE_TTL", 100L);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("홈 추천 코스 조회 - 캐시 적중 (DB 조회 생략)")
    void getHomeRecommendedCourses_cache_hit() throws JsonProcessingException {
        // given
        Long memberId = 1L;
        String expectedKey = "test-key:1";
        String cachedJson = "[{\"courseName\":\"캐시된 코스\"}]";

        CourseRecommendationResponse responseObj = new CourseRecommendationResponse(
                10L,
                "캐시된 코스",
                "설명",
                "image.jpg",
                CourseType.DIY
        );
        List<CourseRecommendationResponse> cachedResponse = List.of(responseObj);

        when(valueOperations.get(expectedKey)).thenReturn(cachedJson);
        when(objectMapper.readValue(eq(cachedJson), any(TypeReference.class)))
                .thenReturn(cachedResponse);

        // when
        List<CourseRecommendationResponse> result = recommendationService.getHomeRecommendedCourses(memberId);

        // then
        assertAll(
                () -> assertThat(result).hasSize(1),
                () -> assertThat(result.get(0).courseName()).isEqualTo("캐시된 코스"),
                () -> assertThat(result.get(0).courseType()).isEqualTo(CourseType.DIY)
        );

        verify(courseRepository, never()).findTopNByMemberIdAndCourseTypeOrderByCreatedDateDesc(anyLong(), any(), any());
        verify(aiCourseGenerationService, never()).generateMultipleAICourses(anyLong(), anyInt());
    }

    @Test
    @DisplayName("홈 추천 코스 조회 - 캐시 미스 (DIY 부족 -> AI 생성 -> 캐시 저장)")
    void getHomeRecommendedCourses_cache_miss_generate_ai() throws JsonProcessingException {
        // given
        Long memberId = 1L;
        String expectedKey = "test-key:1";

        // 1. Redis 캐시 미스 설정
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // 2. 사용자 DIY 코스 조회 (2개 있다고 가정 -> AI 3개 필요)
        Course diyCourse1 = mock(Course.class);
        Course diyCourse2 = mock(Course.class);
        when(diyCourse1.getCourseType()).thenReturn(CourseType.DIY);
        when(diyCourse2.getCourseType()).thenReturn(CourseType.DIY);

        when(courseRepository.findTopNByMemberIdAndCourseTypeOrderByCreatedDateDesc(
                eq(memberId), eq(CourseType.DIY), any()))
                .thenReturn(List.of(diyCourse1, diyCourse2));

        // 3. AI 코스 생성 로직 Mocking (3개 요청)
        GptCourseInfoResponse aiResponse = new GptCourseInfoResponse(
                "AI 코스", "설명", "09:00~18:00", List.of()
        );
        when(aiCourseGenerationService.generateMultipleAICourses(eq(memberId), eq(3)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(List.of(aiResponse, aiResponse, aiResponse)));

        // 4. 멤버 조회 및 저장 로직 무시
        Member mockMember = mock(Member.class);
        when(memberRepository.findById(memberId)).thenReturn(java.util.Optional.of(mockMember));

        // 5. 생성된 AI 코스 DB 조회 결과 (3개)
        Course aiCourse = mock(Course.class);
        when(aiCourse.getCourseType()).thenReturn(CourseType.AI);
        when(courseRepository.findTopNByMemberIdAndCourseTypeOrderByCreatedDateDesc(
                eq(memberId), eq(CourseType.AI), any()))
                .thenReturn(List.of(aiCourse, aiCourse, aiCourse));

        // 6. 직렬화 Mocking
        when(objectMapper.writeValueAsString(any())).thenReturn("serialized-json");

        // when
        List<CourseRecommendationResponse> result = recommendationService.getHomeRecommendedCourses(memberId);

        // then
        assertAll(
                () -> assertThat(result).hasSize(5),
                () -> assertThat(result.get(0).courseType()).isEqualTo(CourseType.DIY),
                () -> assertThat(result.get(4).courseType()).isEqualTo(CourseType.AI)
        );

        verify(aiCourseGenerationService).generateMultipleAICourses(eq(memberId), eq(3));
        verify(courseService, times(3)).saveCourseAndPlaces(any(), eq(mockMember));
        verify(valueOperations).set(eq(expectedKey), eq("serialized-json"), anyLong(), any());
    }

    @Test
    @DisplayName("인기 코스 조회 성공")
    void getPopularCourses_success() {
        // given
        Course popularCourse1 = mock(Course.class);
        Course popularCourse2 = mock(Course.class);

        when(popularCourse1.getId()).thenReturn(10L);
        when(popularCourse1.getCourseName()).thenReturn("인기 코스 1");

        when(popularCourse2.getId()).thenReturn(20L);
        when(popularCourse2.getCourseName()).thenReturn("인기 코스 2");

        when(courseRepository.findPopularCourses()).thenReturn(List.of(popularCourse1, popularCourse2));

        // when
        List<PopularCourseInfoResponse> result = recommendationService.getPopularCourses();

        // then
        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0).courseId()).isEqualTo(10L),
                () -> assertThat(result.get(0).courseName()).isEqualTo("인기 코스 1"),
                () -> assertThat(result.get(1).courseId()).isEqualTo(20L),
                () -> assertThat(result.get(1).courseName()).isEqualTo("인기 코스 2")
        );

        verify(courseRepository).findPopularCourses();
    }
}
