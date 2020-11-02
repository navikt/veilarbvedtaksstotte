package no.nav.veilarbvedtaksstotte.utils;

import com.zaxxer.hikari.HikariConfig;
import lombok.SneakyThrows;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.List;

import static no.nav.common.utils.EnvironmentUtils.isProduction;
import static no.nav.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;

public class DbUtils {

    public static String toPostgresArray(List<String> values) {
        return "{" + String.join(",", values) + "}";
    }

    public static <T> T firstInList(List<T> listResult) {
        return listResult == null || listResult.isEmpty() ? null : listResult.get(0);
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
        config.setMaximumPoolSize(3);
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
        String role = EnumUtils.getName(dbRole).toLowerCase();
        return String.join("-", APPLICATION_NAME, environment, role);
    }

}
