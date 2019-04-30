package no.nav.fo.veilarbvedtaksstotte.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.nav.sbl.jdbc.Database;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    public static final String VEILARBVEDTAKSSTOTTE_DB_URL = "VEILARBVEDTAKSSTOTTEDB_URL";
    public static final String VEILARBVEDTAKSSTOTTE_DB_USERNAME = "VEILARBVEDTAKSSTOTTEDB_USERNAME";
    public static final String VEILARBVEDTAKSSTOTTE_DB_PASSWORD = "VEILARBVEDTAKSSTOTTEDB_PASSWORD";

    @Bean
    public static DataSource getDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_URL));
        config.setUsername(getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_USERNAME));
        config.setPassword(getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_PASSWORD));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        return new HikariDataSource(config);
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
