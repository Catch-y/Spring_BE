package umc.catchy.domain.course.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

public class CourseOSRMRequest {

    @Getter
    @Setter
    @Builder
    public static class routeInfo{
        @NotNull(message = "Longitude must not be null")
        @DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be >= -180.0")
        @DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be <= 180.0")
        Double longitude;

        @NotNull(message = "Latitude must not be null")
        @DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be >= -90.0")
        @DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be <= 90.0")
        Double latitude;
    }

    @Getter
    @Setter
    @Builder
    public static class courseInfo{
        @NotNull(message = "Start location is required")
        routeInfo start;

        @Size(max = 6, message = "Routes can contain up to 8 waypoints")
        List<routeInfo> routes;

        @NotNull(message = "End location is required")
        routeInfo end;
    }
}
