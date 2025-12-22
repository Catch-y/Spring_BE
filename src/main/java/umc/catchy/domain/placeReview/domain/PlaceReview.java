package umc.catchy.domain.placeReview.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceReview extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "placeReview_id")
    private Long id;

    private Integer rating;

    private String comment;

    private LocalDate visitedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Builder.Default
    private Boolean isReported = false;

    public void markAsReported() {
        this.isReported = true;
    }

    public static PlaceReview create(Member member, Place place, Integer rating, String comment, LocalDate visitedDate) {
        return PlaceReview.builder()
                .rating(rating)
                .comment(comment)
                .visitedDate(visitedDate)
                .member(member)
                .place(place)
                .build();
    }
}
