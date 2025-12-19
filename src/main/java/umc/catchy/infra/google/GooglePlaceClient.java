package umc.catchy.infra.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePlaceClient {

    private static final String SEARCH_TEXT_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String FIELD_MASK = "places.displayName,places.formattedAddress,places.location,places.regularOpeningHours,places.websiteUri,places.internationalPhoneNumber,places.photos,places.editorialSummary,places.addressComponents";

    @Value("${map.google.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> getPlaceInfo(String placeName, String address, Double latitude, Double longitude) {
        try {
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("textQuery", (address != null ? address + " " : "") + placeName);
            rootNode.put("languageCode", "ko");

            ObjectNode locationBias = rootNode.putObject("locationBias");
            ObjectNode circle = locationBias.putObject("circle");
            ObjectNode center = circle.putObject("center");
            center.put("latitude", latitude);
            center.put("longitude", longitude);
            circle.put("radius", 500.0);

            String response = sendPostRequest(SEARCH_TEXT_URL, rootNode.toString());
            return parseNewPlaceResponse(response);

        } catch (IOException e) {
            log.error("Google Places API (New) 호출 실패: placeName={}", placeName, e);
            throw new GeneralException(ErrorStatus.SEARCH_PLACE_NOT_FOUND);
        }
    }

    private String sendPostRequest(String urlString, String jsonBody) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("X-Goog-Api-Key", apiKey);
        conn.setRequestProperty("X-Goog-FieldMask", FIELD_MASK);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP 오류: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return response.toString();
    }

    private Map<String, String> parseNewPlaceResponse(String response) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        JsonNode places = root.path("places");

        if (places.isEmpty() || !places.isArray()) {
            throw new GeneralException(ErrorStatus.SEARCH_PLACE_NOT_FOUND);
        }

        JsonNode result = places.get(0);
        Map<String, String> details = new HashMap<>();

        details.put("name", result.path("displayName").path("text").asText(null));
        details.put("address", result.path("formattedAddress").asText(null).replaceFirst("^대한민국\\s+", ""));
        details.put("phone", result.path("internationalPhoneNumber").asText(null));
        details.put("website", result.path("websiteUri").asText(null));

        JsonNode editorialSummary = result.path("editorialSummary");
        details.put("description", editorialSummary.isMissingNode() ? null : editorialSummary.path("text").asText(null));

        JsonNode location = result.path("location");
        details.put("lat", String.valueOf(location.path("latitude").asDouble()));
        details.put("lon", String.valueOf(location.path("longitude").asDouble()));

        JsonNode addressComponents = result.path("addressComponents");
        if (addressComponents.isArray()) {
            for (JsonNode component : addressComponents) {
                JsonNode types = component.path("types");
                for (JsonNode type : types) {
                    String typeStr = type.asText();
                    if ("administrative_area_level_1".equals(typeStr)) {
                        details.put("sido", component.path("longText").asText(null));
                    } else if ("sublocality_level_1".equals(typeStr)) {
                        details.put("sigungu", component.path("longText").asText(null));
                    }
                }
            }
        }

        JsonNode openingHours = result.path("regularOpeningHours");
        if (!openingHours.isMissingNode()) {
            parseOpeningHours(openingHours, details);
        } else {
            details.put("activeTime", "영업시간 미등록");
            details.put("startTime", null);
            details.put("endTime", null);
        }

        JsonNode photos = result.path("photos");
        if (photos.isArray() && photos.size() > 0) {
            String photoName = photos.get(0).path("name").asText(null);
            if (photoName != null) {
                details.put("imageUrl", String.format("https://places.googleapis.com/v1/%s/media?maxHeightPx=400&key=%s", photoName, apiKey));
            }
        }

        return details;
    }

    private void parseOpeningHours(JsonNode openingHours, Map<String, String> details) {
        JsonNode periods = openingHours.path("periods");

        if (periods.isEmpty() || !periods.isArray()) {
            details.put("activeTime", "영업시간 미등록");
            details.put("startTime", null);
            details.put("endTime", null);
            return;
        }

        int earliestOpen = 2400;
        int latestClose = 0;
        String firstOpenTime = null;
        String firstCloseTime = null;
        boolean allSame = true;

        for (JsonNode period : periods) {
            JsonNode open = period.path("open");
            JsonNode close = period.path("close");

            if (open.isMissingNode()) continue;

            String openTime = String.format("%02d%02d", open.path("hour").asInt(), open.path("minute").asInt());
            String closeTime = close.isMissingNode() ? "2359" : String.format("%02d%02d", close.path("hour").asInt(), close.path("minute").asInt());

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

        details.put("startTime", formatTime(earliestOpen));
        details.put("endTime", formatTime(latestClose));

        if (allSame && firstOpenTime != null) {
            details.put("activeTime", String.format("매일 · %s - %s",
                    formatTime(Integer.parseInt(firstOpenTime)),
                    formatTime(Integer.parseInt(firstCloseTime))));
        } else {
            details.put("activeTime", String.format("요일별 상이 (%s - %s)",
                    formatTime(earliestOpen),
                    formatTime(latestClose)));
        }
    }

    private String formatTime(int time) {
        if (time >= 2400) return null;
        return String.format("%02d:%02d", time / 100, time % 100);
    }
}
