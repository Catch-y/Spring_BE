package umc.catchy.domain.mapping.placeCourse.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.place.domain.Place;

import java.util.List;

@Repository
public interface PlaceCourseRepository extends JpaRepository<PlaceCourse, Long>, PlaceCourseRepositoryCustom {
    List<PlaceCourse> findAllByCourse(Course course);
    List<PlaceCourse> findAllByPlace(Place place);

    @Query("SELECT AVG(pc.place.rating) FROM PlaceCourse pc WHERE pc.course = :course AND pc.place.rating > 0")
    Double calculateAverageRatingByCourse(@Param("course") Course course);
}
