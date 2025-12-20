package umc.catchy.domain.place.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.place.dto.response.RecommendationContext;
import umc.catchy.support.fixture.CourseFixture;
import umc.catchy.support.fixture.MemberFixture;
import umc.catchy.support.fixture.PlaceFixture;
import umc.catchy.support.fixture.PlaceVisitFixture;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
class PlaceRecommendationServiceTest {

    @InjectMocks
    private PlaceRecommendationService recommendationService;

    @Test
    @DisplayName("방문 기록 분석 - 정상 케이스")
    void analyzeVisitHistory_success() {
        // given
        Member member = MemberFixture.createTestMember();
        Course course = CourseFixture.createTestCourse(member);

        Category cafeCategory = Category.builder()
                .id(1L)
                .name("카페")
                .bigCategory(BigCategory.CAFE)
                .build();

        Category restaurantCategory = Category.builder()
                .id(2L)
                .name("한식")
                .bigCategory(BigCategory.RESTAURANT)
                .build();

        Place cafePlace1 = PlaceFixture.createPlaceWithCategory(1L, cafeCategory);
        Place cafePlace2 = PlaceFixture.createPlaceWithCategory(2L, cafeCategory);
        Place restaurantPlace = PlaceFixture.createPlaceWithCategory(3L, restaurantCategory);

        List<PlaceVisit> visits = List.of(
                PlaceVisitFixture.createPlaceVisitWithTime(1L, member, cafePlace1, course, LocalDateTime.of(2025, 1, 1, 14, 0)),
                PlaceVisitFixture.createPlaceVisitWithTime(2L, member, cafePlace2, course, LocalDateTime.of(2025, 1, 2, 15, 0)),
                PlaceVisitFixture.createPlaceVisitWithTime(3L, member, restaurantPlace, course, LocalDateTime.of(2025, 1, 3, 18, 0))
        );

        // when
        RecommendationContext context = recommendationService.analyzeVisitHistory(visits);

        // then
        assertAll(
                () -> assertThat(context.sortedCategories()).isNotEmpty(),
                () -> assertThat(context.sortedCategories()).hasSize(2),
                () -> assertThat(context.sortedCategories().get(0)).isEqualTo(1L), // 카페가 2번으로 1순위
                () -> assertThat(context.averageHours()).containsKey(1L),
                () -> assertThat(context.averageHours().get(1L)).isEqualTo(15) // (14 + 15) / 2 = 14.5 → 15
        );
    }

    @Test
    @DisplayName("방문 기록 분석 - 빈 리스트")
    void analyzeVisitHistory_empty_list() {
        // given
        List<PlaceVisit> emptyVisits = Collections.emptyList();

        // when
        RecommendationContext context = recommendationService.analyzeVisitHistory(emptyVisits);

        // then
        assertAll(
                () -> assertThat(context.sortedCategories()).isEmpty(),
                () -> assertThat(context.averageHours()).isEmpty()
        );
    }

    @Test
    @DisplayName("방문 기록 분석 - null")
    void analyzeVisitHistory_null() {
        // when
        RecommendationContext context = recommendationService.analyzeVisitHistory(null);

        // then
        assertAll(
                () -> assertThat(context.sortedCategories()).isEmpty(),
                () -> assertThat(context.averageHours()).isEmpty()
        );
    }

    @Test
    @DisplayName("카테고리별 방문 횟수 정렬 검증")
    void sortByVisitCount() {
        // given
        Member member = MemberFixture.createTestMember();
        Course course = CourseFixture.createTestCourse(member);

        Category cafeCategory = Category.builder()
                .id(1L)
                .name("카페")
                .bigCategory(BigCategory.CAFE)
                .build();

        Category restaurantCategory = Category.builder()
                .id(2L)
                .name("한식")
                .bigCategory(BigCategory.RESTAURANT)
                .build();

        Category cultureCategory = Category.builder()
                .id(3L)
                .name("미술관")
                .bigCategory(BigCategory.CULTURELIFE)
                .build();

        Place cafePlace1 = PlaceFixture.createPlaceWithCategory(1L, cafeCategory);
        Place cafePlace2 = PlaceFixture.createPlaceWithCategory(2L, cafeCategory);
        Place cafePlace3 = PlaceFixture.createPlaceWithCategory(3L, cafeCategory);
        Place restaurantPlace1 = PlaceFixture.createPlaceWithCategory(4L, restaurantCategory);
        Place restaurantPlace2 = PlaceFixture.createPlaceWithCategory(5L, restaurantCategory);
        Place culturePlace = PlaceFixture.createPlaceWithCategory(6L, cultureCategory);

        List<PlaceVisit> visits = List.of(
                PlaceVisitFixture.createPlaceVisitWithTime(1L, member, cafePlace1, course, LocalDateTime.now()),
                PlaceVisitFixture.createPlaceVisitWithTime(2L, member, cafePlace2, course, LocalDateTime.now()),
                PlaceVisitFixture.createPlaceVisitWithTime(3L, member, cafePlace3, course, LocalDateTime.now()),
                PlaceVisitFixture.createPlaceVisitWithTime(4L, member, restaurantPlace1, course, LocalDateTime.now()),
                PlaceVisitFixture.createPlaceVisitWithTime(5L, member, restaurantPlace2, course, LocalDateTime.now()),
                PlaceVisitFixture.createPlaceVisitWithTime(6L, member, culturePlace, course, LocalDateTime.now())
        );

        // when
        RecommendationContext context = recommendationService.analyzeVisitHistory(visits);

        // then
        assertAll(
                () -> assertThat(context.sortedCategories()).hasSize(3),
                () -> assertThat(context.sortedCategories().get(0)).isEqualTo(1L), // 카페 3회
                () -> assertThat(context.sortedCategories().get(1)).isEqualTo(2L), // 음식점 2회
                () -> assertThat(context.sortedCategories().get(2)).isEqualTo(3L)  // 문화 1회
        );
    }

    @Test
    @DisplayName("평균 시간 계산 검증")
    void calculateAverageHours() {
        // given
        Member member = MemberFixture.createTestMember();
        Course course = CourseFixture.createTestCourse(member);

        Category cafeCategory = Category.builder()
                .id(1L)
                .name("카페")
                .bigCategory(BigCategory.CAFE)
                .build();

        Place cafePlace1 = PlaceFixture.createPlaceWithCategory(1L, cafeCategory);
        Place cafePlace2 = PlaceFixture.createPlaceWithCategory(2L, cafeCategory);
        Place cafePlace3 = PlaceFixture.createPlaceWithCategory(3L, cafeCategory);

        List<PlaceVisit> visits = List.of(
                PlaceVisitFixture.createPlaceVisitWithTime(1L, member, cafePlace1, course, LocalDateTime.of(2025, 1, 1, 9, 0)),
                PlaceVisitFixture.createPlaceVisitWithTime(2L, member, cafePlace2, course, LocalDateTime.of(2025, 1, 2, 12, 0)),
                PlaceVisitFixture.createPlaceVisitWithTime(3L, member, cafePlace3, course, LocalDateTime.of(2025, 1, 3, 15, 0))
        );

        // when
        RecommendationContext context = recommendationService.analyzeVisitHistory(visits);

        // then
        assertThat(context.averageHours().get(1L)).isEqualTo(12);
    }
}
