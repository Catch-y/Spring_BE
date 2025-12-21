package umc.catchy.support.fixture;

import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.place.domain.Place;

import java.util.ArrayList;
import java.util.List;

public class PlaceCourseFixture {

    public static PlaceCourse createPlaceCourse(Long id, Course course, Place place, Integer order) {
        return PlaceCourse.builder()
                .id(id)
                .course(course)
                .place(place)
                .placeOrder(order)
                .build();
    }

    public static PlaceCourse createPlaceCourse(Course course, Place place, Integer order) {
        return PlaceCourse.builder()
                .id(1L)
                .course(course)
                .place(place)
                .placeOrder(order)
                .build();
    }

    public static List<PlaceCourse> createPlaceCourseList(Course course, List<Place> places) {
        List<PlaceCourse> placeCourses = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            placeCourses.add(PlaceCourse.builder()
                    .id((long) (i + 1))
                    .course(course)
                    .place(places.get(i))
                    .placeOrder(i + 1)
                    .build());
        }
        return placeCourses;
    }
}
