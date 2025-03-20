/*
 * Copyright 2019 the original author or authors.
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

import io.r2dbc.gaussdb.api.GaussDBConnection;
import io.r2dbc.gaussdb.util.GaussDBServerExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.test.StepVerifier;

import java.util.function.Consumer;

/**
 * Support class for integration tests using {@link GaussDBConnection}.
 */
public abstract class AbstractIntegrationTests {

    @RegisterExtension
    public static final GaussDBServerExtension SERVER = new GaussDBServerExtension();

    public GaussDBConnectionFactory connectionFactory;

    public GaussDBConnection connection;

    /**
     * Entry-point to obtain a {@link GaussDBConnectionFactory}.
     *
     * @return a {@link GaussDBConnectionFactory}.
     */
    protected GaussDBConnectionFactory getConnectionFactory() {
        return getConnectionFactory(this::customize);
    }

    /**
     * Entry-point to obtain a {@link GaussDBConnectionFactory}.
     *
     * @return a {@link GaussDBConnectionFactory}.
     */
    protected GaussDBConnectionFactory getConnectionFactory(Consumer<GaussDBConnectionConfiguration.Builder> customizer) {

        GaussDBConnectionConfiguration.Builder builder = GaussDBConnectionConfiguration.builder()
            .database(SERVER.getDatabase())
            .host(SERVER.getHost())
            .port(SERVER.getPort())
            .password(SERVER.getPassword())
            .username(SERVER.getUsername());

        customizer.accept(builder);
        return new GaussDBConnectionFactory(builder.build());
    }

    /**
     * Template method to customize {@link GaussDBConnectionConfiguration.Builder}.
     *
     * @param builder builder to customize.
     */
    protected void customize(GaussDBConnectionConfiguration.Builder builder) {

    }

    @BeforeEach
    void setUp() {
        this.connectionFactory = getConnectionFactory();
        this.connection = this.connectionFactory.create().block();
    }

    @AfterEach
    void tearDown() {
        if (this.connection != null) {
            this.connection.close().as(StepVerifier::create).verifyComplete();
        }
    }

}
