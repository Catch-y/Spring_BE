package umc.catchy.support.fixture;

import org.springframework.mock.web.MockMultipartFile;
import umc.catchy.domain.group.domain.Groups;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;

import java.time.LocalDateTime;

public class GroupFixture {

    public static CreateGroupRequest createGroupRequest(String name, String inviteCode) {
        MockMultipartFile mockImage = new MockMultipartFile(
                "groupImage",
                "test-image.jpg",
                "image/jpeg",
                "test-image-content".getBytes()
        );

        return new CreateGroupRequest(
                name,
                "서울시 강남구 테헤란로",
                LocalDateTime.of(2025, 1, 25, 15, 30),
                inviteCode,
                mockImage
        );
    }

    public static Groups createGroup(String name, String inviteCode) {
        return Groups.builder()
                .groupName(name)
                .groupLocation("서울시 강남구 테헤란로")
                .promiseTime(LocalDateTime.of(2025, 1, 25, 15, 30))
                .inviteCode(inviteCode)
                .groupImage("https://catchy-s3.com/group-images/test-image.jpg")
                .build();
    }

    public static Groups createGroupWithId(Long id, String name, String inviteCode) {
        return Groups.builder()
                .id(id)
                .groupName(name)
                .groupLocation("서울시 강남구 테헤란로")
                .sido("서울")
                .sigungu("강남구")
                .promiseTime(LocalDateTime.of(2025, 1, 25, 15, 30))
                .inviteCode(inviteCode)
                .groupImage("https://catchy-s3.com/group-images/test-image.jpg")
                .build();
    }

    public static Groups createGroupWithIdAndYearAndMonth(Long id, int year, int month) {
        return Groups.builder()
                .id(id)
                .groupName("테스트 그룹")
                .groupLocation("서울시 강남구 테헤란로")
                .sido("서울")
                .sigungu("강남구")
                .promiseTime(LocalDateTime.of(year, month, 25, 15, 30))
                .inviteCode("ABC123")
                .groupImage("https://catchy-s3.com/group-images/test-image.jpg")
                .build();
    }
}
