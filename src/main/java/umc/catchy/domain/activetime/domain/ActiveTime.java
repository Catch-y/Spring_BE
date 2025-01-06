package umc.catchy.domain.activetime.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.common.BaseTimeEntity;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Entity
@Getter
public class ActiveTime extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activeTime_id")
    private Long id;

    private DayOfWeek dayOfWeek;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
