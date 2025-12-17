package umc.catchy.domain.course.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.course.ai.GptPromptBuilder;
import umc.catchy.domain.course.ai.GptResponseParser;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.mapping.memberActivetime.dao.MemberActiveTimeRepository;
import umc.catchy.domain.mapping.memberCategory.dao.MemberCategoryRepository;
import umc.catchy.domain.mapping.memberLocation.dao.MemberLocationRepository;
import umc.catchy.domain.mapping.memberLocation.domain.MemberLocation;
import umc.catchy.domain.mapping.memberStyle.dao.MemberStyleRepository;
import umc.catchy.domain.location.domain.Location;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.infra.openai.OpenAiClient;
import umc.catchy.support.fixture.MemberFixture;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static umc.catchy.support.fixture.MemberFixture.createTestMember;

@ExtendWith(MockitoExtension.class)
class AICourseGenerationServiceTest {

    @InjectMocks
    private AICourseGenerationService aiCourseService;

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private GptPromptBuilder gptPromptBuilder;

    @Mock
    private GptResponseParser gptResponseParser;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberLocationRepository memberLocationRepository;

    @Mock
    private MemberCategoryRepository memberCategoryRepository;

    @Mock
    private MemberStyleRepository memberStyleRepository;

    @Mock
    private MemberActiveTimeRepository memberActiveTimeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private Executor gptExecutor;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = MemberFixture.createTestMember();

        // 스레드 풀
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(gptExecutor).execute(any());
    }

    @Test
    @DisplayName("AI 코스 생성 성공")
    void generateCourseAutomatically_success() {
        // given
        Long memberId = 1L;
        String mockGptJson = "{\"result\": \"success\"}";

        GptCourseInfoResponse mockParsedResponse = new GptCourseInfoResponse(
                "AI 추천 코스",
                "설명입니다",
                "09:00~18:00",
                List.of()
        );

        // 1. 회원 및 위치 정보 Mocking
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));

        MemberLocation mockLocation = mock(MemberLocation.class);
        Location location = Location.builder().upperLocation("서울").lowerLocation("강남구").build();
        when(mockLocation.getLocation()).thenReturn(location);
        when(memberLocationRepository.findAllByMemberId(memberId)).thenReturn(List.of(mockLocation));

        // 2. 기타 정보 Mocking
        when(memberCategoryRepository.findByMemberId(memberId)).thenReturn(List.of());
        when(categoryRepository.findIdsByNames(anyList())).thenReturn(List.of());
        when(placeRepository.findRecommendedPlaces(any(), any(), any(), any(), anyInt())).thenReturn(List.of());
        when(memberStyleRepository.findByMemberId(memberId)).thenReturn(List.of());
        when(memberActiveTimeRepository.findByMemberId(memberId)).thenReturn(List.of());

        // 3. GPT 호출 및 파싱 Mocking
        when(gptPromptBuilder.buildCourseRecommendationPrompt(any(), any(), any(), any(), any()))
                .thenReturn("generated-prompt");

        when(openAiClient.chat(anyString(), anyInt(), anyDouble()))
                .thenReturn(CompletableFuture.completedFuture(mockGptJson));

        when(gptResponseParser.parse(mockGptJson)).thenReturn(mockParsedResponse);

        // when
        CompletableFuture<GptCourseInfoResponse> future = aiCourseService.generateCourseAutomatically(memberId);
        GptCourseInfoResponse response = future.join();

        // then
        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> assertThat(response.courseName()).isEqualTo("AI 추천 코스"),
                () -> assertThat(response.recommendTime()).isEqualTo("09:00~18:00")
        );

        verify(openAiClient).chat(anyString(), anyInt(), anyDouble());
        verify(gptResponseParser).parse(mockGptJson);
    }

    @Test
    @DisplayName("AI 코스 생성 실패 - 존재하지 않는 회원")
    void generateCourse_fail_member_not_found() {
        // given
        Long memberId = 999L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> aiCourseService.generateCourseAutomatically(memberId))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.MEMBER_NOT_FOUND.getMessage());

        verify(openAiClient, never()).chat(anyString(), anyInt(), anyDouble());
    }

    @Test
    @DisplayName("AI 코스 생성 실패 - 위치 정보 없음")
    void generateCourse_fail_no_location() {
        // given
        Long memberId = 1L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
        when(memberLocationRepository.findAllByMemberId(memberId)).thenReturn(List.of());

        // when & then
        assertThatThrownBy(() -> aiCourseService.generateCourseAutomatically(memberId))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.INVALID_REQUEST_INFO.getMessage());

        verify(openAiClient, never()).chat(anyString(), anyInt(), anyDouble());
    }

    @Test
    @DisplayName("AI 코스 생성 실패 - GPT API 호출 실패")
    void generateCourse_fail_gpt_api_error() {
        // given
        Long memberId = 1L;

        // 1. 기본 정보 Mocking
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));

        MemberLocation mockLocation = mock(MemberLocation.class);
        Location location = Location.builder().upperLocation("서울").lowerLocation("강남구").build();
        when(mockLocation.getLocation()).thenReturn(location);
        when(memberLocationRepository.findAllByMemberId(memberId)).thenReturn(List.of(mockLocation));

        // 2. 중간 데이터 수집 Mocking
        when(memberCategoryRepository.findByMemberId(memberId)).thenReturn(List.of());
        when(categoryRepository.findIdsByNames(anyList())).thenReturn(List.of());
        when(placeRepository.findRecommendedPlaces(any(), any(), any(), any(), anyInt())).thenReturn(List.of());
        when(memberStyleRepository.findByMemberId(memberId)).thenReturn(List.of());
        when(memberActiveTimeRepository.findByMemberId(memberId)).thenReturn(List.of());

        // 3. 프롬프트 생성 Mocking
        when(gptPromptBuilder.buildCourseRecommendationPrompt(any(), any(), any(), any(), any()))
                .thenReturn("prompt");

        // 4. GPT 호출 시 예외 발생 (실패한 Future 반환)
        CompletableFuture<String> failedFuture = CompletableFuture.failedFuture(new RuntimeException("API Error"));
        when(openAiClient.chat(anyString(), anyInt(), anyDouble())).thenReturn(failedFuture);

        // when & then
        // 비동기 파이프라인(.exceptionally)을 타서 예외가 변환되었는지 확인
        assertThatThrownBy(() -> aiCourseService.generateCourseAutomatically(memberId).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("GPT 사용 횟수 증가 성공")
    void increaseGptCount_success() {
        // given
        Long memberId = 1L;

        Member mockMember = mock(Member.class);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));

        // when
        aiCourseService.increaseGptCount(memberId);

        // then
        verify(mockMember).increaseGptCount();
    }
}
