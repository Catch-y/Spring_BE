package umc.catchy.domain.mapping.memberCourse.dao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.member.domain.Member;

import java.util.Optional;

@Repository
public interface MemberCourseRepository extends JpaRepository<MemberCourse, Long> {
    Optional<MemberCourse> findByCourseAndMember(Course course, Member member);
    List<MemberCourse> findAllByMember(Member member);
    Optional<MemberCourse> findByCourseIdAndMemberId(Long courseId, Long memberId);
}
