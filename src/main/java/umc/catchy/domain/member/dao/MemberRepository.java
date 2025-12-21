package umc.catchy.domain.member.dao;

import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.member.domain.Member;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);
    Optional<Member> findByProviderId(String providerId);
    Optional<Member> findByNickname(String nickname);
    Optional<Member> findByEmailAndProviderId(String email, String providerId);

    @Modifying
    @Query(value = """
            DELETE FROM member_style WHERE member_id = :memberId;
                    DELETE FROM member_place_vote WHERE member_id = :memberId;
                    DELETE FROM member_category_vote WHERE member_id = :memberId;
                    DELETE FROM member_active_time WHERE member_id = :memberId;
                    DELETE FROM member_location WHERE member_id = :memberId;
                    DELETE FROM member_group WHERE member_id = :memberId;
                    DELETE FROM member_course WHERE member_id = :memberId;
                    DELETE FROM member_category WHERE member_id = :memberId;
                    DELETE FROM place_visit WHERE member_id = :memberId;
                    DELETE FROM place_like WHERE member_id = :memberId;
            """, nativeQuery = true)
    void deleteAllRelatedEntities(@Param("memberId") Long memberId);

    @Query("SELECT m.id FROM Member m")
    List<Long> findAllMemberIds();
}
