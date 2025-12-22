package umc.catchy.domain.courseReviewImage.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.courseReview.domain.CourseReview;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReviewImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "courseReviewImage_id")
    private Long id;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courseReview_id")
    private CourseReview courseReview;

    public static CourseReviewImage create(String imageUrl, CourseReview courseReview) {
        return CourseReviewImage.builder()
                .imageUrl(imageUrl)
                .courseReview(courseReview)
                .build();
    }
}
