package umc.catchy.domain.group.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.common.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Getter
public class Groups extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(length = 20)
    private String groupName;

    private String groupImage;

    private String groupLocation;

    @Column(length = 36)
    private String inviteCode;

    private LocalDateTime promiseTime;
}
