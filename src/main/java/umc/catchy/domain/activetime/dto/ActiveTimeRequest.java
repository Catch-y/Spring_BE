package umc.catchy.domain.activetime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ActiveTimeRequest {
    private DayOfWeek dayOfWeek;
    private String startTime;
    private String endTime;
}
