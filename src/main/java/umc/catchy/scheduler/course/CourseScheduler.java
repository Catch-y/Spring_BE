package umc.catchy.scheduler.course;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import umc.catchy.domain.course.service.CourseService;

@Slf4j
@RequiredArgsConstructor
@Component
public class CourseScheduler {
    private final CourseService courseService;

    @Scheduled(cron = "0 0 0 ? * MON " , zone = "Asia/Seoul")
    public void pushNotificationByCourseRecommend() {
        log.info("Pushing notifications by recommend");
        courseService.pushNotificationCourseRecommend();
    }
}
