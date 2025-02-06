package umc.catchy.domain.activetime.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.catchy.domain.common.BaseTimeEntity;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ActiveTime extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activeTime_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    public static ActiveTime createActiveTime(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        return ActiveTime.builder().dayOfWeek(dayOfWeek).startTime(startTime).endTime(endTime).build();
    }
}
