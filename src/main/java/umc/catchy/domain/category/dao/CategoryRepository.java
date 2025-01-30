package umc.catchy.domain.category.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByNameIn(List<String> categories);
    @Query("SELECT c.id FROM Category c WHERE c.name IN :names")
    List<Long> findIdsByNames(@Param("names") List<String> names);
}
