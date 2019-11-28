package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.fo.veilarbvedtaksstotte.utils.DbRole;
import no.nav.sbl.jdbc.Database;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static no.nav.fo.veilarbvedtaksstotte.utils.DbUtils.createDataSource;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class DatabaseConfig {
    public static final String VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY = "VEILARBVEDTAKSSTOTTE_DB_URL";

    @Bean
    public DataSource dataSource() {
        String dbUrl = getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY);
        return createDataSource(DbRole.USER, dbUrl);
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


// TODO: Legg til helsesjekk for postgres

//    @Bean
//    public Pingable dbPinger(final DSLContext dslContext) {
//        HelsesjekkMetadata metadata = new HelsesjekkMetadata("db",
//                "Database: " + getRequiredProperty(VEILARBLEST_DB_URL_PROPERTY),
//                "Database for veilarblest",
//                true);
//
//        return () -> {
//            try {
//                dslContext.selectOne().fetch();
//                return Pingable.Ping.lyktes(metadata);
//            } catch (Exception e) {
//                return Pingable.Ping.feilet(metadata, e);
//            }
//        };
//    }

}
