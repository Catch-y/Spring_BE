package umc.catchy.domain.course.util;

public class LocationUtils {
    public static String extractUpperLocation(String roadAddress) {
        if (roadAddress == null || roadAddress.isEmpty()) {
            return "전체 지역";
        }

        String[] parts = roadAddress.split(" ");
        return parts.length > 0 ? parts[0] : "전체 지역"; // 예: "서울특별시"
    }

    public static String extractLowerLocation(String roadAddress) {
        if (roadAddress == null || roadAddress.isEmpty()) {
            return "전체 지역";
        }

        String[] parts = roadAddress.split(" ");
        return parts.length > 1 ? parts[1] : "전체 지역"; // 예: "강남구"
    }
}

