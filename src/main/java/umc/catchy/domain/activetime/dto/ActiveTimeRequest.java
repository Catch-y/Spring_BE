package umc.catchy.domain.activetime.dto;

import java.time.DayOfWeek;

public record ActiveTimeRequest(
        DayOfWeek dayOfWeek,
        String startTime,
        String endTime
) {
}