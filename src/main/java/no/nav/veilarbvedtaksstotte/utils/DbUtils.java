package no.nav.veilarbvedtaksstotte.utils;

import com.zaxxer.hikari.HikariConfig;
import lombok.SneakyThrows;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.flywaydb.core.Flyway;
import org.springframework.dao.EmptyResultDataAccessException;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Supplier;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

public class DbUtils {

    public static String toPostgresArray(List<String> values) {
        return "{" + String.join(",", values) + "}";
    }

    public static <T> T queryForObjectOrNull(Supplier<T> query) {
        try {
            return query.get();
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public static DataSource createDataSource(String dbUrl, DbRole dbRole) {
        HikariConfig config = createDataSourceConfig(dbUrl);
        return createVaultRefreshDataSource(config, dbRole);
    }

    @SneakyThrows
    public static void migrateAndClose(DataSource dataSource, DbRole dbRole) {
        migrate(dataSource, dbRole);
        dataSource.getConnection().close();
    }

    public static void migrate(DataSource dataSource, DbRole dbRole) {
        Flyway.configure()
                .dataSource(dataSource)
                .initSql(String.format("SET ROLE \"%s\"", toDbRoleStr(dbRole)))
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    public static HikariConfig createDataSourceConfig(String dbUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        return config;
    }

    @SneakyThrows
    private static DataSource createVaultRefreshDataSource(HikariConfig config, DbRole dbRole) {
        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, getMountPath(), toDbRoleStr(dbRole));
    }

    private static String getMountPath() {
        boolean isProd = isProduction().orElse(false);
        return "postgresql/" + (isProd ? "prod-fss" : "preprod-fss");
    }

    public static String toDbRoleStr(DbRole dbRole) {
        String environment = isProduction().orElse(false) ? "p" : "q1";
        String dbName = isProduction().orElse(false) ? "veilarbvedtaksstotte-fss13" : "veilarbvedtaksstotte-fss15";
        String role = EnumUtils.getName(dbRole).toLowerCase();
        return String.join("-", dbName, environment, role);
    }

}
