package no.nav.veilarbvedtaksstotte.config;

import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class HealthConfig {

    @Bean
    public DataSourceHealthIndicator dataSourceHealthIndicator(DataSource dataSource) {
        return new DataSourceHealthIndicator(dataSource, "SELECT 1");
    }
}
