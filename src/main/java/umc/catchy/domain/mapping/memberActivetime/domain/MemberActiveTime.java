package umc.catchy.domain.mapping.memberActivetime.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.activetime.domain.ActiveTime;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.member.domain.Member;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberActiveTime extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_activeTime_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activeTime_id")
    private ActiveTime activeTime;

    @Builder
    public MemberActiveTime(Member member, ActiveTime activeTime) {
        this.member = member;
        this.activeTime = activeTime;
    }

    public static MemberActiveTime createMemberActiveTime(Member member, ActiveTime activeTime) {
        return MemberActiveTime.builder()
                .member(member)
                .activeTime(activeTime)
                .build();
    }
}
