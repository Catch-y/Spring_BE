package umc.catchy.support.fixture;

import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.mapping.memberPlaceVote.domain.MemberPlaceVote;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.vote.domain.Vote;

public class MemberPlaceVoteFixture {
    public static MemberPlaceVote create(Place place, Member member, Vote vote, Groups group) {
        return MemberPlaceVote.builder()
                .place(place)
                .member(member)
                .vote(vote)
                .group(group)
                .build();
    }
}
