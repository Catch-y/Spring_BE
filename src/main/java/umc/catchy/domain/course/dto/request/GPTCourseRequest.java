package umc.catchy.domain.course.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class GPTCourseRequest {
    private String courseName;        // 코스 이름
    private String description;       // 코스 설명
    private String region;            // 선호 지역
    private String startTime;         // 활동 시작 시간
    private String endTime;           // 활동 종료 시간
    private String preferences;       // 사용자 선호도
    private MultipartFile courseImage; // 코스 이미지
}