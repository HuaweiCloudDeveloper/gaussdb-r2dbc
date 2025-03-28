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

import io.r2dbc.gaussdb.client.SSLMode;
import org.junit.jupiter.api.Test;
import reactor.netty.resources.LoopResources;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link GaussDBConnectionConfiguration}.
 */
final class GaussDBConnectionConfigurationUnitTests {

    @Test
    void builderNoApplicationName() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder().applicationName(null))
            .withMessage("applicationName must not be null");
    }

    @Test
    void builderNoHostConfiguration() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder().build())
            .withMessage("Connection must be configured for either multi-host or single host connectivity");
    }

    @Test
    void builderHostAndSocket() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder().host("host").socket("socket").build())
            .withMessageContaining("Connection must be configured for either host/port or socket usage but not both");
    }

    @Test
    void builderNoUsername() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder().username((String) null))
            .withMessage("username must not be null");
    }

    @Test
    void builderNegativeFetchSize() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder().fetchSize(-1))
            .withMessage("fetch size must be greater or equal zero");
    }

    @Test
    void builderNoTcpLoopResources() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder().loopResources(null))
            .withMessage("loopResources must not be null");
    }

    @Test
    void configuration() {
        Map<String, String> options = new HashMap<>();
        options.put("lock_timeout", "10s");
        options.put("statement_timeout", "60000"); // [ms]
        LoopResources loopResources = mock(LoopResources.class);

        GaussDBConnectionConfiguration configuration = getPostgresqlConnectionConfiguration(options, loopResources).build();

        assertThat(configuration)
            .hasFieldOrPropertyWithValue("applicationName", "test-application-name")
            .hasFieldOrPropertyWithValue("connectTimeout", Duration.ofMillis(1000))
            .hasFieldOrPropertyWithValue("database", "test-database")
            .hasFieldOrPropertyWithValue("singleHostConfiguration.host", "test-host")
            .hasFieldOrProperty("options")
            .hasFieldOrPropertyWithValue("singleHostConfiguration.port", 100)
            .hasFieldOrProperty("sslConfig")
            .hasFieldOrPropertyWithValue("tcpKeepAlive", true)
            .hasFieldOrPropertyWithValue("tcpNoDelay", false)
            .hasFieldOrPropertyWithValue("loopResources", loopResources);

        assertThat(configuration.getOptions())
            .containsEntry("lock_timeout", "10s")
            .containsEntry("statement_timeout", "60000")
            .containsEntry("search_path", "test-schema");
    }

    @Test
    void configureStatementAndLockTimeouts() {
        Map<String, String> options = new HashMap<>();
        options.put("lock_timeout", "10s");
        options.put("statement_timeout", "60000"); // [ms]
        LoopResources loopResources = mock(LoopResources.class);

        GaussDBConnectionConfiguration configuration = getPostgresqlConnectionConfiguration(options, loopResources)
            .statementTimeout(Duration.ofMillis(5000))
            .lockWaitTimeout(Duration.ofSeconds(50))
            .build();

        assertThat(configuration)
            .hasFieldOrPropertyWithValue("applicationName", "test-application-name")
            .hasFieldOrPropertyWithValue("connectTimeout", Duration.ofMillis(1000))
            .hasFieldOrPropertyWithValue("database", "test-database")
            .hasFieldOrPropertyWithValue("singleHostConfiguration.host", "test-host")
            .hasFieldOrProperty("options")
            .hasFieldOrPropertyWithValue("singleHostConfiguration.port", 100)
            .hasFieldOrProperty("sslConfig")
            .hasFieldOrPropertyWithValue("tcpKeepAlive", true)
            .hasFieldOrPropertyWithValue("tcpNoDelay", false)
            .hasFieldOrPropertyWithValue("loopResources", loopResources);

        assertThat(configuration.getOptions())
            .containsEntry("lock_timeout", "50000")
            .containsEntry("statement_timeout", "5000")
            .containsEntry("search_path", "test-schema");
    }

    private GaussDBConnectionConfiguration.Builder getPostgresqlConnectionConfiguration(Map<String, String> options, LoopResources loopResources) {
        return GaussDBConnectionConfiguration.builder()
            .applicationName("test-application-name")
            .connectTimeout(Duration.ofMillis(1000))
            .database("test-database")
            .host("test-host")
            .options(options)
            .port(100)
            .schema("test-schema")
            .username("test-username")
            .sslMode(SSLMode.ALLOW)
            .tcpKeepAlive(true)
            .tcpNoDelay(false)
            .loopResources(loopResources);
    }

    @Test
    void configurationDefaults() {
        GaussDBConnectionConfiguration configuration = GaussDBConnectionConfiguration.builder()
            .database("test-database")
            .host("test-host")
            .password("test-password")
            .username("test-username")
            .schema("test-schema")
            .build();

        assertThat(configuration)
            .hasFieldOrPropertyWithValue("applicationName", "r2dbc-gaussdb")
            .hasFieldOrPropertyWithValue("database", "test-database")
            .hasFieldOrPropertyWithValue("singleHostConfiguration.host", "test-host")
            .hasFieldOrPropertyWithValue("singleHostConfiguration.port", 8000)
            .hasFieldOrProperty("options")
            .hasFieldOrProperty("sslConfig")
            .hasFieldOrPropertyWithValue("tcpKeepAlive", false)
            .hasFieldOrPropertyWithValue("tcpNoDelay", true)
            .hasFieldOrPropertyWithValue("loopResources", null);

        assertThat(configuration.getOptions())
            .containsEntry("search_path", "test-schema");
    }

    @Test
    void constructorNoUsername() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .password("test-password")
            .build())
            .withMessage("username must not be null");
    }

    @Test
    void constructorNoPassword() {
        GaussDBConnectionConfiguration configuration = GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .username("foo")
            .build();

        assertThat(configuration.getPassword()).isNotNull();
    }

    @Test
    void constructorInvalidOptions() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .username("test-username")
            .password("test-password")
            .options(null)
            .build())
            .withMessage("options map must not be null");

        assertThatIllegalArgumentException().isThrownBy(() -> {
            final Map<String, String> options = new HashMap<>();
            options.put(null, "test-value");
            GaussDBConnectionConfiguration.builder()
                .host("test-host")
                .username("test-username")
                .password("test-password")
                .options(options)
                .build();
        })
            .withMessage("option keys must not be null");

        assertThatIllegalArgumentException().isThrownBy(() -> {
            final Map<String, String> options = new HashMap<>();
            options.put("test-option", null);
            GaussDBConnectionConfiguration.builder()
                .host("test-host")
                .username("test-username")
                .password("test-password")
                .options(options)
                .build();
        })
            .withMessage("option values must not be null");
    }

    @Test
    void constructorNoSslCustomizer() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder()
            .sslContextBuilderCustomizer(null)
            .build())
            .withMessage("sslContextBuilderCustomizer must not be null");
    }

    @Test
    void constructorNoSslCert() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .password("test-password")
            .sslCert((String) null)
            .build())
            .withMessage("sslCert must not be null and must exist");
    }

    @Test
    void constructorNotExistSslCert() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .username("test-username")
            .password("test-password")
            .sslCert("no-client.crt")
            .build())
            .withMessage("sslCert must not be null and must exist");
    }

    @Test
    void constructorSslCert() {
        GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .username("test-username")
            .password("test-password")
            .sslCert("client.crt")
            .build();
    }

    @Test
    void constructorNoSslKey() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .password("test-password")
            .sslKey((String) null)
            .build())
            .withMessage("sslKey must not be null and must exist");
    }

    @Test
    void constructorNotExistSslKey() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .username("test-username")
            .password("test-password")
            .sslKey("no-client.key")
            .build())
            .withMessage("sslKey must not be null and must exist");
    }

    @Test
    void constructorSslKey() {
        GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .username("test-username")
            .password("test-password")
            .sslKey("client.key")
            .build();
    }

    @Test
    void constructorNullSslRootCert() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .password("test-password")
            .sslRootCert((String) null)
            .build())
            .withMessage("sslRootCert must not be null and must exist");
    }

    @Test
    void constructorNotExistSslRootCert() {
        assertThatIllegalArgumentException().isThrownBy(() -> GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .password("test-password")
            .sslRootCert("no-server.crt")
            .build())
            .withMessage("sslRootCert must not be null and must exist");
    }

    @Test
    void constructorSslRootCert() {
        GaussDBConnectionConfiguration.builder()
            .host("test-host")
            .username("test-username")
            .password("test-password")
            .sslRootCert("client.crt")
            .build();
    }

}
