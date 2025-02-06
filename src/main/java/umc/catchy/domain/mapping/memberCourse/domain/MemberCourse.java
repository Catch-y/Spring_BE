package umc.catchy.domain.mapping.memberCourse.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberCourse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_course_id")
    private Long id;

    private boolean isVisited = false;

    private LocalDate visitedDate;

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
