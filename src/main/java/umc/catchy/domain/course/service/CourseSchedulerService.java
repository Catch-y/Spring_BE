package umc.catchy.domain.course.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.infra.config.fcm.FCMService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static umc.catchy.global.common.constants.FcmConstants.COURSE_UPDATED_MESSAGE_CONTENT;
import static umc.catchy.global.common.constants.FcmConstants.COURSE_UPDATED_MESSAGE_TITLE;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSchedulerService {

    private static final String KEY_DELIMITER = ":";

    @Value("${cache.recommended-courses.key}")
    private String CACHE_KEY;

    private final CourseService courseService;
    private final CourseRecommendationService courseRecommendationService;
    private final StringRedisTemplate redisTemplate;
    private final MemberRepository memberRepository;
    private final FCMService fcmService;

    @Qualifier("schedulerExecutor")
    private final Executor schedulerExecutor;

    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    public void scheduledCourseGeneration() {
        log.info("[Scheduler] 주간 코스 추천 캐시 갱신 시작");

        // TODO: 회원 1만명 이상 시 배치 처리로 전환 필요
        List<Long> allMemberIds = courseService.getAllMemberIds();

        List<CompletableFuture<Void>> futures = allMemberIds.stream()
                .map(memberId -> CompletableFuture.runAsync(
                        () -> processMemberRecommendation(memberId),
                        schedulerExecutor
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("[Scheduler] 주간 코스 추천 캐시 갱신 완료. 총 {}명 처리", allMemberIds.size());
    }

    private void processMemberRecommendation(Long memberId) {
        try {
            refreshCourseCache(memberId);
            sendCourseUpdateNotification(memberId);
        } catch (GeneralException e) {
            log.error("회원 ID {}의 추천 코스 생성 중 오류 발생: {}", memberId, e.getMessage());
        } catch (Exception e) {
            log.error("회원 ID {} 처리 중 예상치 못한 오류 발생: {}", memberId, e.getMessage());
        }
    }

    private void refreshCourseCache(Long memberId) {
        String userSpecificCacheKey = CACHE_KEY + KEY_DELIMITER + memberId;
        redisTemplate.delete(userSpecificCacheKey);
        courseRecommendationService.getHomeRecommendedCourses(memberId);
    }

    private void sendCourseUpdateNotification(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (Boolean.TRUE.equals(member.getFcmInfo().getAppAlarm())) {
            fcmService.sendMessageSync(
                    member.getFcmInfo().getFcmToken(),
                    COURSE_UPDATED_MESSAGE_TITLE,
                    COURSE_UPDATED_MESSAGE_CONTENT
            );
        }
    }
}
