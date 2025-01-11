package umc.catchy.domain.member.dao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);
    Optional<Member> findByProviderId(String providerId);
    Optional<Member> findByNickname(String nickname);
    Optional<Member> findByEmailAndProviderId(String email, String providerId);
    Optional<Member> findByRefreshToken(String refreshToken);
    Optional<Member> findByAccessToken(String accessToken);
}
