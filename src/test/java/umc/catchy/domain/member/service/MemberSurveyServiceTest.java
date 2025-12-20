package umc.catchy.domain.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.activetime.dao.ActiveTimeRepository;
import umc.catchy.domain.activetime.domain.ActiveTime;
import umc.catchy.domain.activetime.dto.ActiveTimeRequest;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.category.dto.request.CategorySurveyRequest;
import umc.catchy.domain.location.dao.LocationRepository;
import umc.catchy.domain.location.domain.Location;
import umc.catchy.domain.location.dto.request.LocationSurveyRequest;
import umc.catchy.domain.mapping.memberActivetime.dao.MemberActiveTimeRepository;
import umc.catchy.domain.mapping.memberActivetime.domain.MemberActiveTime;
import umc.catchy.domain.mapping.memberCategory.dao.MemberCategoryRepository;
import umc.catchy.domain.mapping.memberCategory.domain.MemberCategory;
import umc.catchy.domain.mapping.memberCategory.dto.response.MemberCategoryCreatedResponse;
import umc.catchy.domain.mapping.memberLocation.dao.MemberLocationRepository;
import umc.catchy.domain.mapping.memberLocation.domain.MemberLocation;
import umc.catchy.domain.mapping.memberLocation.dto.response.MemberLocationCreatedResponse;
import umc.catchy.domain.mapping.memberStyle.dao.MemberStyleRepository;
import umc.catchy.domain.mapping.memberStyle.domain.MemberStyle;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.FcmInfo;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.StyleAndActiveTimeSurveyRequest;
import umc.catchy.domain.member.dto.response.StyleAndActiveTimeSurveyCreatedResponse;
import umc.catchy.domain.style.dao.StyleRepository;
import umc.catchy.domain.style.domain.Style;
import umc.catchy.domain.style.domain.StyleName;
import umc.catchy.global.util.SecurityUtil;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberSurveyServiceTest {

    @InjectMocks
    private MemberSurveyService memberSurveyService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MemberCategoryRepository memberCategoryRepository;

    @Mock
    private StyleRepository styleRepository;

    @Mock
    private ActiveTimeRepository activeTimeRepository;

    @Mock
    private MemberActiveTimeRepository memberActiveTimeRepository;

    @Mock
    private MemberStyleRepository memberStyleRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private MemberLocationRepository memberLocationRepository;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .providerId("kakao_12345")
                .email("test@test.com")
                .nickname("테스트닉네임")
                .socialType(SocialType.KAKAO)
                .fcmInfo(FcmInfo.createFcmInfo())
                .build();
    }

    @Test
    @DisplayName("카테고리 설문 저장 성공")
    void createMemberCategory_success() {
        CategorySurveyRequest request = new CategorySurveyRequest(List.of("음식점", "카페"));

        Category category1 = mock(Category.class);
        Category category2 = mock(Category.class);

        MemberCategory memberCategory1 = mock(MemberCategory.class);
        when(memberCategory1.getId()).thenReturn(10L);

        MemberCategory memberCategory2 = mock(MemberCategory.class);
        when(memberCategory2.getId()).thenReturn(11L);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(categoryRepository.findAllByNameIn(List.of("음식점", "카페")))
                    .thenReturn(List.of(category1, category2));
            when(memberCategoryRepository.saveAll(anyList()))
                    .thenReturn(List.of(memberCategory1, memberCategory2));

            MemberCategoryCreatedResponse response = memberSurveyService.createMemberCategory(request);

            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.memberCategoryIds()).hasSize(2),
                    () -> assertThat(response.memberCategoryIds()).containsExactly(10L, 11L)
            );

            verify(memberRepository, times(1)).findById(1L);
            verify(categoryRepository, times(1)).findAllByNameIn(List.of("음식점", "카페"));
            verify(memberCategoryRepository, times(1)).saveAll(anyList());
        }
    }

    @Test
    @DisplayName("스타일/시간 설문 저장 성공")
    void createStyleAndActiveTimeSurvey_success() {
        List<StyleName> styleNames = List.of(StyleName.ALONE, StyleName.FRIENDS);
        List<ActiveTimeRequest> activeTimes = List.of(
                new ActiveTimeRequest(DayOfWeek.MONDAY, "10:00", "12:00")
        );

        StyleAndActiveTimeSurveyRequest request = new StyleAndActiveTimeSurveyRequest(styleNames, activeTimes);

        Style style1 = mock(Style.class);
        Style style2 = mock(Style.class);

        ActiveTime activeTime = ActiveTime.builder()
                .id(100L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        MemberStyle memberStyle1 = mock(MemberStyle.class);
        when(memberStyle1.getId()).thenReturn(20L);

        MemberStyle memberStyle2 = mock(MemberStyle.class);
        when(memberStyle2.getId()).thenReturn(21L);

        MemberActiveTime memberActiveTime = mock(MemberActiveTime.class);
        when(memberActiveTime.getId()).thenReturn(30L);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(styleRepository.findAllByNameIn(styleNames)).thenReturn(List.of(style1, style2));
            when(activeTimeRepository.findByDayOfWeekAndStartTimeAndEndTime(
                    DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)
            )).thenReturn(Optional.of(activeTime));

            when(memberStyleRepository.saveAll(anyList()))
                    .thenReturn(List.of(memberStyle1, memberStyle2));
            when(memberActiveTimeRepository.saveAll(anyList()))
                    .thenReturn(List.of(memberActiveTime));

            StyleAndActiveTimeSurveyCreatedResponse response =
                    memberSurveyService.createStyleAndActiveTimeSurvey(request);

            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.memberStyleSurveyId()).hasSize(2),
                    () -> assertThat(response.memberStyleSurveyId()).containsExactly(20L, 21L),
                    () -> assertThat(response.activeTimeSurveyId()).hasSize(1),
                    () -> assertThat(response.activeTimeSurveyId()).containsExactly(30L)
            );

            verify(memberRepository, times(1)).findById(1L);
            verify(styleRepository, times(1)).findAllByNameIn(styleNames);
            verify(memberStyleRepository, times(1)).saveAll(anyList());
            verify(memberActiveTimeRepository, times(1)).saveAll(anyList());
        }
    }

    @Test
    @DisplayName("선호지역 설문 저장 성공")
    void createMemberLocation_success() {
        List<LocationSurveyRequest> request = List.of(
                new LocationSurveyRequest("서울", "강남구"),
                new LocationSurveyRequest("서울", "마포구")
        );

        Location location1 = Location.builder()
                .id(1L)
                .upperLocation("서울")
                .lowerLocation("강남구")
                .build();

        Location location2 = Location.builder()
                .id(2L)
                .upperLocation("서울")
                .lowerLocation("마포구")
                .build();

        MemberLocation memberLocation1 = mock(MemberLocation.class);
        when(memberLocation1.getId()).thenReturn(50L);

        MemberLocation memberLocation2 = mock(MemberLocation.class);
        when(memberLocation2.getId()).thenReturn(51L);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(locationRepository.findByUpperLocationAndLowerLocation("서울", "강남구"))
                    .thenReturn(Optional.of(location1));
            when(locationRepository.findByUpperLocationAndLowerLocation("서울", "마포구"))
                    .thenReturn(Optional.of(location2));
            when(memberLocationRepository.saveAll(anyList()))
                    .thenReturn(List.of(memberLocation1, memberLocation2));

            MemberLocationCreatedResponse response = memberSurveyService.createMemberLocation(request);

            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.memberLocationId()).hasSize(2),
                    () -> assertThat(response.memberLocationId()).containsExactly(50L, 51L)
            );

            verify(memberRepository, times(1)).findById(1L);
            verify(memberLocationRepository, times(1)).saveAll(anyList());
        }
    }
}
