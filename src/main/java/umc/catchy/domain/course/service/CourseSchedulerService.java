package umc.catchy.domain.course.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import umc.catchy.domain.course.service.CourseService;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.constants.FcmConstants;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.infra.config.fcm.FCMService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static umc.catchy.global.common.constants.FcmConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSchedulerService {
    @Value("${cache.recommended-courses.key}")
    private String CACHE_KEY;
    private final CourseService courseService;
    private final RedisTemplate<String, String> redisTemplate;
    private final MemberRepository memberRepository;
    private final FCMService fcmService;

    // 매주 월요일 00시 00분 00초
    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    public void scheduledCourseGeneration() {
        List<Long> allMemberIds = courseService.getAllMemberIds();

        List<CompletableFuture<Void>> futures = allMemberIds.stream()
                .map(memberId -> CompletableFuture.runAsync(() -> processMemberRecommendation(memberId)))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void processMemberRecommendation(Long memberId) {
        try {
            String userSpecificCacheKey = CACHE_KEY + ":" + memberId;
            redisTemplate.delete(userSpecificCacheKey);
            courseService.getHomeRecommendedCourses(memberId);
            Member member = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
            if (member.getFcmInfo().getAppAlarm()) {
                fcmService.sendMessageSync(member.getFcmInfo().getFcmToken(), COURSE_UPDATED_MESSAGE_TITLE, COURSE_UPDATED_MESSAGE_CONTENT);
            }
        } catch (GeneralException e) {
            log.error("회원 ID {}의 추천 코스 생성 중 오류 발생: {}", memberId, e.getMessage());
        } catch (Exception e) {
            log.error("회원 ID {} 처리 중 예상치 못한 오류 발생: {}", memberId, e.getMessage());
        }
    }
}