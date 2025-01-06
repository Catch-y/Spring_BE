package umc.catchy.domain.place.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.common.BaseTimeEntity;

@Entity
@Getter
public class Place extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long id;

    private String placeName;

    private String description;

    private Double latitude; // 위도

    private Double longitude; // 경도

    @OneToOne
    @JoinColumn(name = "category_id")
    private Category category;

}
