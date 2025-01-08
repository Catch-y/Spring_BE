package umc.catchy.domain.member.dao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);
    Optional<Member> findByProviderId(Long providerId);
    Optional<Member> findByNickname(String nickname);
}
