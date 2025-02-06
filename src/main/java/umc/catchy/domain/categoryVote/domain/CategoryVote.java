package umc.catchy.domain.categoryVote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.vote.domain.Vote;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CategoryVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_vote_id")
    private Long id;

    @Column(name = "big_category")
    @Enumerated(EnumType.STRING)
    private BigCategory bigCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id")
    private Vote vote;
}
