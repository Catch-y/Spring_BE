package umc.catchy.domain.vote.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.group.domain.Groups;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private VoteStatus status;

    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Groups group;

    public static Vote create(Groups group) {
        return Vote.builder()
                .status(VoteStatus.IN_PROGRESS)
                .endTime(LocalDateTime.now().plusDays(1))
                .group(group)
                .build();
    }

    public void changeStatus(VoteStatus status) {
        this.status = status;
    }
}
