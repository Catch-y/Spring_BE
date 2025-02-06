package umc.catchy.domain.mapping.placeLike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

@Entity
@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class PlaceLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "placeLike_id")
    private Long id;

    private boolean isLiked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    public static void toggleLiked(PlaceLike placeLike) {
        placeLike.isLiked = !placeLike.isLiked;
    }
}
