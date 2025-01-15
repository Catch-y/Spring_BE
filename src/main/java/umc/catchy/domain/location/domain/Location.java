package umc.catchy.domain.location.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import umc.catchy.domain.common.BaseTimeEntity;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Location extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long id;

    @NotNull(message = "원하는 지역의 시/도 를 선택하세요.")
    private String upperLocation;
    @NotNull(message = "해당 지역의 내부 구를 선택하세요.")
    private String lowerLocation;

    public static Location createLocation(String upperLocation, String lowerLocation) {
        return Location.builder().upperLocation(upperLocation).lowerLocation(lowerLocation).build();
    }
}
