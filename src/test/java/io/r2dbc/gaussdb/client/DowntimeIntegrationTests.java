/*
 * Copyright 2023 the original author or authors.
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

package io.r2dbc.gaussdb.client;

import io.r2dbc.gaussdb.GaussDBConnectionConfiguration;
import io.r2dbc.gaussdb.GaussDBConnectionFactory;
import io.r2dbc.gaussdb.api.GaussDBException;
import org.junit.jupiter.api.Test;
import reactor.netty.DisposableChannel;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;
import reactor.test.StepVerifier;

import java.nio.channels.ClosedChannelException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class DowntimeIntegrationTests {

    @Test
    void failSslHandshakeIfInboundClosed() {
        verifyError(SSLMode.REQUIRE, error ->
            assertThat(error)
                .isInstanceOf(AbstractPostgresSSLHandlerAdapter.GaussDBSslException.class)
                .hasMessage("Connection closed during SSL negotiation"));
    }

    @Test
    void failSslTunnelIfInboundClosed() {
        verifyError(SSLMode.TUNNEL, error -> {
            assertThat(error)
                .isInstanceOf(GaussDBException.class)
                .cause()
                .isInstanceOf(ClosedChannelException.class);

            assertThat(error.getCause().getSuppressed()).hasSize(1);

            assertThat(error.getCause().getSuppressed()[0])
                .hasMessage("Connection closed while SSL/TLS handshake was in progress");
        });
    }

    // Simulate server downtime, where connections are accepted and then closed immediately
    static DisposableServer newServer() {
        return TcpServer.create()
            .doOnConnection(DisposableChannel::dispose)
            .bindNow();
    }

    static GaussDBConnectionFactory newConnectionFactory(DisposableServer server, SSLMode sslMode) {
        return new GaussDBConnectionFactory(
            GaussDBConnectionConfiguration.builder()
                .host(server.host())
                .port(server.port())
                .username("test")
                .sslMode(sslMode)
                .build());
    }

    static void verifyError(SSLMode sslMode, Consumer<Throwable> assertions) {
        DisposableServer server = newServer();
        GaussDBConnectionFactory connectionFactory = newConnectionFactory(server, sslMode);
        try {
            connectionFactory.create().as(StepVerifier::create).verifyErrorSatisfies(assertions);
        } finally {
            server.disposeNow();
        }
    }

}
