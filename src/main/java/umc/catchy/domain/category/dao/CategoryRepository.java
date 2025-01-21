package umc.catchy.domain.category.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByNameIn(List<String> categories);
    Optional<Category> findByBigCategory(BigCategory bigCategory);
}
