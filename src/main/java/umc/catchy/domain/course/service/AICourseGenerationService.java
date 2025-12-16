package umc.catchy.domain.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.course.dto.response.GptPlaceInfoDto;
import umc.catchy.domain.course.dto.response.GptPlaceInfoResponse;
import umc.catchy.domain.course.util.LocationUtils;
import umc.catchy.domain.mapping.memberActivetime.dao.MemberActiveTimeRepository;
import umc.catchy.domain.mapping.memberCategory.dao.MemberCategoryRepository;
import umc.catchy.domain.mapping.memberLocation.dao.MemberLocationRepository;
import umc.catchy.domain.mapping.memberLocation.domain.MemberLocation;
import umc.catchy.domain.mapping.memberStyle.dao.MemberStyleRepository;
import umc.catchy.domain.location.domain.Location;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AICourseGenerationService {

    private static final int MAX_PLACE_CANDIDATES = 100;
    private static final int GPT_MAX_TOKENS = 500;
    private static final double GPT_TEMPERATURE = 0.7;
    private static final int RANDOM_POOL_MULTIPLIER = 3;

    private static final String DEFAULT_COURSE_NAME = "AI 추천 코스";
    private static final String DEFAULT_COURSE_DESCRIPTION = "AI가 추천한 여행 코스입니다.";
    private static final String DEFAULT_TIME_RANGE = "09:00~21:00";
    private static final String LOCATION_ALL = "전체";

    private static final String KEY_COURSE_NAME = "courseName";
    private static final String KEY_COURSE_DESC = "courseDescription";
    private static final String KEY_RECOMMEND_TIME = "recommendTime";
    private static final String KEY_PLACES = "places";
    private static final String KEY_PLACE_ID = "placeId";
    private static final String KEY_PLACE_NAME = "name";
    private static final String KEY_PLACE_ADDRESS = "roadAddress";
    private static final String KEY_PLACE_HOURS = "operatingHours";

    private final WebClient gptWebClient;

    @Value("${openai.model}")
    private String openAiModel;

    private final MemberRepository memberRepository;
    private final MemberLocationRepository memberLocationRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberStyleRepository memberStyleRepository;
    private final MemberActiveTimeRepository memberActiveTimeRepository;
    private final CategoryRepository categoryRepository;
    private final PlaceRepository placeRepository;

    public CompletableFuture<GptCourseInfoResponse> generateCourseAutomatically(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<MemberLocation> memberLocations = memberLocationRepository.findAllByMemberId(memberId);
        List<String> preferredCategories = getPreferredCategories(memberId);
        List<String> userStyles = getUserStyles(memberId);
        List<String> activeTimes = getUserActiveTimes(memberId);

        if (memberLocations.isEmpty()) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO);
        }

        List<String> regionList = memberLocations.stream()
                .map(this::formatRegionString)
                .toList();

        List<Long> preferredCategoryIds = categoryRepository.findIdsByNames(preferredCategories);
        List<Place> places = getRecommendedPlacesForPrompt(regionList, preferredCategoryIds, memberId, MAX_PLACE_CANDIDATES);

        String gptPrompt = buildGptPrompt(regionList, places, preferredCategories, userStyles, activeTimes);

        return CompletableFuture.supplyAsync(() -> callOpenAiApiAsync(gptPrompt).join())
                .thenApply(this::parseGptResponseToDto)
                .exceptionally(e -> {
                    throw new GeneralException(ErrorStatus.GPT_API_CALL_FAILED);
                });
    }

    public CompletableFuture<List<GptCourseInfoResponse>> generateMultipleAICourses(Long memberId, int count) {
        List<CompletableFuture<GptCourseInfoResponse>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            futures.add(generateCourseAutomatically(memberId));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    @Transactional
    public void increaseGptCount(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        member.increaseGptCount();
    }

    private CompletableFuture<String> callOpenAiApiAsync(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", openAiModel,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", GPT_MAX_TOKENS,
                "temperature", GPT_TEMPERATURE
        );

        return gptWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .toFuture();
    }

    private List<Place> getRecommendedPlacesForPrompt(List<String> regionList, List<Long> preferredCategoryIds, Long memberId, int maxPlaces) {
        List<String> upperRegions = regionList.stream()
                .map(LocationUtils::extractUpperLocation)
                .map(LocationUtils::normalizeLocation)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<String> lowerRegions = regionList.stream()
                .map(LocationUtils::extractLowerLocation)
                .map(LocationUtils::normalizeLocation)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Place> recommendedPlaces = placeRepository.findRecommendedPlaces(preferredCategoryIds, upperRegions, lowerRegions, memberId, maxPlaces);

        int subsetSize = Math.min(RANDOM_POOL_MULTIPLIER * maxPlaces, recommendedPlaces.size());
        List<Place> topPlaces = recommendedPlaces.subList(0, subsetSize);
        Collections.shuffle(topPlaces);

        return topPlaces.stream().limit(maxPlaces).toList();
    }

    private String formatRegionString(MemberLocation memberLocation) {
        Location location = memberLocation.getLocation();
        String upper = location.getUpperLocation();
        String lower = location.getLowerLocation();

        if (lower == null || lower.equals(LOCATION_ALL)) {
            return upper + " " + LOCATION_ALL;
        }
        return upper + " " + lower;
    }

    private String buildGptPrompt(List<String> regionList, List<Place> places, List<String> preferredCategories, List<String> userStyles, List<String> activeTimes) {
        StringBuilder prompt = new StringBuilder();

        appendIntroAndRegions(prompt, regionList);
        appendUserPreferences(prompt, preferredCategories, userStyles, activeTimes);
        appendPlaceCandidates(prompt, places);
        appendJsonOutputFormat(prompt);

        return prompt.toString();
    }

    private void appendIntroAndRegions(StringBuilder prompt, List<String> regionList) {
        String introTemplate = """
                Create a unique and creative itinerary for the following regions: %s.
                Randomly select **2 to 5** unique places from the list below to create a diverse and interesting itinerary.
                Do not include all places in the itinerary.
                """;
        prompt.append(introTemplate.formatted(String.join(", ", regionList)));
    }

    private void appendUserPreferences(StringBuilder prompt, List<String> preferredCategories, List<String> userStyles, List<String> activeTimes) {
        prompt.append("The user's preferred categories are: ").append(String.join(", ", preferredCategories)).append(".\n");

        if (!userStyles.isEmpty()) {
            prompt.append("The user prefers the following styles: ").append(String.join(", ", userStyles)).append(".\n");
        }

        if (!activeTimes.isEmpty()) {
            prompt.append("The user's preferred active times are: ").append(String.join(", ", activeTimes)).append(".\n");
        }
    }

    private void appendPlaceCandidates(StringBuilder prompt, List<Place> places) {
        prompt.append("Here are the places to choose from:\n");
        for (Place place : places) {
            prompt.append(String.format(
                    "- Place ID: %d, Name: %s, Road Address: %s, Operating Hours: %s, Category: %s, Description: %s\n",
                    place.getId(), place.getPlaceName(), place.getRoadAddress(), place.getActiveTime(), place.getCategory().getName(), place.getPlaceDescription()
            ));
        }
    }

    private void appendJsonOutputFormat(StringBuilder prompt) {
        String jsonInstructions = """
                
                The course name and description must be written in Korean.
                The course description should be concise, no more than 80 characters.
                The response should include a course name, course description, recommended visit time.
                Please return only the JSON structure below without any additional text, comments, or markdown formatting (e.g., no ```json). Return only the raw JSON structure:
                """;

        String jsonStructure = """
                {
                  "%s": "string (in Korean)",
                  "%s": "string (in Korean)",
                  "%s": "HH:mm~HH:mm",
                  "%s": [
                    {
                      "%s": "numeric",
                      "%s": "string",
                      "%s": "string",
                      "%s": "HH:mm-HH:mm"
                    }
                  ]
                }
                """;

        prompt.append(jsonInstructions);
        prompt.append(jsonStructure.formatted(
                KEY_COURSE_NAME,
                KEY_COURSE_DESC,
                KEY_RECOMMEND_TIME,
                KEY_PLACES,
                KEY_PLACE_ID,
                KEY_PLACE_NAME,
                KEY_PLACE_ADDRESS,
                KEY_PLACE_HOURS
        ));
    }

    private GptCourseInfoResponse parseGptResponseToDto(String gptResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(gptResponse);

            JsonNode choicesNode = rootNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                String content = choicesNode.get(0).path("message").path("content").asText();
                content = content.replaceAll("```json", "").replaceAll("```", "").trim();

                JsonNode contentNode = objectMapper.readTree(content);

                String courseName = contentNode.path(KEY_COURSE_NAME).asText(DEFAULT_COURSE_NAME);
                String courseDescription = contentNode.path(KEY_COURSE_DESC).asText(DEFAULT_COURSE_DESCRIPTION);
                String recommendTime = contentNode.path(KEY_RECOMMEND_TIME).asText(DEFAULT_TIME_RANGE);

                List<Long> placeIds = new ArrayList<>();
                JsonNode placesNode = contentNode.path(KEY_PLACES);
                if (placesNode.isArray()) {
                    for (JsonNode placeNode : placesNode) {
                        placeIds.add(placeNode.path(KEY_PLACE_ID).asLong());
                    }
                }

                List<GptPlaceInfoDto> placeInfoDtos = placeRepository.findPlacesWithCategoryAndReviewCount(placeIds);
                List<GptPlaceInfoResponse> placeInfos = placeInfoDtos.stream()
                        .map(GptPlaceInfoDto::toResponse)
                        .toList();

                return new GptCourseInfoResponse(courseName, courseDescription, recommendTime, placeInfos);
            } else {
                throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR);
            }
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR);
        }
    }

    private List<String> getPreferredCategories(Long memberId) {
        return memberCategoryRepository.findByMemberId(memberId).stream().map(mc -> mc.getCategory().getName()).toList();
    }
    private List<String> getUserStyles(Long memberId) {
        return memberStyleRepository.findByMemberId(memberId).stream().map(ms -> ms.getStyle().getName().name()).toList();
    }
    private List<String> getUserActiveTimes(Long memberId) {
        return memberActiveTimeRepository.findByMemberId(memberId).stream().map(mat -> mat.getActiveTime().toString()).toList();
    }
}
