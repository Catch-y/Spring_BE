package umc.catchy.domain.style.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.style.domain.Style;
import umc.catchy.domain.style.domain.StyleName;

import java.util.List;

@Repository
public interface StyleRepository extends JpaRepository<Style, Long> {
    List<Style> findAllByNameIn(List<StyleName> name);
}
