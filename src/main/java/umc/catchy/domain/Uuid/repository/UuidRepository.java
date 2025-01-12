package umc.catchy.domain.Uuid.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.Uuid.domain.Uuid;

public interface UuidRepository extends JpaRepository<Uuid, Long> {
}
