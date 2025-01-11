package umc.catchy.domain.courseReviewImage.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.courseReview.Domain.CourseReview;

@Entity
@Getter
public class CourseReviewImage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "courseReviewImage_id")
    private long id;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courseReview_id")
    private CourseReview courseReview;
}
