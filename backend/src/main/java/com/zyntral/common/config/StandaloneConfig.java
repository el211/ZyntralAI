package com.zyntral.common.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Zero-dependency local run: boots a real embedded PostgreSQL inside the JVM (no Docker), so
 * the full PG schema (enums, partitioning, jsonb, …) works. Activate with the {@code standalone}
 * profile: {@code mvn spring-boot:run -Dspring-boot.run.profiles=standalone}. Flyway applies the
 * migrations on the embedded instance at startup.
 *
 * <p>The embedded Postgres binaries are {@code provided} scope, so this is excluded from the
 * production jar — prod uses the external Postgres configured via environment variables.
 */
@Configuration
@Profile("standalone")
public class StandaloneConfig {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder().setPort(5432).start();
    }

    @Bean
    @Primary
    public DataSource dataSource(EmbeddedPostgres postgres) {
        return postgres.getPostgresDatabase();
    }
}
