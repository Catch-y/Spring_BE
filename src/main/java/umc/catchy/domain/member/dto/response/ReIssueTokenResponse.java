package umc.catchy.domain.member.dto.response;

public record ReIssueTokenResponse(
        String accessToken,
        String refreshToken
) {
    public static ReIssueTokenResponse of(String accessToken, String refreshToken) {
        return new ReIssueTokenResponse(accessToken, refreshToken);
    }
}
