package umc.catchy.domain.mapping.placeCourse.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaceCourseCommandService {

    private final PlaceRepository placeRepository;

    public Place savePlaceFromGoogleInfo(Long poiId, Map<String, String> placeDetails) {
        Place place = Place.fromGoogleInfo(poiId, placeDetails);
        return placeRepository.save(place);
    }
}
