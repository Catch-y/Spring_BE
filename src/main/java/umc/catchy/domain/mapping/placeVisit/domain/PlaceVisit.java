package umc.catchy.domain.mapping.placeVisit.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class PlaceVisit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "placeVisit_id")
    private Long id;

    @Setter
    private boolean isVisited;

    @Setter
    private LocalDate visitedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;
}
