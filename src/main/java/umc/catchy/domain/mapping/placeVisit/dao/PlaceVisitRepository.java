package umc.catchy.domain.mapping.placeVisit.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

@Repository
public interface PlaceVisitRepository extends JpaRepository<PlaceVisit, Long> {
    PlaceVisit findByPlaceAndMember(Place place, Member member);
}