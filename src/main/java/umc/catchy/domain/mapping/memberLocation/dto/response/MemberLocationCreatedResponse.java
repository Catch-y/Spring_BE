package umc.catchy.domain.mapping.memberLocation.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MemberLocationCreatedResponse {
    List<Long> memberLocationId;
}
