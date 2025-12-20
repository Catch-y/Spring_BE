package umc.catchy.support.fixture;

import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.domain.Place;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class PlaceVisitFixture {

    public static PlaceVisit createPlaceVisit(Long id, Member member, Place place, Course course, LocalDate visitedDate) {
        return PlaceVisit.builder()
                .id(id)
                .member(member)
                .place(place)
                .course(course)
                .isVisited(true)
                .visitedDate(visitedDate)
                .build();
    }

    public static PlaceVisit createTodayVisit(Member member, Place place, Course course) {
        return PlaceVisit.builder()
                .id(1L)
                .member(member)
                .place(place)
                .course(course)
                .isVisited(true)
                .visitedDate(LocalDate.now())
                .build();
    }

    public static PlaceVisit createPlaceVisit(Member member, Place place, Course course) {
        return PlaceVisit.builder()
                .id(1L)
                .member(member)
                .place(place)
                .course(course)
                .isVisited(true)
                .visitedDate(LocalDate.now())
                .build();
    }

    public static List<PlaceVisit> createMultipleVisits(Member member, List<Place> places, Course course) {
        List<PlaceVisit> visits = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            visits.add(PlaceVisit.builder()
                    .id((long) (i + 1))
                    .member(member)
                    .place(places.get(i))
                    .course(course)
                    .isVisited(true)
                    .visitedDate(LocalDate.now().minusDays(i))
                    .build());
        }
        return visits;
    }

    public static List<PlaceVisit> createMultipleVisitsWithDate(Member member, List<Place> places, Course course, LocalDate baseDate) {
        List<PlaceVisit> visits = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            visits.add(PlaceVisit.builder()
                    .id((long) (i + 1))
                    .member(member)
                    .place(places.get(i))
                    .course(course)
                    .isVisited(true)
                    .visitedDate(baseDate.plusDays(i))
                    .build());
        }
        return visits;
    }

    public static PlaceVisit createPlaceVisitWithTime(
            Long id,
            Member member,
            Place place,
            Course course,
            LocalDateTime createdDate) {

        PlaceVisit visit = mock(PlaceVisit.class);

        lenient().when(visit.getId()).thenReturn(id);
        lenient().when(visit.getMember()).thenReturn(member);
        lenient().when(visit.getPlace()).thenReturn(place);
        lenient().when(visit.getCourse()).thenReturn(course);
        lenient().when(visit.isVisited()).thenReturn(true);
        lenient().when(visit.getVisitedDate()).thenReturn(createdDate.toLocalDate());
        lenient().when(visit.getCreatedDate()).thenReturn(createdDate);

        return visit;
    }
}
