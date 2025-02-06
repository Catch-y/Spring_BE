package umc.catchy.domain.member.dto.response;

public record AppleLoginResponse(
        String code,
        String id_token
) {
    public static AppleLoginResponse of(String code, String id_token) {
        return new AppleLoginResponse(code, id_token);
    }
}
