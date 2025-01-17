package umc.catchy.domain.course.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalTime;

@Entity
@Getter
public class Course extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Setter
    private String courseImage;

    @Setter
    private String courseName;

    private LocalTime recommendTimeStart;

    private LocalTime recommendTimeEnd;

    @Setter
    private String courseDescription;

    private Long participantsNumber;

    @Enumerated(EnumType.STRING)
    private CourseType courseType;

    @Setter
    private boolean hasReview;

    @Setter
    private Double rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}
