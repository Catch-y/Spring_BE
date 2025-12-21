package umc.catchy.domain.mapping.placeCourse.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceCourseCommandServiceTest {

    @InjectMocks
    private PlaceCourseCommandService placeCourseCommandService;

    @Mock
    private PlaceRepository placeRepository;

    @Test
    @DisplayName("Google 정보를 바탕으로 장소 저장 - 성공")
    void savePlaceFromGoogleInfo_success() {
        // given
        Long poiId = 12345L;
        Map<String, String> placeDetails = new HashMap<>();
        placeDetails.put("name", "구글 테스트 장소");
        placeDetails.put("description", "구글 설명");
        placeDetails.put("address", "서울시 강남구");
        placeDetails.put("lat", "37.1234");
        placeDetails.put("lon", "127.1234");
        placeDetails.put("activeTime", "24시간");
        placeDetails.put("startTime", "00:00");
        placeDetails.put("endTime", "23:59");
        placeDetails.put("website", "https://google.com");
        placeDetails.put("imageUrl", "https://image.com");

        Place expectedPlace = Place.fromGoogleInfo(poiId, placeDetails);
        given(placeRepository.save(any(Place.class))).willReturn(expectedPlace);

        // when
        Place savedPlace = placeCourseCommandService.savePlaceFromGoogleInfo(poiId, placeDetails);

        // then
        assertAll(
                () -> assertThat(savedPlace).isNotNull(),
                () -> assertThat(savedPlace.getPoiId()).isEqualTo(poiId),
                () -> assertThat(savedPlace.getPlaceName()).isEqualTo("구글 테스트 장소")
        );

        verify(placeRepository).save(any(Place.class));
    }
}
