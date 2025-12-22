package umc.catchy.domain.group.dto.response;

public record GroupJoinResponse(
        boolean success,
        String message
) {
    public static GroupJoinResponse of(boolean success, String message) {
        return new GroupJoinResponse(success, message);
    }
}
