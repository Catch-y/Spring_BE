package umc.catchy.domain.course.util;

import java.util.Map;

public class LocationUtils {

    private static final Map<String, String> locationCorrections = Map.ofEntries(
            Map.entry("서울시", "서울"),
            Map.entry("강원도", "강원"),
            Map.entry("경기도", "경기"),
            Map.entry("충청남도", "충남"),
            Map.entry("충청북도", "충북"),
            Map.entry("세종시", "세종"),
            Map.entry("경상북도", "경북"),
            Map.entry("경상남도", "경남"),
            Map.entry("전라북도", "전북"),
            Map.entry("전라남도", "전남"),
            Map.entry("제주도", "제주")
    );

    public static String normalizeLocation(String location) {
        if (location == null) {
            return null;
        }

        for (Map.Entry<String, String> entry : locationCorrections.entrySet()) {
            if (location.startsWith(entry.getKey())) {
                String normalized = location.replace(entry.getKey(), entry.getValue());
                return normalized;
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