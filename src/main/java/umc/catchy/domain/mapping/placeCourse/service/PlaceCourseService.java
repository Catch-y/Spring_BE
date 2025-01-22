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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfo;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoDetail;
import umc.catchy.domain.place.converter.PlaceConverter;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.dao.PlaceReviewRepository;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

@Service
@Transactional
@RequiredArgsConstructor
public class PlaceCourseService {
    private static final String TMAP_API_URL = "https://apis.openapi.sk.com/tmap/pois";
    private static final String GOOGLE_API_URL = "https://maps.googleapis.com/maps/api/place/";

    @Value("${security.tmap.app-key}")
    private String TMAP_APP_KEY;

    @Value("${security.google.api-key}")
    private String GOOGLE_API_KEY;

    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository placeReviewRepository;

    public List<PlaceInfo> getPlacesByLocation(String searchKeyword, Double latitude, Double longitude, Integer page) {
        List<Long> poiIds = getPoiIds(searchKeyword, latitude, longitude, page);

        return poiIds.stream()
                .map(poiId -> {
                    Optional<Place> place = placeRepository.findByPoiId(poiId);

                    if (place.isPresent()) {
                        Long reviewCount = placeReviewRepository.countByPlaceId(place.get().getId());
                        return PlaceConverter.toPlaceInfo(place.get(), reviewCount);
                    }
                    else {
                        return createPlace(poiId);
                    }
                }).toList();
    }

    public PlaceInfoDetail getPlaceDetailByPlaceId(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

        Long reviewCount = placeReviewRepository.countByPlaceId(placeId);

        return PlaceConverter.toPlaceInfoDetail(place, reviewCount);
    }

    private List<Long> getPoiIds(String keyword, Double latitude, Double longitude, Integer page) {
        try {
            // keyword 인코딩
            String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");

            // URL에 쿼리 파라미터 추가
            String query;

            // 내 위치 기반 검색(장소만 입력했을 때) - 반경 5km, 거리순
            if (latitude != null && longitude != null) {
                query = String.format(
                        "?version=1&searchKeyword=%s&searchType=all&searchtypCd=R&centerLon=%f&centerLat=%f" +
                                "&reqCoordType=WGS84GEO&resCoordType=WGS84GEO&radius=5&page=%d&count=10&multiPoint=Y&poiGroupYn=N",
                        encodedKeyword, longitude, latitude, page
                );
            }
            // 지역 키워드 기반 검색(지역과 장소를 함께 입력)
            else {
                query = String.format(
                        "?version=1&searchKeyword=%s&searchType=all&searchtypCd=A" +
                                "&reqCoordType=WGS84GEO&resCoordType=WGS84GEO&page=%d&count=10&multiPoint=Y&poiGroupYn=N",
                        encodedKeyword, page
                );
            }

            // 연결 설정
            URL url = new URL(TMAP_API_URL + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 헤더 설정
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("appKey", TMAP_APP_KEY);

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
                throw new GeneralException(ErrorStatus.SEARCH_PLACE_NOT_FOUND);
            }

        } catch (IOException e) {
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
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

    private PlaceInfo createPlace(Long poiId) {
        Map<String, String> placeInfo = getPlaceInfo(poiId);

        Place place = PlaceConverter.toPlace(placeInfo);

        placeRepository.save(place);

        return PlaceConverter.toPlaceInfo(place, 0L);
    }

    private Map<String, String> getPlaceInfo(Long poiId) {
        try {
            // URL에 쿼리 파라미터 추가
            String query = String.format(
                    "/%d?version=1&findOption=id&resCoordType=WGS84GEO"
                    , poiId
            );

            // 연결 설정
            URL url = new URL(TMAP_API_URL + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 헤더 설정
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("appKey", TMAP_APP_KEY);

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
                throw new RuntimeException("TMAP POI API ERROR: " + responseCode);
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
        mappedInfo.put("address", poiDetailInfoNode.path("address").asText(null));
        mappedInfo.put("lat", poiDetailInfoNode.path("lat").asText(null));
        mappedInfo.put("lon", poiDetailInfoNode.path("lon").asText(null));
        mappedInfo.put("additionalInfo", poiDetailInfoNode.path("additionalInfo").asText(null));
        mappedInfo.put("homepageURL", poiDetailInfoNode.path("homepageURL").asText(null));

        // 도로명 주소 저장
        String bldNo1 = poiDetailInfoNode.path("bldNo1").asText(null);
        String bldNo2 = poiDetailInfoNode.path("bldNo2").asText(null);

        String bldNum = bldNo1;
        if (!bldNo2.isEmpty()) bldNum += "-" + bldNo2;

        mappedInfo.put("bldAddr", poiDetailInfoNode.path("bldAddr").asText(null) + " " + bldNum);

        // 이미지 불러오기
        String imageUrl = getPlaceImageByName(mappedInfo.get("bldAddr"), mappedInfo.get("name"));

        // 이미지 저장
        mappedInfo.put("image", imageUrl);

        return mappedInfo;
    }

    private String getPlaceImageByName(String address, String placeName) {
        try {
            String searchKeyword = address + " " + placeName;
            String encodedKeyword = URLEncoder.encode(searchKeyword, "UTF-8");

            // URL에 쿼리 파라미터 추가
            String query = String.format(
                    "findplacefromtext/json?fields=photo&input=%s&inputtype=textquery&key=%s",
                    encodedKeyword, GOOGLE_API_KEY
            );

            // 연결 설정
            URL url = new URL(GOOGLE_API_URL + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 헤더 설정
            conn.setRequestMethod("GET");

            // 응답 처리
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("GOOGLE MAP API ERROR: " + responseCode);
            }

            // 응답 읽기
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            // Json 파싱 및 thumbnail 값 추출
            return parsePlaceImageFromJson(response.toString());

        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    private String parsePlaceImageFromJson(String response) throws Exception {
        if (response == null) throw new RuntimeException("응답이 존재하지 않습니다.");

        // ObjectMapper 초기화
        ObjectMapper objectMapper = new ObjectMapper();

        // JSON 파싱
        JsonNode rootNode = objectMapper.readTree(response);

        JsonNode candidates = rootNode.path("candidates");

        // photo_reference 받아오기
        String photoReference = "";

        for (JsonNode candidate : candidates) {
            JsonNode photos = candidate.path("photos");
            for (JsonNode photo : photos) {
                photoReference = photo.path("photo_reference").asText();
            }
        }

        // photo_reference로 photo_url 받아오기
        String query = String.format(
                "photo?maxwidth=400&photoreference=%s&key=GOOGLE_API_KEY",
                photoReference
        );

        return GOOGLE_API_URL + query;
    }
}
