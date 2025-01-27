package umc.catchy.domain.courseReview.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalDate;

@Entity
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

    private LocalDate visitedDate; // 리뷰 장소 방문 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;
}
