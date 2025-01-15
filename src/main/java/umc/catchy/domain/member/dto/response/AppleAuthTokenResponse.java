package umc.catchy.domain.member.dto.response;

public record AppleAuthTokenResponse(
        String accessToken,
        Integer expires_in,
        String id_token,
        String refresh_token,
        String token_type
) {
}
