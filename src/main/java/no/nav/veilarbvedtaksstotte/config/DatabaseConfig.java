package no.nav.veilarbvedtaksstotte.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableTransactionManagement
@RequiredArgsConstructor
public class DatabaseConfig {

    private final EnvironmentProperties environmentProperties;

    public static DataSource createDataSource(String dbUrl) {
        try {
            HikariConfig config = createDataSourceConfig(dbUrl, 15);
            return new HikariDataSource(config);
        } catch (Exception e) {
            log.info("Can't connect to db, error: " + e, e);
            return null;
        }
    }

    public static HikariConfig createDataSourceConfig(String dbUrl, int maximumPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(600000); // 10min
        config.setMinimumIdle(1);
        return config;
    }


    @PostConstruct
    @SneakyThrows
    public void migrate() {
        DataSource dataSource = createDataSource(environmentProperties.getDbUrl());

        if (dataSource != null) {
            log.info("Starting database migration...");
            Flyway.configure()
                    .validateMigrationNaming(true)
                    .dataSource(dataSource)
                    .locations("db/postgres")
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();

            dataSource.getConnection().close();
        }
    }

}
