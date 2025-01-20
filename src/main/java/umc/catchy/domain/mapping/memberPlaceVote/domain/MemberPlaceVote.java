package umc.catchy.domain.mapping.memberPlaceVote.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.vote.domain.Vote;

@Entity
@Getter
@NoArgsConstructor
public class MemberPlaceVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_place_vote_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @Builder
    public MemberPlaceVote(Place place, Member member, Vote vote, Groups group) {
        this.place = place;
        this.member = member;
        this.vote = vote;
        this.group = group;
    }
}
