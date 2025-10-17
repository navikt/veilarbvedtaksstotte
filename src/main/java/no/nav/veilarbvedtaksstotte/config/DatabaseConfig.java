package no.nav.veilarbvedtaksstotte.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableTransactionManagement
@RequiredArgsConstructor
public class DatabaseConfig {

    private final EnvironmentProperties environmentProperties;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = createDataSourceConfig(environmentProperties.getDbUrl(), 15);
        return new HikariDataSource(config);
    }

    @Bean (initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .validateMigrationNaming(true)
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

    public static HikariConfig createDataSourceConfig(String dbUrl, int maximumPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(600000); // 10min
        config.setMinimumIdle(1);
        return config;
    }
}
