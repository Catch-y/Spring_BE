package umc.catchy.support.fixture;

import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalTime;

public class CourseFixture {

    public static Course createTestCourse(Member member) {
        return Course.builder()
                .id(1L)
                .courseName("테스트 코스")
                .courseDescription("테스트 설명")
                .courseType(CourseType.DIY)
                .recommendTimeStart(LocalTime.of(9, 0))
                .recommendTimeEnd(LocalTime.of(18, 0))
                .courseImage("https://s3.aws.com/course.jpg")
                .participantsNumber(0L)
                .rating(0.0)
                .hasReview(false)
                .member(member)
                .build();
    }

    public static Course createTestCourse(Long id, String courseName, Member member) {
        return Course.builder()
                .id(id)
                .courseName(courseName)
                .courseDescription("테스트 설명")
                .courseType(CourseType.DIY)
                .recommendTimeStart(LocalTime.of(9, 0))
                .recommendTimeEnd(LocalTime.of(18, 0))
                .courseImage("https://s3.aws.com/course.jpg")
                .participantsNumber(0L)
                .rating(0.0)
                .hasReview(false)
                .member(member)
                .build();
    }

    public static Course createTestCourseWithNoImage(Long id, String courseName, Member member) {
        return Course.builder()
                .id(id)
                .courseName(courseName)
                .courseDescription("테스트 설명")
                .courseType(CourseType.DIY)
                .recommendTimeStart(LocalTime.of(9, 0))
                .recommendTimeEnd(LocalTime.of(18, 0))
                .courseImage(null)
                .participantsNumber(0L)
                .rating(0.0)
                .hasReview(false)
                .member(member)
                .build();
    }

    public static Course createTestCourseWithReviewStatus(Member member, boolean hasReview) {
        return Course.builder()
                .id(1L)
                .courseName("테스트 코스")
                .courseDescription("테스트 설명")
                .courseType(CourseType.DIY)
                .recommendTimeStart(LocalTime.of(9, 0))
                .recommendTimeEnd(LocalTime.of(18, 0))
                .courseImage("https://s3.aws.com/course.jpg")
                .participantsNumber(0L)
                .rating(0.0)
                .hasReview(hasReview)
                .member(member)
                .build();
    }

    public static Course createAiCourse(Member member) {
        return Course.builder()
                .id(2L)
                .courseName("AI 코스")
                .courseDescription("AI가 생성한 코스")
                .courseType(CourseType.AI)
                .recommendTimeStart(LocalTime.of(10, 0))
                .recommendTimeEnd(LocalTime.of(19, 0))
                .courseImage(null)
                .participantsNumber(0L)
                .rating(0.0)
                .hasReview(false)
                .member(member)
                .build();
    }
}
