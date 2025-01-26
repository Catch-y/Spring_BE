package umc.catchy.domain.member.dto.response;

public record KakaoLoginResponse(
        String code
) {
    public static KakaoLoginResponse of(String code) {
        return new KakaoLoginResponse(code);
    }
}
