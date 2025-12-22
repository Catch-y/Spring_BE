package umc.catchy.domain.place.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.place.dto.response.RecommendationContext;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceRecommendationService {

    public RecommendationContext analyzeVisitHistory(List<PlaceVisit> visits) {
        if (visits == null || visits.isEmpty()) {
            return new RecommendationContext(Collections.emptyList(), Collections.emptyMap());
        }

        Map<Long, List<LocalDateTime>> categoryVisits = groupVisitsByCategory(visits);

        return RecommendationContext.of(
                sortByVisitCount(categoryVisits),
                calculateAverageHours(categoryVisits)
        );
    }

    private Map<Long, List<LocalDateTime>> groupVisitsByCategory(List<PlaceVisit> visits) {
        Map<Long, List<LocalDateTime>> categoryVisits = new HashMap<>();

        for (PlaceVisit visit : visits) {
            Long categoryId = visit.getPlace().getCategory().getId();
            LocalDateTime visitTime = visit.getCreatedDate();

            categoryVisits
                    .computeIfAbsent(categoryId, k -> new ArrayList<>())
                    .add(visitTime);
        }

        return categoryVisits;
    }

    private List<Long> sortByVisitCount(Map<Long, List<LocalDateTime>> categoryVisits) {
        return categoryVisits.entrySet().stream()
                .sorted(Comparator.comparingInt((Entry<Long, List<LocalDateTime>> e) ->
                        e.getValue().size()).reversed())
                .map(Entry::getKey)
                .toList();
    }

    private Map<Long, Integer> calculateAverageHours(Map<Long, List<LocalDateTime>> categoryVisits) {
        return categoryVisits.entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        e -> (int) Math.round(e.getValue().stream()
                                .mapToDouble(LocalDateTime::getHour)
                                .average()
                                .orElse(0))
                ));
    }
}
