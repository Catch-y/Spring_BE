package umc.catchy.domain.place.domain;

import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Place extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long id;

    private Long poiId;

    private String placeName;

    @Column(length = 50000)
    private String placeDescription;

    private String roadAddress;

    private String numberAddress;

    private Double latitude;

    private Double longitude;

    private String activeTime;

    private LocalTime startTime;

    private LocalTime endTime;

    private String placeSite;

    @Column(length = 50000)
    private String imageUrl;

    @Builder.Default
    private Double rating = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    public void assignCategory(Category category) {
        if (this.category != null) {
            throw new GeneralException(ErrorStatus.PLACE_CATEGORY_EXIST);
        }
        this.category = category;
    }

    public void updateRating(Double newRating) {
        this.rating = newRating;
    }
}
