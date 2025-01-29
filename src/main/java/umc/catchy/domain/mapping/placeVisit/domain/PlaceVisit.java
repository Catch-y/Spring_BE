package umc.catchy.domain.mapping.placeVisit.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
public class PlaceVisit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "placeVisit_id")
    private Long id;

    private boolean isVisited;

    private LocalDate visitedDate;

    private boolean isLiked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    public static void toggleLiked(PlaceVisit placeVisit) {
        placeVisit.isLiked = !placeVisit.isLiked;
    }
}
