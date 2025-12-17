package umc.catchy.support.fixture;

import umc.catchy.domain.member.domain.FcmInfo;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.domain.SocialType;

public class MemberFixture {

    public static Member createTestMember() {
        return Member.builder()
                .id(1L)
                .providerId("kakao_123")
                .email("test@test.com")
                .nickname("테스트유저")
                .profileImage("https://s3.aws.com/profile.jpg")
                .socialType(SocialType.KAKAO)
                .fcmInfo(FcmInfo.createFcmInfo())
                .build();
    }

    public static Member createTestMember(Long id, String email) {
        return Member.builder()
                .id(id)
                .providerId("kakao_" + id)
                .email(email)
                .nickname("테스트유저" + id)
                .profileImage("https://s3.aws.com/profile.jpg")
                .socialType(SocialType.KAKAO)
                .fcmInfo(FcmInfo.createFcmInfo())
                .build();
    }
}
