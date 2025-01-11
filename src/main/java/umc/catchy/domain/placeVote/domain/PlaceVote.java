package umc.catchy.domain.placeVote.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.vote.domain.Vote;

@Entity
@Getter
public class PlaceVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "placeVote_id")
    private Long id;

    @Column(length = 20)
    private String item; // 장소명

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;
}
