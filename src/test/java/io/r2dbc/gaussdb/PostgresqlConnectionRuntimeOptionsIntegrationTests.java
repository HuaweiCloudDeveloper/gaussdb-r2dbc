package io.r2dbc.gaussdb;

import io.r2dbc.gaussdb.util.GaussDBServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests for {@link GaussDBConnection} connection options.
 */
final class PostgresqlConnectionRuntimeOptionsIntegrationTests {

    @RegisterExtension
    static final GaussDBServerExtension SERVER = new GaussDBServerExtension();

    private static final Map<String, String> options = new HashMap<>();

    static {
//        options.put("lock_timeout", "5000");
        options.put("statement_timeout", "60s");
    }

    private final GaussDBConnectionConfiguration configuration = GaussDBConnectionConfiguration.builder()
        .database(SERVER.getDatabase())
        .host(SERVER.getHost())
        .port(SERVER.getPort())
        .password(SERVER.getPassword())
        .username(SERVER.getUsername())
        .forceBinary(true)
        .options(options)
        .build();

    private final GaussDBConnectionFactory connectionFactory = new GaussDBConnectionFactory(this.configuration);

    @Test
    void connectionFactoryShouldApplyParameters() {
        GaussDBConnection connection = (GaussDBConnection) connectionFactory.create().block();
// TODO: GaussDB do not support lock_timeout parameter
//        connection
//            .createStatement("SHOW lock_timeout").execute()
//            .flatMap(result -> result.map((row, rowMetadata) -> row.get("lock_timeout", String.class)))
//            .as(StepVerifier::create)
//            .expectNext("5s")
//            .verifyComplete();

        connection
            .createStatement("SHOW statement_timeout").execute()
            .flatMap(result -> result.map((row, rowMetadata) -> row.get("statement_timeout", String.class)))
            .as(StepVerifier::create)
            .expectNext("1min")
            .verifyComplete();

        connection.close().block();
    }

    @Test
    void connectionFactoryShouldApplyParametersUsingTimeoutApis() {
        // TODO: GaussDB do not support lock_timeout parameter
        GaussDBConnection connection = (GaussDBConnection) connectionFactory.create().block();
//        connection.setLockWaitTimeout(Duration.ofSeconds(10)).block();
        connection.setStatementTimeout(Duration.ofMinutes(2)).block();

//        connection
//            .createStatement("SHOW lock_timeout").execute()
//            .flatMap(result -> result.map((row, rowMetadata) -> row.get("lock_timeout", String.class)))
//            .as(StepVerifier::create)
//            .expectNext("10s")
//            .verifyComplete();

        connection
            .createStatement("SHOW statement_timeout").execute()
            .flatMap(result -> result.map((row, rowMetadata) -> row.get("statement_timeout", String.class)))
            .as(StepVerifier::create)
            .expectNext("2min")
            .verifyComplete();

        connection.close().block();
    }

}
