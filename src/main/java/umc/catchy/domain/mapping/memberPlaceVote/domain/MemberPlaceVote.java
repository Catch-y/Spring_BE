package umc.catchy.domain.mapping.memberPlaceVote.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.vote.domain.Vote;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_place_vote", indexes = {
        @Index(name = "idx_membervote_place_group", columnList = "place_id, group_id")
})
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

    public static MemberPlaceVote create(Place place, Member member, Vote vote, Groups group) {
        return MemberPlaceVote.builder()
                .place(place)
                .member(member)
                .vote(vote)
                .group(group)
                .build();
    }
}
