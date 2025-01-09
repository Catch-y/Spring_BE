package umc.catchy.infra.config.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JWTProperties {
    private String secret;
    private Long accessTokenValidity;
    private Long refreshTokenValidity;
}
