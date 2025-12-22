package umc.catchy.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.activetime.dao.ActiveTimeRepository;
import umc.catchy.domain.activetime.domain.ActiveTime;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.category.dto.request.CategorySurveyRequest;
import umc.catchy.domain.course.util.LocationUtils;
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
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.dto.request.StyleAndActiveTimeSurveyRequest;
import umc.catchy.domain.member.dto.response.StyleAndActiveTimeSurveyCreatedResponse;
import umc.catchy.domain.style.dao.StyleRepository;
import umc.catchy.domain.style.domain.Style;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberSurveyService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final StyleRepository styleRepository;
    private final ActiveTimeRepository activeTimeRepository;
    private final MemberActiveTimeRepository memberActiveTimeRepository;
    private final MemberStyleRepository memberStyleRepository;
    private final LocationRepository locationRepository;
    private final MemberLocationRepository memberLocationRepository;

    public MemberCategoryCreatedResponse createMemberCategory(CategorySurveyRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = findMemberById(memberId);
        List<Category> categories = categoryRepository.findAllByNameIn(request.categories());

        List<MemberCategory> memberCategories = categories.stream()
                .map(category -> MemberCategory.createMemberCategory(currentMember, category))
                .collect(Collectors.toList());

        List<MemberCategory> savedMemberCategories = memberCategoryRepository.saveAll(memberCategories);

        List<Long> memberCategoryIds = savedMemberCategories.stream()
                .map(MemberCategory::getId)
                .toList();

        return MemberCategoryCreatedResponse.of(memberCategoryIds);
    }

    public StyleAndActiveTimeSurveyCreatedResponse createStyleAndActiveTimeSurvey(StyleAndActiveTimeSurveyRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = findMemberById(memberId);
        List<Style> styleList = styleRepository.findAllByNameIn(request.styleNames());

        List<ActiveTime> activeTimeList = request.activeTimes().stream()
                .map(activeTime -> findOrCreateActiveTime(
                        activeTime.dayOfWeek(),
                        LocalTime.parse(activeTime.startTime(), TIME_FORMATTER),
                        LocalTime.parse(activeTime.endTime(), TIME_FORMATTER)))
                .toList();

        List<MemberStyle> memberStyleList = styleList.stream()
                .map(style -> MemberStyle.createMemberStyle(currentMember, style))
                .collect(Collectors.toList());
        List<Long> memberStyleIds = saveAndExtractIds(memberStyleRepository.saveAll(memberStyleList), MemberStyle::getId);

        List<MemberActiveTime> memberActiveTimeList = activeTimeList.stream()
                .map(activeTime -> MemberActiveTime.createMemberActiveTime(currentMember, activeTime))
                .collect(Collectors.toList());
        List<Long> memberActiveTimeIds = saveAndExtractIds(memberActiveTimeRepository.saveAll(memberActiveTimeList), MemberActiveTime::getId);

        return StyleAndActiveTimeSurveyCreatedResponse.of(memberStyleIds, memberActiveTimeIds);
    }

    public MemberLocationCreatedResponse createMemberLocation(List<LocationSurveyRequest> request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = findMemberById(memberId);

        List<Location> locationList = request.stream()
                .map(r -> findOrCreateLocation(r.upperLocation(), r.lowerLocation()))
                .toList();

        List<MemberLocation> memberLocationList = locationList.stream()
                .map(location -> MemberLocation.createMemberLocation(currentMember, location))
                .collect(Collectors.toList());

        List<MemberLocation> savedMemberLocations = memberLocationRepository.saveAll(memberLocationList);

        List<Long> memberLocationIds = savedMemberLocations.stream()
                .map(MemberLocation::getId)
                .collect(Collectors.toList());

        return MemberLocationCreatedResponse.of(memberLocationIds);
    }

    private <T> List<Long> saveAndExtractIds(List<T> savedEntities, java.util.function.Function<T, Long> idExtractor) {
        return savedEntities.stream()
                .map(idExtractor)
                .toList();
    }

    private ActiveTime findOrCreateActiveTime(java.time.DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        return activeTimeRepository.findByDayOfWeekAndStartTimeAndEndTime(dayOfWeek, startTime, endTime)
                .orElseGet(() -> activeTimeRepository.save(ActiveTime.createActiveTime(dayOfWeek, startTime, endTime)));
    }

    private Location findOrCreateLocation(String upperLocation, String lowerLocation) {
        String normalizedUpper = LocationUtils.normalizeLocation(upperLocation);

        return locationRepository.findByUpperLocationAndLowerLocation(normalizedUpper, lowerLocation)
                .orElseGet(() -> {
                    return locationRepository.save(Location.createLocation(normalizedUpper, lowerLocation));
                });
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
