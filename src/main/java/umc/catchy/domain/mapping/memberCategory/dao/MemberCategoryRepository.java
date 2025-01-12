package umc.catchy.domain.mapping.memberCategory.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.mapping.memberCategory.domain.MemberCategory;

public interface MemberCategoryRepository extends JpaRepository<MemberCategory, Long> {
}
