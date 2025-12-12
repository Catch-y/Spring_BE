package umc.catchy.domain.member.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.catchy.domain.category.dto.request.CategorySurveyRequest;
import umc.catchy.domain.location.dto.request.LocationSurveyRequest;
import umc.catchy.domain.mapping.memberCategory.dto.response.MemberCategoryCreatedResponse;
import umc.catchy.domain.mapping.memberLocation.dto.response.MemberLocationCreatedResponse;
import umc.catchy.domain.member.dto.request.StyleAndActiveTimeSurveyRequest;
import umc.catchy.domain.member.dto.response.StyleAndActiveTimeSurveyCreatedResponse;
import umc.catchy.domain.member.service.MemberSurveyService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

import java.util.List;

@Tag(name = "Survey", description = "사용자 취향 설문 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/member/survey")
public class MemberSurveyController {

    private final MemberSurveyService memberSurveyService;

    @PostMapping("/category")
    @Operation(summary = "사용자 취향설문 카테고리 저장 API ", description = "사용자 취향설문 1,2단계를 저장")
    public BaseResponse<MemberCategoryCreatedResponse> createMemberCategory(
            @RequestBody CategorySurveyRequest request) {
        MemberCategoryCreatedResponse response = memberSurveyService.createMemberCategory(request);
        return BaseResponse.onSuccess(SuccessStatus._CREATED, response);
    }

    @PostMapping("/styletime")
    @Operation(summary = "사용자 취향설문 참여스타일 및 활동요일,시간 저장 API ", description = "사용자 취향설문 3,4단계를 저장")
    public BaseResponse<StyleAndActiveTimeSurveyCreatedResponse> createMemberStyleTime(
            @RequestBody StyleAndActiveTimeSurveyRequest request) {
        StyleAndActiveTimeSurveyCreatedResponse response = memberSurveyService.createStyleAndActiveTimeSurvey(request);
        return BaseResponse.onSuccess(SuccessStatus._CREATED, response);
    }

    @PostMapping("/location")
    @Operation(summary = "사용자 취향설문 선호지역 저장 API", description = "사용자 취향설문 5단계를 저장")
    public BaseResponse<MemberLocationCreatedResponse> createMemberLocation(@RequestBody List<LocationSurveyRequest> request) {
        MemberLocationCreatedResponse response = memberSurveyService.createMemberLocation(request);
        return BaseResponse.onSuccess(SuccessStatus._CREATED, response);
    }
}
