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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AICourseGenerationService {

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
                .map(memberLocation -> {
                    Location location = memberLocation.getLocation();
                    String upper = location.getUpperLocation();
                    String lower = location.getLowerLocation();
                    upper = (upper != null && !upper.equals("전체")) ? upper : null;
                    lower = (lower != null && !lower.equals("전체")) ? lower : null;
                    return upper + (lower != null ? " " + lower : " 전체");
                })
                .toList();

        List<Long> preferredCategoryIds = categoryRepository.findIdsByNames(preferredCategories);
        List<Place> places = getRecommendedPlacesForPrompt(regionList, preferredCategoryIds, memberId, 100);

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
                "max_tokens", 1000,
                "temperature", 0.7
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
        int subsetSize = Math.min(3 * maxPlaces, recommendedPlaces.size());
        List<Place> topPlaces = recommendedPlaces.subList(0, subsetSize);
        Collections.shuffle(topPlaces);

        return topPlaces.stream().limit(maxPlaces).toList();
    }

    private String buildGptPrompt(List<String> regionList, List<Place> places, List<String> preferredCategories, List<String> userStyles, List<String> activeTimes) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Create a unique and creative itinerary for the following regions: ");
        prompt.append(String.join(", ", regionList)).append(".\n");
        prompt.append("Randomly select **2 to 5** unique places from the list below to create a diverse and interesting itinerary.\n");
        prompt.append("Do not include all places in the itinerary.\n");

        prompt.append("The user's preferred categories are: ");
        prompt.append(String.join(", ", preferredCategories)).append(".\n");

        if (!userStyles.isEmpty()) {
            prompt.append("The user prefers the following styles: ");
            prompt.append(String.join(", ", userStyles)).append(".\n");
        }

        if (!activeTimes.isEmpty()) {
            prompt.append("The user's preferred active times are: ");
            prompt.append(String.join(", ", activeTimes)).append(".\n");
        }

        prompt.append("Here are the places to choose from:\n");
        for (Place place : places) {
            prompt.append(String.format(
                    "- Place ID: %d, Name: %s, Road Address: %s, Operating Hours: %s, Category: %s, Description: %s\n",
                    place.getId(), place.getPlaceName(), place.getRoadAddress(), place.getActiveTime(), place.getCategory().getName(), place.getPlaceDescription()
            ));
        }

        prompt.append("\nThe course name and description must be written in Korean.\n");
        prompt.append("The course description should be concise, no more than 80 characters.\n");
        prompt.append("The response should include a course name, course description, recommended visit time.\n");
        prompt.append("Please return only the JSON structure below without any additional text, comments, or markdown formatting (e.g., no ```json). Return only the raw JSON structure:\n");
        prompt.append("{\n");
        prompt.append("  \"courseName\": \"string (in Korean)\",\n");
        prompt.append("  \"courseDescription\": \"string (in Korean)\",\n");
        prompt.append("  \"recommendTime\": \"HH:mm~HH:mm\",\n");
        prompt.append("  \"places\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"placeId\": \"numeric\",\n");
        prompt.append("      \"name\": \"string\",\n");
        prompt.append("      \"roadAddress\": \"string\",\n");
        prompt.append("      \"operatingHours\": \"HH:mm-HH:mm\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        return prompt.toString();
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

                String courseName = contentNode.path("courseName").asText("AI 추천 코스");
                String courseDescription = contentNode.path("courseDescription").asText("AI가 추천한 여행 코스입니다.");
                String recommendTime = contentNode.path("recommendTime").asText("09:00~21:00");

                List<Long> placeIds = new ArrayList<>();
                JsonNode placesNode = contentNode.path("places");
                if (placesNode.isArray()) {
                    for (JsonNode placeNode : placesNode) {
                        placeIds.add(placeNode.path("placeId").asLong());
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
