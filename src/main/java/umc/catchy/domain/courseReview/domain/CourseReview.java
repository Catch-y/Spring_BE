package umc.catchy.domain.courseReview.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalDate;

@Entity
@Table(name = "course_review", indexes = {
        @Index(name = "idx_course_review_id_date", columnList = "course_id, createdAt DESC")
})
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "courseReview_id")
    private Long id;

    private String comment;

    private LocalDate createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Builder.Default
    private Boolean isReported = false;

    public void markAsReported() {
        this.isReported = true;
    }

    public static CourseReview create(Member member, Course course, String comment) {
        return CourseReview.builder()
                .comment(comment)
                .member(member)
                .course(course)
                .createdAt(LocalDate.now())
                .build();
    }
}
