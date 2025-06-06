/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.gaussdb;

import io.r2dbc.gaussdb.util.GaussDBServerExtension;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.R2dbcPermissionDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.publisher.Mono;

import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.GAUSSDB_DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for authentication failures.
 */
final class PostgresqlAuthenticationFailureIntegrationTests {

    @RegisterExtension
    static final GaussDBServerExtension SERVER = new GaussDBServerExtension();

    @Test
    void authExceptionCausedByWrongCredentials() {
        final ConnectionFactory connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(DATABASE, SERVER.getDatabase())
            .option(HOST, SERVER.getHost())
            .option(PORT, SERVER.getPort())
            .option(PASSWORD, "SUPER WRONG PASSWORD")
            .option(USER, SERVER.getUsername())
            .build());

        assertThatExceptionOfType(R2dbcPermissionDeniedException.class)
            .isThrownBy(() -> Mono.from(connectionFactory.create()).block())
            .withMessage("Invalid username/password,login denied.");
    }

}
