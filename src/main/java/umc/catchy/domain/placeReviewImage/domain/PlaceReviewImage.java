package umc.catchy.domain.placeReviewImage.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.placeReview.domain.PlaceReview;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceReviewImage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "placeReviewImage_id")
    private Long id;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placeReview_id")
    private PlaceReview placeReview;
}
