package umc.catchy.domain.course.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.FcmInfo;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.infra.config.fcm.FCMService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static umc.catchy.global.common.constants.FcmConstants.COURSE_UPDATED_MESSAGE_CONTENT;
import static umc.catchy.global.common.constants.FcmConstants.COURSE_UPDATED_MESSAGE_TITLE;

@ExtendWith(MockitoExtension.class)
class CourseSchedulerServiceTest {

    @InjectMocks
    private CourseSchedulerService schedulerService;

    @Mock
    private CourseService courseService;

    @Mock
    private CourseRecommendationService courseRecommendationService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FCMService fcmService;

    @Mock
    private Executor schedulerExecutor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(schedulerService, "CACHE_KEY", "test-key");

        // 스레드 풀
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(schedulerExecutor).execute(any());
    }

    @Test
    @DisplayName("주간 코스 추천 스케줄러 실행 성공 - 캐시 갱신 및 알림 전송")
    void scheduledCourseGeneration_success() {
        // given
        Long memberId = 1L;
        String expectedRedisKey = "test-key:1";

        when(courseService.getAllMemberIds()).thenReturn(List.of(memberId));

        Member mockMember = mock(Member.class);
        FcmInfo mockFcmInfo = mock(FcmInfo.class);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));
        when(mockMember.getFcmInfo()).thenReturn(mockFcmInfo);
        when(mockFcmInfo.getAppAlarm()).thenReturn(true);
        when(mockFcmInfo.getFcmToken()).thenReturn("test-token");

        // when
        schedulerService.scheduledCourseGeneration();

        // then
        verify(redisTemplate).delete(expectedRedisKey);
        verify(courseRecommendationService).getHomeRecommendedCourses(memberId);
        verify(fcmService).sendMessageSync(
                eq("test-token"),
                eq(COURSE_UPDATED_MESSAGE_TITLE),
                eq(COURSE_UPDATED_MESSAGE_CONTENT)
        );
    }

    @Test
    @DisplayName("스케줄러 실행 성공 - 알림 미수신 설정 시 캐시만 갱신")
    void scheduledCourseGeneration_success_no_alarm() {
        // given
        Long memberId = 1L;
        String expectedRedisKey = "test-key:1";

        when(courseService.getAllMemberIds()).thenReturn(List.of(memberId));

        Member mockMember = mock(Member.class);
        FcmInfo mockFcmInfo = mock(FcmInfo.class);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));
        when(mockMember.getFcmInfo()).thenReturn(mockFcmInfo);
        when(mockFcmInfo.getAppAlarm()).thenReturn(false);

        // when
        schedulerService.scheduledCourseGeneration();

        // then
        verify(redisTemplate).delete(expectedRedisKey);
        verify(courseRecommendationService).getHomeRecommendedCourses(memberId);
        verify(fcmService, never()).sendMessageSync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("스케줄러 실행 중 예외 발생 - 특정 회원 실패 시에도 전체 프로세스는 중단되지 않음")
    void scheduledCourseGeneration_partial_failure() {
        // given
        Long failMemberId = 1L;
        Long successMemberId = 2L;

        when(courseService.getAllMemberIds()).thenReturn(List.of(failMemberId, successMemberId));

        // 1. 실패하는 회원 (Member 1): Redis 삭제 시 예외 발생한다고 가정
        String failKey = "test-key:" + failMemberId;
        doThrow(new RuntimeException("Redis Error")).when(redisTemplate).delete(failKey);

        // 2. 성공하는 회원 (Member 2): 정상 설정
        Member mockMember = mock(Member.class);
        FcmInfo mockFcmInfo = mock(FcmInfo.class);
        when(memberRepository.findById(successMemberId)).thenReturn(Optional.of(mockMember));
        when(mockMember.getFcmInfo()).thenReturn(mockFcmInfo);
        when(mockFcmInfo.getAppAlarm()).thenReturn(true);
        when(mockFcmInfo.getFcmToken()).thenReturn("token-2");

        // when
        // 내부 try-catch로 인해 메서드 밖으로 예외가 던져지지 않아야 함
        schedulerService.scheduledCourseGeneration();

        // then
        // 1. 실패한 멤버: 예외 발생 지점까지만 호출되고, 이후 로직(코스 생성)은 수행되지 않음
        verify(redisTemplate).delete(failKey);
        verify(courseRecommendationService, never()).getHomeRecommendedCourses(failMemberId);

        // 2. 성공한 멤버: 에러와 상관없이 정상적으로 끝까지 수행됨
        verify(redisTemplate).delete("test-key:" + successMemberId);
        verify(courseRecommendationService).getHomeRecommendedCourses(successMemberId);
        verify(fcmService).sendMessageSync(eq("token-2"), anyString(), anyString());
    }
}
