package no.nav.fo.veilarbvedtaksstotte.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.sbl.jdbc.Database;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static no.nav.fo.veilarbvedtaksstotte.config.DatabaseConfig.VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY;
import static no.nav.fo.veilarbvedtaksstotte.utils.DbUtils.createDataSourceConfig;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class DatabaseTestConfig {

    @Bean
    public DataSource dataSource() {
        String dbUrl = getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY);
        HikariConfig config = createDataSourceConfig(dbUrl);
        config.setUsername("postgres");
        config.setPassword("qwerty");

        DataSource source = new HikariDataSource(config);
        DbTestUtils.testMigrate(source);

        return source;
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public Database database(JdbcTemplate jdbcTemplate) {
        return new Database(jdbcTemplate);
    }

    @Bean
    public Transactor transactor(PlatformTransactionManager platformTransactionManager) {
        return new Transactor(platformTransactionManager);
    }

}
