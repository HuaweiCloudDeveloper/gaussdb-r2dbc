/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.gaussdb;

import com.ongres.scram.client.ScramClient;
import io.r2dbc.gaussdb.client.Client;
import io.r2dbc.gaussdb.client.TestClient;
import io.r2dbc.gaussdb.client.TestStartupParameterProvider;
import io.r2dbc.gaussdb.message.backend.AuthenticationMD5Password;
import io.r2dbc.gaussdb.message.backend.AuthenticationOk;
import io.r2dbc.gaussdb.message.backend.AuthenticationSASL;
import io.r2dbc.gaussdb.message.backend.ErrorResponse;
import io.r2dbc.gaussdb.message.frontend.PasswordMessage;
import io.r2dbc.gaussdb.message.frontend.SASLInitialResponse;
import io.r2dbc.gaussdb.message.frontend.StartupMessage;
import io.r2dbc.gaussdb.util.ByteBufferUtils;
import io.r2dbc.spi.R2dbcNonTransientResourceException;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static io.r2dbc.gaussdb.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link GaussDBConnectionFactory}.
 */
final class PostgresqlConnectionFactoryUnitTests {

    @Test
    void constructorNoConfiguration() {
        assertThatIllegalArgumentException().isThrownBy(() -> new GaussDBConnectionFactory(null))
            .withMessage("configuration must not be null");
    }

    @Test
    void createAuthenticationMD5Password() {
        // @formatter:off
        Client client = TestClient.builder()
            .window()
                .expectRequest(new StartupMessage( "test-database", "test-username", new TestStartupParameterProvider()))
                .thenRespond(new AuthenticationMD5Password(TEST.buffer(4).writeInt(100)))
                .expectRequest(new PasswordMessage("md55e9836cdb369d50e3bc7d127e88b4804"))
                .thenRespond(AuthenticationOk.INSTANCE)
                .done()
            .build();
        // @formatter:on

        GaussDBConnectionConfiguration configuration = GaussDBConnectionConfiguration.builder()
            .applicationName("test-application-name")
            .database("test-database")
            .host("test-host")
            .username("test-username")
            .password("test-password")
            .build();

        new GaussDBConnectionFactory(testClientFactory(client, configuration), configuration)
            .create()
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void createAuthenticationSASL() {
        ScramClient scramClient = ScramClient.builder()
            .advertisedMechanisms(Collections.singletonList("SCRAM-SHA-256"))
            .username("test-username")
            .password("test-password".toCharArray())
            .build();

        // @formatter:off
        Client client = TestClient.builder()
            .window()
                .expectRequest(new StartupMessage( "test-database", "test-username", new TestStartupParameterProvider())).thenRespond(new AuthenticationSASL(Collections.singletonList("SCRAM-SHA-256")))
                .expectRequest(new SASLInitialResponse(ByteBufferUtils.encode(scramClient.clientFirstMessage().toString()), "SCRAM-SHA-256")).thenRespond(AuthenticationOk.INSTANCE)
                .done()
            .build();
        // @formatter:on

        GaussDBConnectionConfiguration configuration = GaussDBConnectionConfiguration.builder()
            .applicationName("test-application-name")
            .database("test-database")
            .host("test-host")
            .username("test-username")
            .password("test-password")
            .build();
    }

    @Test
    void createError() {
        // @formatter:off
        Client client = TestClient.builder()
            .window()
                .expectRequest(new StartupMessage( "test-database", "test-username", new TestStartupParameterProvider())).thenRespond(new ErrorResponse(Collections.emptyList()))
                .done()
            .build();
        // @formatter:on

        GaussDBConnectionConfiguration configuration = GaussDBConnectionConfiguration.builder()
            .applicationName("test-application-name")
            .database("test-database")
            .host("test-host")
            .username("test-username")
            .password("test-password")
            .build();

        new GaussDBConnectionFactory(testClientFactory(client, configuration), configuration).create()
            .as(StepVerifier::create)
            .verifyErrorMatches(R2dbcNonTransientResourceException.class::isInstance);
    }

    @Test
    void getMetadata() {
        // @formatter:off
        Client client = TestClient.builder()
            .window()
                .expectRequest(new StartupMessage( "test-database", "test-username", new TestStartupParameterProvider())).thenRespond(new AuthenticationMD5Password(TEST.buffer(4).writeInt(100)))
                .expectRequest(new PasswordMessage("md55e9836cdb369d50e3bc7d127e88b4804")).thenRespond(AuthenticationOk.INSTANCE)
                .done()
            .build();
        // @formatter:on

        GaussDBConnectionConfiguration configuration = GaussDBConnectionConfiguration.builder()
            .applicationName("test-application-name")
            .database("test-database")
            .host("test-host")
            .username("test-username")
            .password("test-password")
            .build();

        assertThat(new GaussDBConnectionFactory(testClientFactory(client, configuration), configuration).getMetadata()).isNotNull();
    }

    private ConnectionFunction testClientFactory(Client client, GaussDBConnectionConfiguration configuration) {
        return new SingleHostConnectionFunction((endpoint, settings) -> Mono.just(client), configuration);
    }

}
