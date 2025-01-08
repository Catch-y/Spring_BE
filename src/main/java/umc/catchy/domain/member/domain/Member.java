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
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "provider_id", nullable = false, length = 50)
    private Long providerId;

    private String email;

    private String nickname;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    @Enumerated(EnumType.STRING)
    private MemberState state;

    @Setter
    private String access_token;

    @Setter
    private String refresh_token;

    private Integer gpt_count;

    public static Member createMember(
            Long providerId,
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
