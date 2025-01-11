package umc.catchy.domain.placeReviewImage.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.placeReview.domain.PlaceReview;

@Entity
@Getter
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
