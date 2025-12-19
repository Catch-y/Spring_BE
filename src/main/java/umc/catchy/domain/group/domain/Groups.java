package umc.catchy.domain.group.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.course.util.LocationUtils;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "`groups`", indexes = {
        @Index(name = "idx_groups_region", columnList = "sido, sigungu")
})
public class Groups extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    private String groupName;

    private String groupLocation;

    @Column(length = 20)
    private String sido;

    @Column(length = 20)
    private String sigungu;

    private LocalDateTime promiseTime;

    private String inviteCode;

    private String groupImage;

    @PrePersist
    @PreUpdate
    public void preUpdateLocation() {
        if (this.groupLocation != null && !this.groupLocation.isEmpty()) {
            this.sido = LocationUtils.extractUpperLocation(this.groupLocation);
            this.sigungu = LocationUtils.extractLowerLocation(this.groupLocation);
        }
    }
}
