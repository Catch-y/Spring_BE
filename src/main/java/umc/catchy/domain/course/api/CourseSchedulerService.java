package umc.catchy.domain.course.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import umc.catchy.domain.course.service.CourseService;
import umc.catchy.global.error.exception.GeneralException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSchedulerService {
    @Value("${cache.recommended-courses.key}")
    private String CACHE_KEY;
    private final CourseService courseService;
    private final RedisTemplate<String, String> redisTemplate;

    // 매주 월요일 00시 00분 00초
    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    public void scheduledCourseGeneration() {
        List<Long> allMemberIds = courseService.getAllMemberIds();
        log.info("총 {}명의 회원에 대해 추천 코스를 생성합니다.", allMemberIds.size());

        for (Long memberId : allMemberIds) {
            try {
                // 스케줄러 실행 시 기존 Redis 캐시 삭제
                String userSpecificCacheKey = CACHE_KEY + ":" + memberId;
                redisTemplate.delete(userSpecificCacheKey);
                log.info("회원 ID {}의 기존 캐시를 삭제했습니다.", memberId);

                // 캐시 삭제 후 추천 코스 생성
                log.info("회원 ID {}의 추천 코스 생성을 시작합니다.", memberId);
                courseService.getHomeRecommendedCourses(memberId);
                log.info("회원 ID {}의 추천 코스 생성이 완료되었습니다.", memberId);
            } catch (GeneralException e) {
                log.error("회원 ID {}의 추천 코스 생성 중 오류 발생: {}", memberId, e.getMessage());
            } catch (Exception e) {
                log.error("회원 ID {} 처리 중 예상치 못한 오류 발생: {}", memberId, e.getMessage());
            }
        }
        log.info("모든 회원의 추천 코스 생성 작업이 완료되었습니다.");
    }
}