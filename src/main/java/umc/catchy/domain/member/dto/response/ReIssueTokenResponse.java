package umc.catchy.domain.member.dto.response;

public record ReIssueTokenResponse(
        String accessToken,
        String refreshToken
) {
    public static ReIssueTokenResponse of(TokenPair tokens) {
        return new ReIssueTokenResponse(tokens.accessToken(), tokens.refreshToken());
    }
}
