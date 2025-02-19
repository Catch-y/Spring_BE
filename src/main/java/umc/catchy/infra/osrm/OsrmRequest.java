package umc.catchy.infra.osrm;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class OsrmRequest {

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

        @Size(max = 3, message = "Routes can contain up to 5 waypoints")
        List<routeInfo> routes;

        @NotNull(message = "End location is required")
        routeInfo end;
    }
}
