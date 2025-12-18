package umc.catchy.support.fixture;

import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalDate;

public class MemberCourseFixture {

    public static MemberCourse createTestMemberCourse(Member member, Course course) {
        return MemberCourse.builder()
                .id(1L)
                .member(member)
                .course(course)
                .isVisited(false)
                .visitedDate(null)
                .bookmark(false)
                .build();
    }

    public static MemberCourse createTestMemberCourse(Long id, Member member, Course course, boolean bookmark) {
        return MemberCourse.builder()
                .id(id)
                .member(member)
                .course(course)
                .isVisited(false)
                .visitedDate(null)
                .bookmark(bookmark)
                .build();
    }

    public static MemberCourse createVisitedMemberCourse(Member member, Course course, LocalDate visitedDate) {
        return MemberCourse.builder()
                .id(1L)
                .member(member)
                .course(course)
                .isVisited(true)
                .visitedDate(visitedDate)
                .bookmark(false)
                .build();
    }

    public static MemberCourse createBookmarkedMemberCourse(Member member, Course course) {
        return MemberCourse.builder()
                .id(1L)
                .member(member)
                .course(course)
                .isVisited(false)
                .visitedDate(null)
                .bookmark(true)
                .build();
    }
}
