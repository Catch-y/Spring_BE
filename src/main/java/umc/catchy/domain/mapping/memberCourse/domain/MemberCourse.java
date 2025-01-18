package umc.catchy.domain.mapping.memberCourse.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalDateTime;

@Entity
@Getter
public class MemberCourse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_course_id")
    private Long id;

    private boolean isVisited;

    private LocalDateTime visitedDate;

    private boolean bookmark = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    public static void toggleBookmark(MemberCourse memberCourse) {
        memberCourse.bookmark = !memberCourse.bookmark;
    }
}
