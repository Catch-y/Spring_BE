package umc.catchy.domain.group.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.group.dto.request.CreateGroupRequest;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;
import umc.catchy.support.fixture.GroupFixture;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupFacadeTest {

    @InjectMocks
    private GroupFacade groupFacade;

    @Mock
    private GroupCommandService groupCommandService;

    @Mock
    private AmazonS3Manager amazonS3Manager;

    @Test
    @DisplayName("그룹 생성 실패 시 S3에 업로드된 파일이 삭제되어야 함")
    void createGroup_fail_then_delete_s3_image() {
        CreateGroupRequest request = GroupFixture.createGroupRequest("테스트 그룹", "ABC1234");
        String uploadedUrl = "https://s3.com/test.jpg";

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(amazonS3Manager.uploadFile(anyString(), any(MultipartFile.class))).thenReturn(uploadedUrl);

            when(groupCommandService.createGroupMetadata(any(), anyLong(), anyString(), any()))
                    .thenThrow(new RuntimeException("DB 저장 실패"));

            assertThatThrownBy(() -> groupFacade.createGroup(request))
                    .isInstanceOf(RuntimeException.class);

            verify(amazonS3Manager, times(1)).deleteImage(uploadedUrl);
        }
    }
}
