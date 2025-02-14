package umc.catchy.domain.mapping.placeLike.dao;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.placeLike.domain.PlaceLike;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

@Repository
public interface PlaceLikeRepository extends JpaRepository<PlaceLike, Long> {
    Optional<PlaceLike> findByPlaceAndMember(Place place, Member member);
    Integer deleteAllByMember(Member member);
}