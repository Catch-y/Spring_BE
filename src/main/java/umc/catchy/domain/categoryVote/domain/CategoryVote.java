package umc.catchy.domain.categoryVote.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.vote.domain.Vote;

@Entity
@Getter
public class CategoryVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "categoryVote_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private BigCategory bigCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id")
    private Vote vote;
}
