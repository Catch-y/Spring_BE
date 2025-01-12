package umc.catchy.domain.mapping.memberGroup.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class MemberGroup extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_group_id")
    private Long id;

    private LocalDateTime promiseTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Groups group;

    @Builder
    public MemberGroup(LocalDateTime promiseTime, Member member, Groups group) {
        this.promiseTime = promiseTime;
        this.member = member;
        this.group = group;
    }
}
