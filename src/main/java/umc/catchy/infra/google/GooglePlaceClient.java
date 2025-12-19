package umc.catchy.infra.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePlaceClient {

    private static final String FIND_PLACE_URL = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json";
    private static final String PLACE_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json";
    private static final String PLACE_PHOTO_URL = "https://maps.googleapis.com/maps/api/place/photo";

    @Value("${map.google.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String findPlaceId(String placeName, String address, Double latitude, Double longitude) {
        try {
            String input = buildSearchInput(placeName, address);
            String locationBias = String.format("point:%f,%f", latitude, longitude);

            String query = String.format(
                    "?input=%s&inputtype=textquery&locationbias=%s&fields=place_id&language=ko&key=%s",
                    URLEncoder.encode(input, "UTF-8"),
                    URLEncoder.encode(locationBias, "UTF-8"),
                    apiKey
            );

            String response = sendGetRequest(FIND_PLACE_URL + query);
            return parsePlaceId(response);

        } catch (IOException e) {
            log.error("Google Find Place API 호출 실패: placeName={}, address={}", placeName, address, e);
            throw new GeneralException(ErrorStatus.SEARCH_PLACE_NOT_FOUND);
        }
    }

    public Map<String, String> getPlaceDetails(String placeId) {
        try {
            String query = String.format(
                    "?place_id=%s&fields=name,formatted_address,geometry,opening_hours,website,formatted_phone_number,photos,editorial_summary&language=ko&key=%s",
                    placeId,
                    apiKey
            );

            String response = sendGetRequest(PLACE_DETAILS_URL + query);
            return parsePlaceDetails(response);

        } catch (IOException e) {
            log.error("Google Place Details API 호출 실패: placeId={}", placeId, e);
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    public String getPhotoUrl(String photoReference) {
        if (photoReference == null || photoReference.isBlank()) {
            return null;
        }

        return String.format(
                "%s?maxwidth=400&photoreference=%s&key=%s",
                PLACE_PHOTO_URL,
                photoReference,
                apiKey
        );
    }

    private String buildSearchInput(String placeName, String address) {
        if (address != null && !address.isBlank()) {
            return address + " " + placeName;
        }
        return placeName;
    }

    private String sendGetRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP 오류: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return response.toString();
    }

    private String parsePlaceId(String response) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        JsonNode candidates = root.path("candidates");

        if (candidates.isEmpty()) {
            throw new GeneralException(ErrorStatus.SEARCH_PLACE_NOT_FOUND);
        }

        return candidates.get(0).path("place_id").asText();
    }

    private Map<String, String> parsePlaceDetails(String response) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        JsonNode result = root.path("result");

        Map<String, String> details = new HashMap<>();

        // 기본 정보
        details.put("name", result.path("name").asText(null));
        details.put("address", result.path("formatted_address").asText(null));
        details.put("phone", result.path("formatted_phone_number").asText(null));
        details.put("website", result.path("website").asText(null));

        // 설명
        JsonNode editorialSummary = result.path("editorial_summary");
        if (!editorialSummary.isMissingNode()) {
            details.put("description", editorialSummary.path("overview").asText(null));
        } else {
            details.put("description", null);
        }

        // 좌표
        JsonNode location = result.path("geometry").path("location");
        details.put("lat", String.valueOf(location.path("lat").asDouble()));
        details.put("lon", String.valueOf(location.path("lng").asDouble()));

        // 영업시간 파싱
        JsonNode openingHours = result.path("opening_hours");
        if (!openingHours.isMissingNode()) {
            parseOpeningHours(openingHours, details);
        } else {
            details.put("activeTime", "영업시간 미등록");
            details.put("startTime", null);
            details.put("endTime", null);
        }

        // 사진 (첫 번째만)
        JsonNode photos = result.path("photos");
        if (photos.isArray() && photos.size() > 0) {
            String photoReference = photos.get(0).path("photo_reference").asText(null);
            if (photoReference != null) {
                details.put("photoReference", photoReference);
                details.put("imageUrl", getPhotoUrl(photoReference));
            }
        }

        return details;
    }

    /**
     * 영업시간 파싱
     * - activeTime: "매일 · 07:00 - 23:00" (표시용)
     * - startTime: "07:00" (알고리즘용)
     * - endTime: "23:00" (알고리즘용)
     */
    private void parseOpeningHours(JsonNode openingHours, Map<String, String> details) {
        JsonNode periods = openingHours.path("periods");

        if (periods.isEmpty() || !periods.isArray()) {
            details.put("activeTime", "영업시간 미등록");
            details.put("startTime", null);
            details.put("endTime", null);
            return;
        }

        // 가장 이른 오픈 시간 & 가장 늦은 마감 시간 찾기
        int earliestOpen = 2400;
        int latestClose = 0;

        String firstOpenTime = null;
        String firstCloseTime = null;
        boolean allSame = true;

        for (JsonNode period : periods) {
            JsonNode open = period.path("open");
            JsonNode close = period.path("close");

            if (open.isMissingNode()) continue;

            String openTime = open.path("time").asText("2400");
            String closeTime = close.isMissingNode() ? "2359" : close.path("time").asText("0000");

            int openInt = Integer.parseInt(openTime);
            int closeInt = Integer.parseInt(closeTime);

            if (firstOpenTime == null) {
                firstOpenTime = openTime;
                firstCloseTime = closeTime;
            } else if (!openTime.equals(firstOpenTime) || !closeTime.equals(firstCloseTime)) {
                allSame = false;
            }

            earliestOpen = Math.min(earliestOpen, openInt);
            latestClose = Math.max(latestClose, closeInt);
        }

        // startTime, endTime 설정 (알고리즘용)
        details.put("startTime", formatTime(earliestOpen));
        details.put("endTime", formatTime(latestClose));

        // activeTime 요약 (표시용)
        if (allSame && firstOpenTime != null) {
            // 모든 요일 동일: "매일 · 07:00 - 23:00"
            details.put("activeTime", String.format("매일 · %s - %s",
                    formatTime(Integer.parseInt(firstOpenTime)),
                    formatTime(Integer.parseInt(firstCloseTime))));
        } else {
            // 요일별 상이: "요일별 상이 (07:00 - 23:00)"
            details.put("activeTime", String.format("요일별 상이 (%s - %s)",
                    formatTime(earliestOpen),
                    formatTime(latestClose)));
        }
    }

    /**
     * 시간 포맷 변환: 0700 → 07:00
     */
    private String formatTime(int time) {
        if (time >= 2400) return null;
        return String.format("%02d:%02d", time / 100, time % 100);
    }
}
