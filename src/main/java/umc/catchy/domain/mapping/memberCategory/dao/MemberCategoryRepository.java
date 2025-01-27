package umc.catchy.domain.mapping.memberCategory.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.mapping.memberCategory.domain.MemberCategory;

import java.util.List;

public interface MemberCategoryRepository extends JpaRepository<MemberCategory, Long> {
    List<MemberCategory> findByMemberId(Long memberId);
}
