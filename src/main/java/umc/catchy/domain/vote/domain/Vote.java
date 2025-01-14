package umc.catchy.domain.vote.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.group.domain.Groups;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
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

    public static Vote createVote(Groups group) {
        Vote vote = new Vote();
        vote.status = VoteStatus.IN_PROGRESS;
        vote.endTime = LocalDateTime.now().plusDays(1);
        vote.group = group;
        return vote;
    }
}