package umc.catchy.domain.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.course.dto.response.GptPlaceInfoDto;
import umc.catchy.domain.course.dto.response.GptPlaceInfoResponse;
import umc.catchy.domain.course.util.LocationUtils;
import umc.catchy.domain.mapping.memberActivetime.dao.MemberActiveTimeRepository;
import umc.catchy.domain.mapping.memberCategory.dao.MemberCategoryRepository;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.memberLocation.dao.MemberLocationRepository;
import umc.catchy.domain.mapping.memberLocation.domain.MemberLocation;
import umc.catchy.domain.mapping.memberStyle.dao.MemberStyleRepository;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.location.domain.Location;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AICourseGenerationService {

    private final GPTCourseService gptCourseService;
    private final MemberRepository memberRepository;
    private final MemberLocationRepository memberLocationRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberStyleRepository memberStyleRepository;
    private final MemberActiveTimeRepository memberActiveTimeRepository;
    private final CategoryRepository categoryRepository;
    private final PlaceRepository placeRepository;
    private final CourseRepository courseRepository;
    private final PlaceCourseRepository placeCourseRepository;
    private final MemberCourseRepository memberCourseRepository;

    // 오버로딩 메서드 (API 호출 시 사용)
    public CompletableFuture<GptCourseInfoResponse> generateCourseAutomatically(boolean isForHome) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return generateCourseAutomatically(memberId, isForHome);
    }

    public CompletableFuture<GptCourseInfoResponse> generateCourseAutomatically(Long memberId, boolean isForHome) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (!isForHome) {
            member.increaseGptCount();
            memberRepository.save(member);
        }

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
                .collect(Collectors.toList());

        List<Long> preferredCategoryIds = categoryRepository.findIdsByNames(preferredCategories);

        // 프롬프트 구성을 위한 장소 추천 (내부 로직 사용)
        List<Place> places = getRecommendedPlacesForPrompt(regionList, preferredCategoryIds, memberId, 100);

        String gptPrompt = buildGptPrompt(regionList, places, preferredCategories, userStyles, activeTimes);

        return CompletableFuture.supplyAsync(() -> gptCourseService.callOpenAiApiAsync(gptPrompt).join())
                .thenCompose(gptResponse -> {
                    GptCourseInfoResponse response = parseGptResponseToDto(gptResponse);

                    if (isForHome) {
                        // 홈 화면용이면 바로 저장
                        return saveCourseAndPlaces(response, member, true)
                                .thenApply(courseId -> response);
                    } else {
                        // 유저 요청이면 저장 안 하고 데이터만 반환
                        return CompletableFuture.completedFuture(response);
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    throw new GeneralException(ErrorStatus.GPT_API_CALL_FAILED);
                });
    }

    public CompletableFuture<Void> generateMultipleAICourses(Long memberId, int count, boolean isForHome) {
        List<CompletableFuture<GptCourseInfoResponse>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            futures.add(generateCourseAutomatically(memberId, isForHome));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // AI 코스 저장 로직 (이 서비스가 전담)
    @Async
    @Transactional
    public CompletableFuture<Long> saveCourseAndPlaces(GptCourseInfoResponse parsedResponse, Member member, boolean isForHome) {
        Pair<LocalTime, LocalTime> recommendTime = parseRecommendTime(parsedResponse.recommendTime());

        Course course = Course.builder()
                .courseName(parsedResponse.courseName())
                .courseDescription(parsedResponse.courseDescription())
                .courseType(CourseType.AI)
                .recommendTimeStart(recommendTime.getLeft())
                .recommendTimeEnd(recommendTime.getRight())
                .courseImage(null) // 이미지는 나중에 처리
                .participantsNumber(0L)
                .member(member)
                .build();

        Course savedCourse = courseRepository.saveAndFlush(course);

        int order = 1;
        double totalRating = 0.0;
        int placeCount = 0;

        for (GptPlaceInfoResponse placeInfo : parsedResponse.placeInfos()) {
            Place place = placeRepository.findById(placeInfo.placeId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

            PlaceCourse placeCourse = PlaceCourse.builder()
                    .course(savedCourse)
                    .place(place)
                    .placeOrder(order++)
                    .build();

            placeCourseRepository.save(placeCourse);

            if (place.getRating() != null && place.getRating() > 0) {
                totalRating += place.getRating();
                placeCount++;
            }
        }

        double courseRating = placeCount > 0 ? totalRating / placeCount : 0.0;
        savedCourse.updateRating(courseRating);
        courseRepository.saveAndFlush(savedCourse);

        if (!isForHome) {
            MemberCourse memberCourse = MemberCourse.builder()
                    .course(savedCourse)
                    .member(member)
                    .build();
            memberCourseRepository.save(memberCourse);
        }

        return CompletableFuture.completedFuture(savedCourse.getId());
    }

    // 내부 Helper 메서드들
    private List<Place> getRecommendedPlacesForPrompt(List<String> regionList, List<Long> preferredCategoryIds, Long memberId, int maxPlaces) {
        List<String> upperRegions = regionList.stream()
                .map(LocationUtils::extractUpperLocation)
                .map(LocationUtils::normalizeLocation)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> lowerRegions = regionList.stream()
                .map(LocationUtils::extractLowerLocation)
                .map(LocationUtils::normalizeLocation)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<Place> recommendedPlaces = placeRepository.findRecommendedPlaces(preferredCategoryIds, upperRegions, lowerRegions, memberId, maxPlaces);

        int subsetSize = Math.min(3 * maxPlaces, recommendedPlaces.size());
        List<Place> topPlaces = recommendedPlaces.subList(0, subsetSize);
        Collections.shuffle(topPlaces);

        return topPlaces.stream().limit(maxPlaces).collect(Collectors.toList());
    }

    private Pair<LocalTime, LocalTime> parseRecommendTime(String recommendTime) {
        try {
            String[] times = recommendTime.split("~");
            LocalTime startTime = LocalTime.parse(times[0].trim());
            LocalTime endTime = times[1].equals("24:00") ? LocalTime.MIDNIGHT : LocalTime.parse(times[1].trim());
            return Pair.of(startTime, endTime);
        } catch (DateTimeParseException e) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO);
        }
    }

    private String buildGptPrompt(
            List<String> regionList,
            List<Place> places,
            List<String> preferredCategories,
            List<String> userStyles,
            List<String> activeTimes
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Create a unique and creative itinerary for the following regions: ");
        prompt.append(String.join(", ", regionList)).append(".\n");
        prompt.append("Randomly select **2 to 5** unique places from the list below to create a diverse and interesting itinerary.\n");
        prompt.append("Do not include all places in the itinerary.\n");

        prompt.append("The user's preferred categories are: ");
        prompt.append(String.join(", ", preferredCategories));
        prompt.append(".\n");

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
                    place.getId(),
                    place.getPlaceName(),
                    place.getRoadAddress(),
                    place.getActiveTime(),
                    place.getCategory().getName(),
                    place.getPlaceDescription()
            ));
        }

        prompt.append("\nThe course name and description must be written in Korean.\n");
        prompt.append("The course description should be concise, no more than 80 characters.\n");
        prompt.append("Please generate a course name and description that fits the selected places and reflects the user's preferred styles.\n");
        prompt.append("Each course must contain at least 2 and at most 5 places.\n");
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
        prompt.append("Return only this JSON structure, with no additional text.");
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

                List<GptPlaceInfoDto> placeInfoDtos =
                        placeRepository.findPlacesWithCategoryAndReviewCount(placeIds);

                List<GptPlaceInfoResponse> placeInfos = placeInfoDtos.stream()
                        .map(GptPlaceInfoDto::toResponse)
                        .toList();

                return new GptCourseInfoResponse(
                        courseName,
                        courseDescription,
                        recommendTime,
                        placeInfos
                );
            } else {
                throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR, "GPT 응답의 choices 배열이 비어 있습니다.");
            }
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR, "JSON 파싱 중 오류 발생: " + e.getMessage());
        }
    }

    // 유저 정보 조회 Helper (Prompt 구성용)
    private List<String> getPreferredCategories(Long memberId) {
        return memberCategoryRepository.findByMemberId(memberId).stream()
                .map(mc -> mc.getCategory().getName()).toList();
    }
    private List<String> getUserStyles(Long memberId) {
        return memberStyleRepository.findByMemberId(memberId).stream()
                .map(ms -> ms.getStyle().getName().name()).collect(Collectors.toList());
    }
    private List<String> getUserActiveTimes(Long memberId) {
        return memberActiveTimeRepository.findByMemberId(memberId).stream()
                .map(mat -> mat.getActiveTime().getDayOfWeek() + " " + mat.getActiveTime().getStartTime() + "~" + mat.getActiveTime().getEndTime())
                .collect(Collectors.toList());
    }
}
