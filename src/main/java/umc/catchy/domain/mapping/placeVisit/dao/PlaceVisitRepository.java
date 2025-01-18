package umc.catchy.domain.mapping.placeVisit.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

import java.util.Optional;

@Repository
public interface PlaceVisitRepository extends JpaRepository<PlaceVisit, Long> {
    Optional<PlaceVisit> findByPlaceAndMember(Place place, Member member);
    Optional<PlaceVisit> findByPlaceIdAndMemberId(Long placeId, Long memberId);
}