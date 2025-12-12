package umc.catchy.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import umc.catchy.domain.member.service.JwtTokenService;
import umc.catchy.global.config.security.SecurityConfig;
import umc.catchy.global.error.exception.CustomAccessDeniedHandler;
import umc.catchy.global.error.exception.CustomAuthenticationEntryPoint;
import umc.catchy.global.util.JwtUtil;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Import(SecurityConfig.class)
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean protected CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    @MockitoBean protected CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockitoBean protected JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockitoBean protected JwtTokenService jwtTokenService;
    @MockitoBean protected JwtUtil jwtUtil;

    protected String testToken;

    @BeforeEach
    void setUp() {
        testToken = "Bearer test-token";

        when(jwtTokenService.isAccessTokenValid(anyString())).thenReturn(true);
        when(jwtUtil.getMemberIdFromToken(anyString())).thenReturn(1L);
    }
}
