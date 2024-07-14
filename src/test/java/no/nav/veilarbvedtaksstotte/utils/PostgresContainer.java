package no.nav.veilarbvedtaksstotte.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.sql.DataSource;

public class PostgresContainer {

    public static PostgreSQLContainer<?> postgreDBContainer = new PostgreSQLContainer<>("postgres:14.1-alpine");

    public PostgresContainer() {
        postgreDBContainer.setWaitStrategy(Wait.defaultWaitStrategy());
        postgreDBContainer.start();
    }

    public void stopContainer() {
        postgreDBContainer.stop();
    }

    public DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getDbContainerUrl());
        config.setUsername(postgreDBContainer.getUsername());
        config.setPassword(postgreDBContainer.getPassword());
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);

        return new HikariDataSource(config);
    }

    public JdbcTemplate createJdbcTemplate() {
        return new JdbcTemplate(createDataSource());
    }

    private String getDbContainerUrl() {
        String containerIp = postgreDBContainer.getHost();
        String containerPort = postgreDBContainer.getFirstMappedPort().toString();
        return String.format("jdbc:postgresql://%s:%s/postgres", containerIp, containerPort);
    }

}
