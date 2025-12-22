package umc.catchy.domain.course.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalTime;

@Entity
@Table(name = "course", indexes = {
        @Index(name = "idx_course_member_type_date", columnList = "member_id, course_type, created_date desc")
})
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

    private String courseImage;

    private String courseName;

    private LocalTime recommendTimeStart;

    private LocalTime recommendTimeEnd;

    private String courseDescription;

    private Long participantsNumber;

    @Enumerated(EnumType.STRING)
    private CourseType courseType;

    private boolean hasReview;

    private Double rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public static Course createAiCourse(String name, String description, LocalTime start, LocalTime end, Member member) {
        return Course.builder()
                .courseName(name)
                .courseDescription(description)
                .courseType(CourseType.AI)
                .recommendTimeStart(start)
                .recommendTimeEnd(end)
                .participantsNumber(0L)
                .member(member)
                .build();
    }

    public void updateCourseName(String courseName) {
        if (courseName != null && !courseName.isEmpty()) {
            this.courseName = courseName;
        }
    }

    public void updateCourseDescription(String courseDescription) {
        if (courseDescription != null && !courseDescription.isEmpty()) {
            this.courseDescription = courseDescription;
        }
    }

    public void updateCourseImage(String imageUrl) {
        this.courseImage = imageUrl;
    }

    public void updateRecommendTime(LocalTime startTime, LocalTime endTime) {
        this.recommendTimeStart = startTime;
        this.recommendTimeEnd = endTime;
    }

    public void updateRating(Double newRating) {
        if (newRating != null) {
            this.rating = Math.round(newRating * 10) / 10.0;
        }
    }

    public void markAsReviewed() {
        this.hasReview = true;
    }

    public void increaseParticipants() {
        if (this.participantsNumber == null) {
            this.participantsNumber = 0L;
        }
        this.participantsNumber++;
    }
}
