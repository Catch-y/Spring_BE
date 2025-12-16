package umc.catchy.domain.course.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.course.dto.response.GptPlaceInfoDto;
import umc.catchy.domain.course.dto.response.GptPlaceInfoResponse;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GptResponseParser {

    private static final String DEFAULT_COURSE_NAME = "AI 추천 코스";
    private static final String DEFAULT_COURSE_DESCRIPTION = "AI가 추천한 여행 코스입니다.";
    private static final String DEFAULT_TIME_RANGE = "09:00~21:00";

    private static final String KEY_COURSE_NAME = "courseName";
    private static final String KEY_COURSE_DESC = "courseDescription";
    private static final String KEY_RECOMMEND_TIME = "recommendTime";
    private static final String KEY_PLACES = "places";
    private static final String KEY_PLACE_ID = "placeId";

    private final ObjectMapper objectMapper;
    private final PlaceRepository placeRepository;

    public GptCourseInfoResponse parse(String gptResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(gptResponse);
            JsonNode choicesNode = rootNode.path("choices");

            if (!choicesNode.isArray() || choicesNode.size() == 0) {
                throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR);
            }

            String content = extractContent(choicesNode);
            JsonNode contentNode = objectMapper.readTree(content);

            String courseName = contentNode.path(KEY_COURSE_NAME).asText(DEFAULT_COURSE_NAME);
            String courseDescription = contentNode.path(KEY_COURSE_DESC).asText(DEFAULT_COURSE_DESCRIPTION);
            String recommendTime = contentNode.path(KEY_RECOMMEND_TIME).asText(DEFAULT_TIME_RANGE);

            List<Long> placeIds = extractPlaceIds(contentNode);
            List<GptPlaceInfoResponse> placeInfos = fetchPlaceInfos(placeIds);

            return new GptCourseInfoResponse(courseName, courseDescription, recommendTime, placeInfos);

        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR);
        }
    }

    private String extractContent(JsonNode choicesNode) {
        String content = choicesNode.get(0).path("message").path("content").asText();
        return content.replaceAll("```json", "").replaceAll("```", "").trim();
    }

    private List<Long> extractPlaceIds(JsonNode contentNode) {
        List<Long> placeIds = new ArrayList<>();
        JsonNode placesNode = contentNode.path(KEY_PLACES);

        if (placesNode.isArray()) {
            for (JsonNode placeNode : placesNode) {
                placeIds.add(placeNode.path(KEY_PLACE_ID).asLong());
            }
        }

        return placeIds;
    }

    private List<GptPlaceInfoResponse> fetchPlaceInfos(List<Long> placeIds) {
        List<GptPlaceInfoDto> placeInfoDtos = placeRepository.findPlacesWithCategoryAndReviewCount(placeIds);
        return placeInfoDtos.stream()
                .map(GptPlaceInfoDto::toResponse)
                .toList();
    }
}
