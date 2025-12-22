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

@Entity
@Table(name = "member_course", indexes = {
        @Index(name = "idx_member_course_id", columnList = "member_id, course_id")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberCourse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_course_id")
    private Long id;

    @Builder.Default
    private boolean isVisited = false;

    @Builder.Default
    private boolean bookmark = false;

    private LocalDate visitedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    public void toggleBookmark() {
        this.bookmark = !this.bookmark;
    }

    public void markAsVisited(LocalDate visitedDate) {
        this.isVisited = true;
        this.visitedDate = visitedDate;
    }
}
