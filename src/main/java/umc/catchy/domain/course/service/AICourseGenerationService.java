package umc.catchy.domain.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.course.ai.GptPromptBuilder;
import umc.catchy.domain.course.ai.GptResponseParser;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
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
import umc.catchy.infra.openai.OpenAiClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AICourseGenerationService {

    private static final int MAX_PLACE_CANDIDATES = 100;
    private static final int GPT_MAX_TOKENS = 500;
    private static final double GPT_TEMPERATURE = 0.7;
    private static final int RANDOM_POOL_MULTIPLIER = 3;
    private static final String LOCATION_ALL = "전체";

    private final OpenAiClient openAiClient;
    private final GptPromptBuilder gptPromptBuilder;
    private final GptResponseParser gptResponseParser;
    private final MemberRepository memberRepository;
    private final MemberLocationRepository memberLocationRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberStyleRepository memberStyleRepository;
    private final MemberActiveTimeRepository memberActiveTimeRepository;
    private final CategoryRepository categoryRepository;
    private final PlaceRepository placeRepository;

    @Qualifier("gptExecutor")
    private final Executor gptExecutor;

    public CompletableFuture<GptCourseInfoResponse> generateCourseAutomatically(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<MemberLocation> memberLocations = memberLocationRepository.findAllByMemberId(memberId);
        if (memberLocations.isEmpty()) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO);
        }

        List<String> regionList = memberLocations.stream()
                .map(this::formatRegionString)
                .toList();

        List<String> preferredCategories = getPreferredCategories(memberId);
        List<Long> preferredCategoryIds = categoryRepository.findIdsByNames(preferredCategories);
        List<Place> places = getRecommendedPlacesForPrompt(regionList, preferredCategoryIds, memberId, MAX_PLACE_CANDIDATES);

        List<String> userStyles = getUserStyles(memberId);
        List<String> activeTimes = getUserActiveTimes(memberId);

        String prompt = gptPromptBuilder.buildCourseRecommendationPrompt(
                regionList,
                places,
                preferredCategories,
                userStyles,
                activeTimes
        );

        return callGpt(prompt)
                .thenApplyAsync(gptResponseParser::parse, gptExecutor)
                .exceptionally(e -> {
                    throw new CompletionException(
                            new GeneralException(ErrorStatus.GPT_API_CALL_FAILED)
                    );
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

    private CompletableFuture<String> callGpt(String prompt) {
        return openAiClient.chat(prompt, GPT_MAX_TOKENS, GPT_TEMPERATURE);
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

    private List<String> getPreferredCategories(Long memberId) {
        return memberCategoryRepository.findByMemberId(memberId).stream()
                .map(mc -> mc.getCategory().getName())
                .toList();
    }

    private List<String> getUserStyles(Long memberId) {
        return memberStyleRepository.findByMemberId(memberId).stream()
                .map(ms -> ms.getStyle().getName().name())
                .toList();
    }

    private List<String> getUserActiveTimes(Long memberId) {
        return memberActiveTimeRepository.findByMemberId(memberId).stream()
                .map(mat -> mat.getActiveTime().toString())
                .toList();
    }
}
