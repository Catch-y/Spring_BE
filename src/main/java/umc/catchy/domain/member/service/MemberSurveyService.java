package umc.catchy.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.catchy.domain.activetime.dao.ActiveTimeRepository;
import umc.catchy.domain.activetime.domain.ActiveTime;
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
public class MemberSurveyService {
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
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        List<Category> categories = categoryRepository.findAllByNameIn(request.categories());
        List<MemberCategory> collect = categories.stream().map(category -> MemberCategory.createMemberCategory(currentMember, category)).collect(Collectors.toList());
        memberCategoryRepository.saveAll(collect);

        List<Long> memberCategoryIds = collect.stream()
                .map(MemberCategory::getId)
                .toList();

        return new MemberCategoryCreatedResponse(memberCategoryIds);
    }

    public StyleAndActiveTimeSurveyCreatedResponse createStyleAndActiveTimeSurvey(StyleAndActiveTimeSurveyRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        List<Style> styleList = styleRepository.findAllByNameIn(request.styleNames());

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        List<ActiveTime> activeTimeList = request.activeTimes().stream().map(activeTime ->
                activeTimeRepository.findByDayOfWeekAndStartTimeAndEndTime(activeTime.dayOfWeek(),
                                LocalTime.parse(activeTime.startTime(),dateTimeFormatter),
                                LocalTime.parse(activeTime.endTime(), dateTimeFormatter))
                        .orElseGet(() -> activeTimeRepository.save(ActiveTime.createActiveTime(activeTime.dayOfWeek(),
                                LocalTime.parse(activeTime.startTime(),dateTimeFormatter),
                                LocalTime.parse(activeTime.endTime(), dateTimeFormatter)))
                        )).toList();

        List<MemberStyle> memberStyleList = styleList.stream().map(style -> MemberStyle.createMemberStyle(currentMember, style)).collect(Collectors.toList());
        List<Long> memberStyleIds = saveMemberStyleAndReturnIds(memberStyleList);
        List<MemberActiveTime> memberActiveTimeList = activeTimeList.stream().map(activeTime -> MemberActiveTime.createMemberActiveTime(currentMember, activeTime)).collect(Collectors.toList());
        List<Long> memberActiveTimeIds = saveMemberActiveTimeAndReturnIds(memberActiveTimeList);

        return new StyleAndActiveTimeSurveyCreatedResponse(memberStyleIds, memberActiveTimeIds);
    }

    private List<Long> saveMemberStyleAndReturnIds(List<MemberStyle> memberStyleList) {
        List<MemberStyle> savedEntities = memberStyleRepository.saveAll(memberStyleList);

        return savedEntities.stream()
                .map(MemberStyle::getId) // 저장된 엔티티의 ID 값 추출
                .collect(Collectors.toList());
    }

    private List<Long> saveMemberActiveTimeAndReturnIds(List<MemberActiveTime> memberActiveTimeList) {
        List<MemberActiveTime> savedEntities = memberActiveTimeRepository.saveAll(memberActiveTimeList);

        return savedEntities.stream()
                .map(MemberActiveTime::getId)
                .collect(Collectors.toList());
    }

    public MemberLocationCreatedResponse createMemberLocation(List<LocationSurveyRequest> request) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        List<Location> locationList = request.stream().map(r -> locationRepository.findByUpperLocationAndLowerLocation(r.upperLocation(), r.lowerLocation())
                .orElseGet(() -> locationRepository.save(Location.createLocation(r.upperLocation(), r.lowerLocation())))
        ).toList();

        List<MemberLocation> memberLocationList = locationList.stream().map(location -> MemberLocation.createMemberLocation(currentMember, location)).collect(Collectors.toList());
        memberLocationRepository.saveAll(memberLocationList);

        List<Long> memberLocationIds = memberLocationList.stream()
                .map(MemberLocation::getId)
                .collect(Collectors.toList());

        return new MemberLocationCreatedResponse(memberLocationIds);
    }
}
