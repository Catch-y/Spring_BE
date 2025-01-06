package umc.catchy.domain.mapping.memberActivetime.domain;

import jakarta.persistence.*;
import lombok.Getter;
import umc.catchy.domain.activetime.domain.ActiveTime;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.member.domain.Member;

@Entity
@Getter
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

}
