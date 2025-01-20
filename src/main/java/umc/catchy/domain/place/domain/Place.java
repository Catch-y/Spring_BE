package umc.catchy.domain.place.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.common.BaseTimeEntity;

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

    private String placeDescription;

    private String roadAddress; //도로명 주소

    private String numberAddress; // 지번 주소

    private Double latitude; // 위도

    private Double longitude; // 경도

    private String activeTime; // 영업시간

    private String placeSite; // 장소 사이트

    private String imageUrl; // 장소 이미지

    @Setter
    private Double rating; // 장소 총 평점 : 처음에 0으로 초기화해주세요

    @OneToOne
    @JoinColumn(name = "category_id")
    private Category category;

}
