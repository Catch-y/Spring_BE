package umc.catchy.domain.course.domain;

import jakarta.persistence.*;
import lombok.Getter;
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

    private String courseImage;

    private String courseName;

    private LocalTime recommendTimeStart;

    private LocalTime recommendTimeEnd;

    private String courseDescription;

    private Long participantsNumber;

    @Enumerated(EnumType.STRING)
    private CourseType courseType;

    private boolean hasReview;

    private Float rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}
