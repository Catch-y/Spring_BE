package umc.catchy.domain.vote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.group.domain.Groups;

import java.time.LocalDateTime;

@Entity
@Getter
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

    @Builder
    public Vote(VoteStatus status, LocalDateTime endTime, Groups group) {
        this.status = status;
        this.endTime = endTime;
        this.group = group;
    }

    public void changeStatus(VoteStatus status) {
        this.status = status;
    }
}