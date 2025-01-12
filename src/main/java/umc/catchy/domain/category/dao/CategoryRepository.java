package umc.catchy.domain.category.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.category.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
