/*
 * Copyright 2022 the original author or authors.
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

/**
 * Integration tests for multi-node Postgres server topologies.
 */
final class HighAvailabilityClusterIntegrationTests {
// TODO： Not provide high availability image yet
//    @RegisterExtension
//    static final PostgresqlHighAvailabilityClusterExtension SERVERS = new PostgresqlHighAvailabilityClusterExtension();
//
//    @Test
//    void testPrimaryAndStandbyStartup() {
//        assertThat(SERVERS.getPrimaryJdbcTemplate().queryForObject("SHOW TRANSACTION_READ_ONLY", Boolean.class)).isFalse();
//        assertThat(SERVERS.getStandbyJdbcTemplate().queryForObject("SHOW TRANSACTION_READ_ONLY", Boolean.class)).isTrue();
//    }
//
//    @Test
//    void testMultipleCallsOnSameFactory() {
//        GaussDBConnectionFactory connectionFactory = this.multiHostConnectionFactory(MultiHostConnectionStrategy.TargetServerType.PREFER_SECONDARY, SERVERS.getPrimary(), SERVERS.getStandby());
//
//        Mono.usingWhen(connectionFactory.create(), this::isPrimary, Connection::close)
//            .as(StepVerifier::create)
//            .expectNext(false)
//            .verifyComplete();
//
//        Mono.usingWhen(connectionFactory.create(), this::isPrimary, Connection::close)
//            .as(StepVerifier::create)
//            .expectNext(false)
//            .verifyComplete();
//    }
//
//    @Test
//    void testTargetAnyChooseFirst() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.ANY, SERVERS.getPrimary(), SERVERS.getStandby())
//            .as(StepVerifier::create)
//            .expectNext(true)
//            .verifyComplete();
//
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.ANY, SERVERS.getStandby(), SERVERS.getPrimary())
//            .as(StepVerifier::create)
//            .expectNext(false)
//            .verifyComplete();
//    }
//
//    @Test
//    void testTargetAnyConnectedToPrimary() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.ANY, SERVERS.getPrimary())
//            .as(StepVerifier::create)
//            .expectNext(true)
//            .verifyComplete();
//    }
//
//    @Test
//    void testTargetAnyConnectedToStandby() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.ANY, SERVERS.getStandby())
//            .as(StepVerifier::create)
//            .expectNext(false)
//            .verifyComplete();
//    }
//
//    @Test
//    void testTargetPreferSecondaryChooseStandby() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.PREFER_SECONDARY, SERVERS.getStandby(), SERVERS.getPrimary())
//            .as(StepVerifier::create)
//            .expectNext(false)
//            .verifyComplete();
//
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.PREFER_SECONDARY, SERVERS.getPrimary(), SERVERS.getStandby())
//            .as(StepVerifier::create)
//            .expectNext(false)
//            .verifyComplete();
//    }
//
//    @Test
//    void testTargetPreferSecondaryConnectedToPrimary() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.PREFER_SECONDARY, SERVERS.getPrimary())
//            .as(StepVerifier::create)
//            .expectNext(true)
//            .verifyComplete();
//    }
//
//    @Test
//    void testTargetPreferSecondaryConnectedToStandby() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.PREFER_SECONDARY, SERVERS.getStandby())
//            .as(StepVerifier::create)
//            .expectNext(false)
//            .verifyComplete();
//    }
//
//    @Test
//    void testTargetPrimaryChoosePrimary() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.PRIMARY, SERVERS.getPrimary(), SERVERS.getStandby())
//            .as(StepVerifier::create)
//            .expectNext(true)
//            .verifyComplete();
//
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.PRIMARY, SERVERS.getStandby(), SERVERS.getPrimary())
//            .as(StepVerifier::create)
//            .expectNext(true)
//            .verifyComplete();
//    }
//
//    @Test
//    void testTargetPrimaryConnectedOnPrimary() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.PRIMARY, SERVERS.getPrimary())
//            .as(StepVerifier::create)
//            .expectNext(true)
//            .verifyComplete();
//    }
//
//    @Test
//    void testTargetPrimaryFailedOnStandby() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.PRIMARY, SERVERS.getStandby())
//            .as(StepVerifier::create)
//            .verifyError(R2dbcNonTransientResourceException.class);
//    }
//
//    @Test
//    void testTargetSecondaryChooseStandby() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.SECONDARY, SERVERS.getStandby(), SERVERS.getPrimary())
//            .as(StepVerifier::create)
//            .expectNext(false)
//            .verifyComplete();
//
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.SECONDARY, SERVERS.getPrimary(), SERVERS.getStandby())
//            .as(StepVerifier::create)
//            .expectNext(false)
//            .verifyComplete();
//    }
//
//    @Test
//    void testTargetSecondaryConnectedOnStandby() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.SECONDARY, SERVERS.getStandby())
//            .as(StepVerifier::create)
//            .expectNext(false)
//            .verifyComplete();
//    }
//
//    @Test
//    void testTargetSecondaryFailedOnPrimary() {
//        isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType.SECONDARY, SERVERS.getPrimary())
//            .as(StepVerifier::create)
//            .verifyError(R2dbcException.class);
//    }
//
//    private Mono<Boolean> isConnectedToPrimary(MultiHostConnectionStrategy.TargetServerType targetServerType, PostgreSQLContainer<?>... servers) {
//        GaussDBConnectionFactory connectionFactory = this.multiHostConnectionFactory(targetServerType, servers);
//
//        return Mono.usingWhen(connectionFactory.create(), this::isPrimary, Connection::close);
//    }
//
//    private Mono<Boolean> isPrimary(GaussDBConnection connection) {
//        return connection.createStatement("SHOW TRANSACTION_READ_ONLY")
//            .execute()
//            .flatMap(result -> result.map((row, meta) -> row.get(0, String.class)))
//            .map(str -> str.equalsIgnoreCase("off"))
//            .next();
//    }
//
//    private GaussDBConnectionFactory multiHostConnectionFactory(MultiHostConnectionStrategy.TargetServerType targetServerType, PostgreSQLContainer<?>... servers) {
//        PostgreSQLContainer<?> firstServer = servers[0];
//        GaussDBConnectionConfiguration.Builder builder = GaussDBConnectionConfiguration.builder();
//        for (PostgreSQLContainer<?> server : servers) {
//            builder.addHost(server.getHost(), server.getMappedPort(8000));
//        }
//        GaussDBConnectionConfiguration configuration = builder
//            .targetServerType(targetServerType)
//            .username(firstServer.getUsername())
//            .password(firstServer.getPassword())
//            .build();
//        return new GaussDBConnectionFactory(configuration);
//    }

}
