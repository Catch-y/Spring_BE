package umc.catchy.domain.course.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GPTPlaceDTO {
    private Long id;
    private String name;
    private String roadAddress;
    private String description;
}
