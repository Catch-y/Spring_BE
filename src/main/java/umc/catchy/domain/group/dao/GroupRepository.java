package umc.catchy.domain.group.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.group.domain.Groups;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Groups, Long> {
    Optional<Groups> findByInviteCode(String inviteCode);
}