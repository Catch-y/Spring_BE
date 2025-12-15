package umc.catchy.domain.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record CourseCreateRequest(
        @NotBlank(message = "코스 이름은 필수 입력 항목입니다.")
        String courseName,

        @NotBlank(message = "코스 설명은 필수 입력 항목입니다.")
        String courseDescription,

        @NotNull(message = "코스 타입은 필수입니다.")
        CourseType courseType,

        @Size(min = 2, max = 5, message = "장소는 2개 이상 5개 이하로 선택되어야 합니다.")
        List<Long> placeIds,

        MultipartFile courseImage,

        @NotNull(message = "시작 시간대는 필수입니다.")
        String recommendTimeStart, // "HH:mm"

        @NotNull(message = "종료 시간대는 필수입니다.")
        String recommendTimeEnd    // "HH:mm"
) {
    public Course toEntity(Member member, String imageUrl) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        return Course.builder()
                .courseName(courseName)
                .courseDescription(courseDescription)
                .courseImage(imageUrl)
                .courseType(courseType)
                .recommendTimeStart(LocalTime.parse(recommendTimeStart, formatter))
                .recommendTimeEnd(LocalTime.parse(recommendTimeEnd, formatter))
                .member(member)
                .participantsNumber(0L)
                .hasReview(false)
                .rating(0.0)
                .build();
    }
}
