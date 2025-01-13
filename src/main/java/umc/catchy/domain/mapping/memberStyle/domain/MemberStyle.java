package umc.catchy.domain.mapping.memberStyle.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.common.BaseTimeEntity;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.style.domain.Style;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberStyle extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_style_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_id")
    private Style style;

    @Builder
    public MemberStyle(Member member, Style style) {
        this.member = member;
        this.style = style;
    }
    public static MemberStyle createMemberStyle(Member member, Style style) {
        return MemberStyle.builder()
                .member(member)
                .style(style)
                .build();
    }
}
