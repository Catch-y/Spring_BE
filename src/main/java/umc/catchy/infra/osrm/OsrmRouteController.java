package umc.catchy.infra.osrm;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.catchy.domain.course.dto.request.CourseOSRMRequest;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.SuccessStatus;

@Tag(name = "Route", description = "OSRM-route HTTP API")
@RestController
@RequestMapping("/route")
@RequiredArgsConstructor
public class OsrmRouteController {

    private final OsrmService osrmService;

    @Operation(summary = "osrm-routed http 호출 API", description = "OSRM 서버의 route 서비스를 사용하기 위한 API입니다.")
    @PostMapping("/")
    public ResponseEntity<BaseResponse<OsrmResponse>> getRouteOfCourse(
            @Valid @RequestBody CourseOSRMRequest.courseInfo request
    ){
        OsrmResponse response = osrmService.getRoute(request);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }
}
