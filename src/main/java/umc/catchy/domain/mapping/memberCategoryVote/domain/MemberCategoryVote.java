package umc.catchy.domain.mapping.memberCategoryVote.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.categoryVote.domain.CategoryVote;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.member.domain.Member;

@Entity
@Getter
public class MemberCategoryVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_categoryVote_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoryVote_id")
    private CategoryVote categoryVote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "vote_id")
    private Long voteId;

    public MemberCategoryVote(Member member, CategoryVote categoryVote, Long voteId) {
        this.member = member;
        this.categoryVote = categoryVote;
        this.voteId = voteId;
    }
    protected MemberCategoryVote() {
    }
}
