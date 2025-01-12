package umc.catchy.domain.category.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.category.domain.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByNameIn(List<String> categories);
}
