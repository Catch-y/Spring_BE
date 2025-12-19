package umc.catchy.domain.mapping.placeLike.dao;


import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.placeLike.domain.PlaceLike;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

@Repository
public interface PlaceLikeRepository extends JpaRepository<PlaceLike, Long> {
    Optional<PlaceLike> findByPlaceAndMember(Place place, Member member);
    Integer deleteAllByMember(Member member);

    @Query("SELECT pl.place.id FROM PlaceLike pl WHERE pl.member.id = :memberId AND pl.place.id IN :placeIds")
    Set<Long> findLikedPlaceIdsByMemberAndPlaceIds(@Param("memberId") Long memberId, @Param("placeIds") List<Long> placeIds);
}