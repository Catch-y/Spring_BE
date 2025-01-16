package umc.catchy.domain.mapping.memberCourse.dao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.member.domain.Member;

public interface MemberCourseRepository extends JpaRepository<MemberCourse, Long> {
    List<MemberCourse> findAllByMember(Member member);
}
