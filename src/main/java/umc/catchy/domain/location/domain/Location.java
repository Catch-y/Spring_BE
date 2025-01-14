package umc.catchy.domain.location.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import umc.catchy.domain.common.BaseTimeEntity;

@Entity
@Getter
public class Location extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long id;

    @NotNull(message = "원하는 지역의 시/도 를 선택하세요.")
    private String upperLocation;
    @NotNull(message = "해당 지역의 내부 구를 선택하세요.")
    private String lowerLocation;
}
