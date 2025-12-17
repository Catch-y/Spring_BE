package umc.catchy.domain.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseCommandService {

    private final CourseRepository courseRepository;
    private final PlaceCourseRepository placeCourseRepository;
    private final MemberCourseRepository memberCourseRepository;
    private final PlaceRepository placeRepository;

    public Course saveCourse(Course course) {
        return courseRepository.save(course);
    }

    public void registerPlacesToCourse(Course course, List<Long> placeIds) {
        List<Place> places = placeRepository.findAllById(placeIds);

        Map<Long, Place> placeMap = places.stream()
                .collect(Collectors.toMap(Place::getId, p -> p));

        List<Place> sortedPlaces = placeIds.stream()
                .map(placeMap::get)
                .filter(Objects::nonNull)
                .toList();

        List<PlaceCourse> placeCourses = new ArrayList<>();
        double totalRating = 0.0;
        int validRatingCount = 0;

        for (int i = 0; i < sortedPlaces.size(); i++) {
            Place place = sortedPlaces.get(i);

            placeCourses.add(PlaceCourse.builder()
                    .course(course)
                    .place(place)
                    .placeOrder(i + 1)
                    .build());

            if (place.getRating() != null && place.getRating() > 0) {
                totalRating += place.getRating();
                validRatingCount++;
            }
        }

        placeCourseRepository.saveAll(placeCourses);

        double averageRating = 0.0;
        if (validRatingCount > 0) {
            averageRating = totalRating / validRatingCount;
        }
        course.updateRating(averageRating);
    }

    public MemberCourse saveMemberCourse(Course course, Member member) {
        MemberCourse memberCourse = MemberCourse.builder()
                .course(course)
                .member(member)
                .build();
        return memberCourseRepository.save(memberCourse);
    }

    public void updateCourse(Course course, String courseName, String courseDescription,
                             String imageUrl, List<Long> placeIds,
                             LocalTime startTime, LocalTime endTime) {

        course.updateCourseName(courseName);
        course.updateCourseDescription(courseDescription);

        if (imageUrl != null) {
            course.updateCourseImage(imageUrl);
        }

        if (placeIds != null && !placeIds.isEmpty()) {
            List<PlaceCourse> originPlaces = placeCourseRepository.findAllByCourse(course);
            placeCourseRepository.deleteAll(originPlaces);
            placeCourseRepository.flush();
            registerPlacesToCourse(course, placeIds);
        }

        if (startTime != null && endTime != null) {
            course.updateRecommendTime(startTime, endTime);
        }
    }

    public void deleteCourse(Course course, Member member) {
        MemberCourse memberCourse = memberCourseRepository.findByCourseAndMember(course, member)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_INVALID_MEMBER));

        memberCourseRepository.delete(memberCourse);
        List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourse(course);
        placeCourseRepository.deleteAll(placeCourses);
        courseRepository.delete(course);
    }
}
