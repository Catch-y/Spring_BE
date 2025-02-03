package umc.catchy.domain.reviewReport.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.courseReview.domain.CourseReview;
import umc.catchy.domain.placeReview.domain.PlaceReview;

@Entity
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReport extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_report_id")
    private Long id;

    private String reason;

    @Enumerated(EnumType.STRING)
    private ReviewType reviewType;

    //리뷰 타입에 맞게 둘 중 하나만 값을 가지도록 함.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_review_id", nullable = true)
    private CourseReview courseReview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_review_id", nullable = true)
    private PlaceReview placeReview;
}
