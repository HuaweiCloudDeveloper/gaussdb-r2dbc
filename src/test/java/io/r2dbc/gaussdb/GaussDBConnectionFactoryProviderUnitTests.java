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

import io.r2dbc.gaussdb.client.MultiHostConfiguration;
import io.r2dbc.gaussdb.client.MultiHostConfiguration.ServerHost;
import io.r2dbc.gaussdb.client.SSLConfig;
import io.r2dbc.gaussdb.client.SSLMode;
import io.r2dbc.gaussdb.extension.Extension;
import io.r2dbc.gaussdb.util.LogLevel;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.net.ssl.SSLParameters;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Supplier;

import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.AUTODETECT_EXTENSIONS;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.COMPATIBILITY_MODE;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.ERROR_RESPONSE_LOG_LEVEL;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.EXTENSIONS;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.FAILOVER_PROTOCOL;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.FETCH_SIZE;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.FORCE_BINARY;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.GAUSSDB_DRIVER;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.HOST_RECHECK_TIME;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.LOAD_BALANCE_HOSTS;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.LOCK_WAIT_TIMEOUT;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.OPTIONS;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.PREFER_ATTACHED_BUFFERS;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.PREPARED_STATEMENT_CACHE_QUERIES;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.SOCKET;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.SSL_CERT;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.SSL_CONTEXT_BUILDER_CUSTOMIZER;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.SSL_KEY;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.SSL_MODE;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.SSL_ROOT_CERT;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.SSL_SNI;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.STATEMENT_TIMEOUT;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.TARGET_SERVER_TYPE;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.TCP_KEEPALIVE;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.TCP_NODELAY;
import static io.r2dbc.gaussdb.GaussDBConnectionFactoryProvider.TIME_ZONE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL;
import static io.r2dbc.spi.ConnectionFactoryOptions.SSL;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;
import static io.r2dbc.spi.ConnectionFactoryOptions.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GaussDBConnectionFactoryProvider}.
 */
final class GaussDBConnectionFactoryProviderUnitTests {

    private final GaussDBConnectionFactoryProvider provider = new GaussDBConnectionFactoryProvider();

    @Test
    void doesNotSupportWithWrongDriver() {
        assertThat(this.provider.supports(ConnectionFactoryOptions.builder()
            .option(DRIVER, "test-driver")
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .build())).isFalse();
    }

    @Test
    void doesNotSupportWithoutDriver() {
        assertThat(this.provider.supports(ConnectionFactoryOptions.builder()
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .build())).isFalse();
    }

    @Test
    void createFailsWithoutHost() {
        assertThatThrownBy(() -> this.provider.create(ConnectionFactoryOptions.builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .build())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void supportsWithoutHost() {
        assertThat(this.provider.supports(ConnectionFactoryOptions.builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .build())).isTrue();
    }

    @Test
    void supportsWithoutPassword() {
        assertThat(this.provider.supports(ConnectionFactoryOptions.builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(USER, "test-user")
            .build())).isTrue();
    }

    @Test
    void returnsDriverIdentifier() {
        assertThat(this.provider.getDriver()).isEqualTo(GAUSSDB_DRIVER);
    }

    @Test
    void supports() {
        assertThat(this.provider.supports(ConnectionFactoryOptions.builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .build())).isTrue();
    }

    @Test
    void supportsWithoutUser() {
        assertThat(this.provider.supports(ConnectionFactoryOptions.builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .build())).isTrue();
    }

    @Test
    void disableSsl() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(SSL, false)
            .build());

        SSLConfig sslConfig = factory.getConfiguration().getSslConfig();

        assertThat(sslConfig.getSslMode()).isEqualTo(SSLMode.DISABLE);
    }

