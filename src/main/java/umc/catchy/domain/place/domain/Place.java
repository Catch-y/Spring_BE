package umc.catchy.domain.place.domain;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.course.util.LocationUtils;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

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

    public static Place fromTmapInfo(Map<String, String> placeInfo) {
        List<String> parsedTime = parsingTime(placeInfo.get("additionalInfo"));

        return Place.builder()
                .poiId(Long.parseLong(placeInfo.get("id")))
                .placeName(placeInfo.get("name"))
                .imageUrl(placeInfo.get("image"))
                .placeDescription(placeInfo.get("desc"))
                .roadAddress(placeInfo.get("bldAddr"))
                .numberAddress(placeInfo.get("address"))
                .latitude(Double.parseDouble(placeInfo.get("lat")))
                .longitude(Double.parseDouble(placeInfo.get("lon")))
                .activeTime(placeInfo.get("additionalInfo"))
                .startTime(parsedTime.isEmpty() ? null : formatTime(parsedTime.get(0)))
                .endTime(parsedTime.isEmpty() ? null : formatTime(parsedTime.get(1)))
                .placeSite(placeInfo.get("homepageURL"))
                .build();
    }

    private static List<String> parsingTime(String activeTime) {
        if (activeTime == null) return new ArrayList<>();
        List<String> timeRange = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b\\d{2}:\\d{2}~\\d{2}:\\d{2}\\b");
        Matcher matcher = pattern.matcher(activeTime);

        while (matcher.find()) {
            timeRange = Arrays.stream(matcher.group().split("~")).toList();
        }
        return timeRange;
    }

    private static LocalTime formatTime(String time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return LocalTime.parse(time, formatter);
    }
}
