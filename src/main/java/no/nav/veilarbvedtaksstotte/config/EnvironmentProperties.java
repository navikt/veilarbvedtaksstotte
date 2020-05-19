package no.nav.veilarbvedtaksstotte.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.env")
public class EnvironmentProperties {

    private String openAmDiscoveryUrl;

    private String openAmClientId;

    private String stsDiscoveryUrl;

    private String refreshUrl;

    private String abacUrl;

    private String aktorregisterUrl;

    private String dbUrl;

}
