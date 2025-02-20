package umc.catchy.domain.course.util;

import java.util.Map;
import java.util.Set;

public class LocationUtils {

    private static final Map<String, Set<String>> locationAliases = Map.ofEntries(
            Map.entry("서울", Set.of("서울", "서울특별시", "서울시")),
            Map.entry("인천", Set.of("인천", "인천광역시")),
            Map.entry("강원", Set.of("강원", "강원도", "강원특별자치도")),
            Map.entry("경기", Set.of("경기", "경기도")),
            Map.entry("충남", Set.of("충남", "충청남도")),
            Map.entry("충북", Set.of("충북", "충청북도")),
            Map.entry("세종", Set.of("세종", "세종시", "세종특별자치시")),
            Map.entry("경북", Set.of("경북", "경상북도")),
            Map.entry("대구", Set.of("대구", "대구광역시")),
            Map.entry("경남", Set.of("경남", "경상남도")),
            Map.entry("전북", Set.of("전북", "전라북도", "전북특별자치도")),
            Map.entry("전남", Set.of("전남", "전라남도")),
            Map.entry("광주", Set.of("광주", "광주광역시")),
            Map.entry("울산", Set.of("울산", "울산광역시")),
            Map.entry("부산", Set.of("부산", "부산광역시")),
            Map.entry("대전", Set.of("대전", "대전광역시")),

            Map.entry("제주", Set.of("제주", "제주도", "제주특별자치도"))
    );

    public static String normalizeLocation(String location) {
        if (location == null) {
            return null;
        }

        for (Map.Entry<String, Set<String>> entry : locationAliases.entrySet()) {
            for (String alias : entry.getValue()) {
                if (location.startsWith(alias)) {
                    return entry.getKey();
                }
            }
        }

        return location;
    }

    public static String extractUpperLocation(String roadAddress) {
        if (roadAddress == null || roadAddress.isEmpty()) {
            return "전체 지역";
        }

        String[] parts = roadAddress.split(" ");
        if (parts.length > 0) {
            return normalizeLocation(parts[0]);
        }
        return "전체 지역";
    }

    public static String extractLowerLocation(String roadAddress) {
        if (roadAddress == null || roadAddress.isEmpty()) {
            return "전체 지역";
        }

        String[] parts = roadAddress.split(" ");
        return parts.length > 1 ? parts[1] : "전체 지역"; // 예: "강남구"
    }
}