package no.nav.veilarbvedtaksstotte.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;

import javax.sql.DataSource;

public class PostgresContainer {

    private final static String DB_IMAGE = "postgres:11.5";
    private final static String DB_USER = "postgres";
    private final static int DB_PORT = 5432;

    private DataSource dataSource;

    private GenericContainer container = new GenericContainer(DB_IMAGE).withExposedPorts(DB_PORT);

    private String getDbContainerUrl() {
        String containerIp = container.getContainerIpAddress();
        String containerPort = container.getFirstMappedPort().toString();
        return String.format("jdbc:postgresql://%s:%s/postgres", containerIp, containerPort);
    }

    public GenericContainer getContainer() {
        return container;
    }

    public DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(getDbContainerUrl());
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);
            config.setUsername(DB_USER);

            dataSource = new HikariDataSource(config);
        }

        return dataSource;
    }

    public JdbcTemplate getDb() {
        return new JdbcTemplate(getDataSource());
    }

}
