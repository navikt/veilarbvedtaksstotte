package no.nav.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.utils.DbRole;
import no.nav.veilarbvedtaksstotte.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import static no.nav.veilarbvedtaksstotte.utils.DbUtils.createDataSource;

@Slf4j
@Configuration
public class DatabaseConfig {

    private final EnvironmentProperties environmentProperties;

    @Autowired
    public DatabaseConfig(EnvironmentProperties environmentProperties) {
        this.environmentProperties = environmentProperties;
    }

    @Bean
    public DataSource dataSource(EnvironmentProperties properties) {
        return createDataSource(properties.getDbUrl(), DbRole.USER);
    }

    @PostConstruct
    public void migrateDb() {
        log.info("Starting database migration...");
        DbUtils.migrateAndClose(DbUtils.createDataSource(environmentProperties.getDbUrl(), DbRole.ADMIN), DbRole.ADMIN);
    }

}
