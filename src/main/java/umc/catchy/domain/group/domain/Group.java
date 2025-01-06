package umc.catchy.domain.group.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.common.BaseTimeEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
public class Group extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    private String groupName;
    private String groupImage;
    private String groupLocation;
    private String inviteCode;
    private LocalDateTime promiseTime;
}
