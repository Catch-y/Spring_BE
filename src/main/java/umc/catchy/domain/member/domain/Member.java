package umc.catchy.domain.member.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.catchy.domain.common.BaseTimeEntity;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "provider_id", nullable = false, length = 50)
    private String providerId;

    private String email;

    private String nickname;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    @Enumerated(EnumType.STRING)
    private MemberState state;

    @Setter
    private String accessToken;

    @Setter
    private String refreshToken;

    private Integer gpt_count;

    //사용자 인증 도입 후에는 필요없음
    public Member(Long id) {
        this.id = id;
    }

    public static Member createMember(
            String providerId,
            String email,
            String nickname,
            String profileImage,
            SocialType socialType
    ) {
        return Member.builder()
                .providerId(providerId)
                .email(email)
                .nickname(nickname)
                .profileImage(profileImage)
                .socialType(socialType)
                .state(MemberState.ACTIVE)
                .build();
    }
}
