package umc.catchy.domain.mapping.placeVisit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceVisitedDateResponse {
    private List<LocalDate> visitedDate;
}
