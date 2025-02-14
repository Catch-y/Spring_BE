package umc.catchy.domain.mapping.placeVisit.dao;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceVisitRepository extends JpaRepository<PlaceVisit, Long> {
    Optional<PlaceVisit> findByPlaceAndMember(Place place, Member member);
    Optional<PlaceVisit> findByPlaceIdAndMemberId(Long placeId, Long memberId);
    Optional<PlaceVisit> findByPlaceAndMemberAndVisitedDate(Place place, Member member, LocalDate visitedDate);

    @Query("SELECT pv FROM PlaceVisit pv JOIN FETCH pv.place p WHERE pv.member.id = :memberId AND p.id IN :placeIds")
    List<PlaceVisit> findPlaceVisitsByMemberAndPlaces(@Param("memberId") Long memberId, @Param("placeIds") List<Long> placeIds);
    List<PlaceVisit> findAllByMemberAndPlaceAndIsVisitedTrue(Member member, Place place);
    List<PlaceVisit> findAllByMemberOrderByVisitedDateDesc(Member member);
    Integer deleteAllByMember(Member member);
}