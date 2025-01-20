package umc.catchy.domain.mapping.placeCourse.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoResponse;
import umc.catchy.domain.place.converter.PlaceConverter;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class PlaceCourseService {

    private static final String APP_KEY = "yrBgObXPpI1JaoLesS3g79MWrtlSNrkT6xeabWMz";
    private static final String API_URL = "https://apis.openapi.sk.com/tmap/pois";

    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository placeReviewRepository;

    public List<PlaceInfoResponse> getPlacesByMemberLocation(String searchKeyword, Float latitude, Float longitude) {
        List<Long> poiIds = getPoiIds(searchKeyword, latitude, longitude);

        return poiIds.stream()
                .map(poiId -> {
                    Optional<Place> place = placeRepository.findByPoiId(poiId);

                    if (place.isPresent()) {
                        Long reviewCount = placeReviewRepository.countByPlaceId(place.get().getId());
                        return PlaceConverter.toPlaceInfoResponse(place.get(), reviewCount);
                    }
                    else {
                        return createPlace(poiId);
                    }
                }).toList();
    }

    private List<Long> getPoiIds(String keyword, Float latitude, Float longitude) {
        try {
            // keyword 인코딩
            String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");

            // URL에 쿼리 파라미터 추가
            String query = String.format(
                    "?version=1&searchKeyword=%s&searchType=all&searchtypCd=A&centerLon=%f&centerLat=%f" +
                            "&reqCoordType=WGS84GEO&resCoordType=WGS84GEO&radius=5&page=1&count=20&multiPoint=N&poiGroupYn=N",
                    encodedKeyword, longitude, latitude
            );

            // 연결 설정
            URL url = new URL(API_URL + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 헤더 설정
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("appKey", APP_KEY);

            // 응답 처리
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return parsePoiIds(response.toString());
            } else {
                throw new RuntimeException("HTTP 응답 코드: " + responseCode);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Long> parsePoiIds(String jsonResponse) throws IOException {
        // Jackson ObjectMapper 초기화
        ObjectMapper objectMapper = new ObjectMapper();

        // JSON 파싱
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode poisNode = rootNode.path("searchPoiInfo").path("pois").path("poi");

        // ID 리스트 추출
        List<Long> poiIds = new ArrayList<>();
        if (poisNode.isArray()) {
            for (JsonNode poiNode : poisNode) {
                Long id = Long.parseLong(poiNode.path("id").asText());

                // 중복 방지
                if (poiIds.contains(id)) continue;

                poiIds.add(id);
            }
        }

        return poiIds;
    }

    private PlaceInfoResponse createPlace(Long poiId) {
        Map<String, String> placeInfo = getPlaceInfo(poiId);

        Place place = PlaceConverter.toPlace(placeInfo);

        return PlaceConverter.toPlaceInfoResponse(place, 0L);
    }

    private Map<String, String> getPlaceInfo(Long poiId) {
        try {
            // URL에 쿼리 파라미터 추가
            String query = String.format(
                    "/%d?version=1&findOption=id&resCoordType=WGS84GEO"
                    , poiId
            );

            // 연결 설정
            URL url = new URL(API_URL + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 헤더 설정
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("appKey", APP_KEY);

            // 응답 처리
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return getPlaceMapByResponse(response.toString());

            } else {
                throw new RuntimeException("HTTP 응답 코드: " + responseCode);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getPlaceMapByResponse(String response) throws JsonProcessingException {
        // ObjectMapper 초기화
        ObjectMapper objectMapper = new ObjectMapper();

        // JSON 파싱
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode poiDetailInfoNode = rootNode.path("poiDetailInfo");

        // Map에 정보 저장
        Map<String, String> mappedInfo = new HashMap<>();
        mappedInfo.put("id", poiDetailInfoNode.path("id").asText(null));
        mappedInfo.put("name", poiDetailInfoNode.path("name").asText(null));
        mappedInfo.put("desc", poiDetailInfoNode.path("desc").asText(null));
        mappedInfo.put("bldAddr", poiDetailInfoNode.path("bldAddr").asText(null));
        mappedInfo.put("address", poiDetailInfoNode.path("address").asText(null));
        mappedInfo.put("lat", poiDetailInfoNode.path("lat").asText(null));
        mappedInfo.put("lon", poiDetailInfoNode.path("lon").asText(null));
        mappedInfo.put("additionalInfo", poiDetailInfoNode.path("additionalInfo").asText(null));
        mappedInfo.put("homepageURL", poiDetailInfoNode.path("homepageURL").asText(null));

        return mappedInfo;
    }
}
