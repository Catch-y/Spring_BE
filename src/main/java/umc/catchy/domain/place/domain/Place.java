package umc.catchy.domain.place.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.course.util.LocationUtils;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "place",
        indexes = {
                @Index(name = "idx_place_search", columnList = "category_id, sido, sigungu"),
                @Index(name = "idx_place_recommendation", columnList = "category_id, latitude, longitude, start_time, end_time")
        }
)
public class Place extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long id;

    private Long poiId;

    private String placeName;

    @Column(length = 50000)
    private String placeDescription;

    @Column(length = 255)
    private String roadAddress;

    @Column(length = 20)
    private String sido;

    @Column(length = 20)
    private String sigungu;

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

    @PrePersist
    @PreUpdate
    public void preUpdateAddress() {
        if (this.roadAddress != null && !this.roadAddress.isEmpty()) {
            this.sido = LocationUtils.extractUpperLocation(this.roadAddress); // "서울"
            this.sigungu = LocationUtils.extractLowerLocation(this.roadAddress); // "강남구"
        }
    }

    public void assignCategory(Category category) {
        if (this.category != null) {
            throw new GeneralException(ErrorStatus.PLACE_CATEGORY_EXIST);
        }
        this.category = category;
    }

    public void updateRating(Double newRating) {
        this.rating = newRating;
    }

    public static Place fromGoogleInfo(Long poiId, Map<String, String> googleInfo) {
        return Place.builder()
                .poiId(poiId)
                .placeName(googleInfo.get("name"))
                .placeDescription(googleInfo.get("description"))
                .roadAddress(googleInfo.get("address"))
                .latitude(parseDouble(googleInfo.get("lat")))
                .longitude(parseDouble(googleInfo.get("lon")))
                .activeTime(googleInfo.get("activeTime"))
                .startTime(parseLocalTime(googleInfo.get("startTime")))
                .endTime(parseLocalTime(googleInfo.get("endTime")))
                .placeSite(googleInfo.get("website"))
                .imageUrl(googleInfo.get("imageUrl"))
                .build();
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalTime parseLocalTime(String time) {
        if (time == null || time.isBlank()) return null;
        try {
            return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return null;
        }
    }
}