    @Test
    void enableSsl() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(SSL, true)
            .build());

        SSLConfig sslConfig = factory.getConfiguration().getSslConfig();

        assertThat(sslConfig.getSslMode()).isEqualTo(SSLMode.VERIFY_FULL);
    }

    @Test
    void supportsSslCertificates() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(SSL, true)
            .option(SSL_KEY, "client.key")
            .option(SSL_CERT, "client.crt")
            .option(SSL_ROOT_CERT, "server.crt")
            .build());

        assertThat(factory).isNotNull();
    }

    @Test
    void supportsSslCertificatesByClasspath() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(SSL, true)
            .option(SSL_KEY, "client.key")
            .option(SSL_CERT, "client.crt")
            .option(SSL_ROOT_CERT, "server.crt")
            .build());

        assertThat(factory).isNotNull();
    }

    @Test
    void supportsSslMode() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(Option.valueOf("sslMode"), "DISABLE")
            .build());

        SSLConfig sslConfig = factory.getConfiguration().getSslConfig();

        assertThat(sslConfig.getSslMode()).isEqualTo(SSLMode.DISABLE);
    }

    @Test
    void supportsSslModeAlias() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(Option.valueOf("sslmode"), "require")
            .build());

        SSLConfig sslConfig = factory.getConfiguration().getSslConfig();

        assertThat(sslConfig.getSslMode()).isEqualTo(SSLMode.REQUIRE);
    }

    @Test
    void shouldCreateConnectionFactoryWithoutPassword() {
        assertThat(this.provider.create(ConnectionFactoryOptions.builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(USER, "test-user")
            .build())).isNotNull();
    }

    @Test
    void providerShouldConsiderFetchSize() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(FETCH_SIZE, 100)
            .build());

        assertThat(factory.getConfiguration().getFetchSize().applyAsInt("")).isEqualTo(100);
    }

    @Test
    void providerShouldConsiderFetchSizeAsString() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(Option.valueOf("fetchSize"), "100")
            .build());

        assertThat(factory.getConfiguration().getFetchSize().applyAsInt("")).isEqualTo(100);
    }

    @Test
    void providerShouldConsiderBinaryTransfer() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(FORCE_BINARY, true)
            .build());

        assertThat(factory.getConfiguration().isForceBinary()).isTrue();
    }

    @Test
    void providerShouldConsiderBinaryTransferWhenProvidedAsString() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(Option.valueOf("forceBinary"), "true")
            .build());

        assertThat(factory.getConfiguration().isForceBinary()).isTrue();
    }

    @Test
    void providerShouldConsiderCompatibilityMode() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(COMPATIBILITY_MODE, true)
            .build());

        assertThat(factory.getConfiguration().isCompatibilityMode()).isTrue();
    }

    @Test
    void providerShouldConsiderPreparedStatementCacheQueries() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(PREPARED_STATEMENT_CACHE_QUERIES, -2)
            .build());

        assertThat(factory.getConfiguration().getPreparedStatementCacheQueries()).isEqualTo(-2);
    }

    @Test
    void providerShouldConsiderPreparedStatementCacheQueriesWhenProvidedAsString() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(Option.valueOf("preparedStatementCacheQueries"), "5")
            .build());

        assertThat(factory.getConfiguration().getPreparedStatementCacheQueries()).isEqualTo(5);
    }

    @Test
    void providerShouldParseAndHandleConnectionParameters() {
        Map<String, String> expectedOptions = new HashMap<>();
        expectedOptions.put("lock_timeout", "5s");
        expectedOptions.put("statement_timeout", "6000");
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(OPTIONS, expectedOptions)
            .build());

        Map<String, String> actualOptions = factory.getConfiguration().getOptions();

        assertThat(actualOptions).isNotNull();
        assertThat(actualOptions).isEqualTo(expectedOptions);
    }

    @Test
    void providerShouldParseAndHandleLockStatementTimeouts() {
        Map<String, String> expectedOptions = new HashMap<>();
        expectedOptions.put("lock_timeout", "5000");
        expectedOptions.put("statement_timeout", "6000");
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(LOCK_WAIT_TIMEOUT, Duration.ofSeconds(5))
            .option(STATEMENT_TIMEOUT, Duration.ofSeconds(6))
            .build());

        Map<String, String> actualOptions = factory.getConfiguration().getOptions();

        assertThat(actualOptions).isNotNull();
        assertThat(actualOptions).isEqualTo(expectedOptions);
    }

    @Test
    void shouldConfigureAutodetectExtensions() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(AUTODETECT_EXTENSIONS, true)
            .build());

        assertThat(factory.getConfiguration().isAutodetectExtensions()).isTrue();

        factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(AUTODETECT_EXTENSIONS, false)
            .build());

        assertThat(factory.getConfiguration().isAutodetectExtensions()).isFalse();
    }

    @Test
    void shouldConfigureLogLevels() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(ERROR_RESPONSE_LOG_LEVEL, LogLevel.OFF)
            .option(Option.valueOf("noticeLogLevel"), "WARN")
            .build());

        assertThat(factory.getConfiguration())
            .hasFieldOrPropertyWithValue("errorResponseLogLevel", LogLevel.OFF)
            .hasFieldOrPropertyWithValue("noticeLogLevel", LogLevel.WARN);
    }

    @Test
    void shouldApplySslContextBuilderCustomizer() {

        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(SSL_MODE, SSLMode.ALLOW)
            .option(SSL_CONTEXT_BUILDER_CUSTOMIZER, sslContextBuilder -> {
                throw new IllegalStateException("Works!");
            })
            .build());

        assertThatIllegalStateException().isThrownBy(() -> factory.getConfiguration().getSslConfig().getSslProvider().get()).withMessageContaining("Works!");

        SSLParameters sslParameters = factory.getConfiguration().getSslConfig().getSslParametersFactory().apply(InetSocketAddress.createUnresolved("myhost", 1));

        assertThat(sslParameters.getServerNames()).hasSize(1);
    }

    @Test
    void shouldApplySslSni() {

        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(SSL_MODE, SSLMode.ALLOW)
            .option(SSL_SNI, false)
            .build());

        SSLParameters sslParameters = factory.getConfiguration().getSslConfig().getSslParametersFactory().apply(InetSocketAddress.createUnresolved("myhost", 1));

        assertThat(sslParameters.getServerNames()).isNull();
    }

    @Test
    void shouldConfigureTcpKeepAlive() {

        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(TCP_KEEPALIVE, true)
            .build());

        assertThat(factory.getConfiguration().isTcpKeepAlive()).isTrue();
    }

    @Test
    void shouldConfigureTcpNoDelay() {

        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(TCP_NODELAY, true)
            .build());

        assertThat(factory.getConfiguration().isTcpNoDelay()).isTrue();
    }

    @Test
    void shouldConfigureTimeZone() {

        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(TIME_ZONE, TimeZone.getTimeZone("Europe/Amsterdam"))
            .build());

        assertThat(factory.getConfiguration().getTimeZone()).isEqualTo(TimeZone.getTimeZone("Europe/Amsterdam"));
    }

    @Test
    void shouldConfigureTimeZoneAsString() {

        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(Option.valueOf("timeZone"), "Europe/Amsterdam")
            .build());

        assertThat(factory.getConfiguration().getTimeZone()).isEqualTo(TimeZone.getTimeZone("Europe/Amsterdam"));
    }

    @Test
    void shouldConnectUsingUnixDomainSocket() {

        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(SOCKET, "/tmp/.s.PGSQL.8000")
            .option(USER, "postgres")
            .build());

        assertThat(factory.getConfiguration().getSingleHostConfiguration().isUseSocket()).isTrue();
        assertThat(factory.getConfiguration().getSingleHostConfiguration().getRequiredSocket()).isEqualTo("/tmp/.s.PGSQL.8000");
    }

    @Test
    void shouldConnectUsingMultiHostConfiguration() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(PROTOCOL, FAILOVER_PROTOCOL)
            .option(HOST, "host1:8001,host2:8000,host3")
            .option(USER, "postgres")
            .option(LOAD_BALANCE_HOSTS, true)
            .option(HOST_RECHECK_TIME, Duration.ofMillis(20000))
            .option(TARGET_SERVER_TYPE, MultiHostConnectionStrategy.TargetServerType.SECONDARY)
            .build());

        assertThat(factory.getConfiguration().getSingleHostConfiguration()).isNull();
        assertThat(factory.getConfiguration().getMultiHostConfiguration().isLoadBalanceHosts()).isEqualTo(true);
        assertThat(factory.getConfiguration().getMultiHostConfiguration().getHostRecheckTime()).isEqualTo(Duration.ofMillis(20000));
        assertThat(factory.getConfiguration().getMultiHostConfiguration().getTargetServerType()).isEqualTo(MultiHostConnectionStrategy.TargetServerType.SECONDARY);
        List<ServerHost> hosts = factory.getConfiguration().getMultiHostConfiguration().getHosts();
        assertThat(hosts).hasSize(3);
        assertThat(hosts.get(0)).usingRecursiveComparison().isEqualTo(new ServerHost("host1", 8001));
        assertThat(hosts.get(1)).usingRecursiveComparison().isEqualTo(new ServerHost("host2", 8000));
        assertThat(hosts.get(2)).usingRecursiveComparison().isEqualTo(new ServerHost("host3", 8000));
    }

    @Test
    void shouldConnectUsingMultiHostConfigurationFromUrl() {
        GaussDBConnectionFactory factory = this.provider.create(ConnectionFactoryOptions.parse("r2dbc:gaussdb:failover://user:foo@host1:8001,host2:8000,host3" +
            "?loadBalanceHosts=true&hostRecheckTime=20s&targetServerType=SECONdArY"));

        assertThat(factory.getConfiguration().getSingleHostConfiguration()).isNull();
        MultiHostConfiguration config = factory.getConfiguration().getRequiredMultiHostConfiguration();

        assertThat(config.isLoadBalanceHosts()).isEqualTo(true);
        assertThat(config.getHostRecheckTime()).isEqualTo(Duration.ofMillis(20000));
        assertThat(config.getTargetServerType()).isEqualTo(MultiHostConnectionStrategy.TargetServerType.SECONDARY);

        List<ServerHost> hosts = config.getHosts();
        assertThat(hosts).hasSize(3).containsExactly(new ServerHost("host1", 8001), new ServerHost("host2", 8000), new ServerHost("host3", 8000));
    }

    @Test
    void shouldParseOptionsProvidedAsString() {
        Option<String> options = Option.valueOf("options");
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(options, "search_path=public,private;default_tablespace=unknown")
            .build());

        assertThat(factory.getConfiguration().getOptions().get("search_path")).isEqualTo("public,private");
        assertThat(factory.getConfiguration().getOptions().get("default_tablespace")).isEqualTo("unknown");
    }

    @Test
    void shouldParseOptionsProvidedAsMap() {
        Option<Map<String, String>> options = Option.valueOf("options");

        Map<String, String> optionsMap = new HashMap<>();
        optionsMap.put("search_path", "public,private");
        optionsMap.put("default_tablespace", "unknown");

        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(options, optionsMap)
            .build());

        assertThat(factory.getConfiguration().getOptions().get("search_path")).isEqualTo("public,private");
        assertThat(factory.getConfiguration().getOptions().get("default_tablespace")).isEqualTo("unknown");
    }

    @Test
    void shouldConfigurePreferAttachedBuffers() {

        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(PREFER_ATTACHED_BUFFERS, true)
            .build());

        assertThat(factory.getConfiguration().isPreferAttachedBuffers()).isTrue();
    }

    @Test
    void shouldConfigureExtensions() {
        TestExtension testExtension1 = new TestExtension("extension-1");
        TestExtension testExtension2 = new TestExtension("extension-2");
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(PASSWORD, "test-password")
            .option(USER, "test-user")
            .option(EXTENSIONS, Arrays.asList(testExtension1, testExtension2))
            .build());

        assertThat(factory.getConfiguration().getExtensions()).containsExactly(testExtension1, testExtension2);
    }

    @Test
    void supportsUsernameAndPasswordSupplier() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(Option.valueOf("password"), (Supplier<String>) () -> "test-password")
            .option(Option.valueOf("user"), (Supplier<String>) () -> "test-user")
            .option(USER, "test-user")
            .build());

        StepVerifier.create(factory.getConfiguration().getPassword()).expectNext("test-password").verifyComplete();
        StepVerifier.create(factory.getConfiguration().getUsername()).expectNext("test-user").verifyComplete();
    }

    @Test
    void supportsUsernameAndPasswordPublisher() {
        GaussDBConnectionFactory factory = this.provider.create(builder()
            .option(DRIVER, GAUSSDB_DRIVER)
            .option(HOST, "test-host")
            .option(Option.valueOf("password"), Mono.just("test-password"))
            .option(Option.valueOf("user"), Mono.just("test-user"))
            .option(USER, "test-user")
            .build());

        StepVerifier.create(factory.getConfiguration().getPassword()).expectNext("test-password").verifyComplete();
        StepVerifier.create(factory.getConfiguration().getUsername()).expectNext("test-user").verifyComplete();
    }

    private static class TestExtension implements Extension {

        private final String name;

        private TestExtension(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestExtension that = (TestExtension) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

    }

}
