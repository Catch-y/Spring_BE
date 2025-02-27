package umc.catchy.domain.course.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.course.converter.CourseTypeConverter;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamicInsert
public class Course extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Setter
    private String courseImage;

    @Setter
    private String courseName;

    @Setter
    private LocalTime recommendTimeStart;

    @Setter
    private LocalTime recommendTimeEnd;

    @Setter
    private String courseDescription;

    @Setter
    private Long participantsNumber;

    @Setter
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
