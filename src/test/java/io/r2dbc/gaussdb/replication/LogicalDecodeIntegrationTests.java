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

package io.r2dbc.gaussdb.replication;

import io.r2dbc.gaussdb.GaussDBConnectionConfiguration;
import io.r2dbc.gaussdb.GaussDBConnectionFactory;
import io.r2dbc.gaussdb.api.GaussDBConnection;
import io.r2dbc.gaussdb.api.GaussDBReplicationConnection;
import io.r2dbc.gaussdb.api.GaussDBResult;
import io.r2dbc.gaussdb.util.GaussDBServerExtension;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for logical decode.
 */
final class LogicalDecodeIntegrationTests {
// TODO：逻辑复制需要使用不一样的端口，并且需要配置数据库用户及其权限。暂时不测试这个场景。
//    @RegisterExtension
//    static final GaussDBServerExtension SERVER = new GaussDBServerExtension();
//
//    private final GaussDBConnectionFactory connectionFactory = new GaussDBConnectionFactory(GaussDBConnectionConfiguration.builder()
//        .database(SERVER.getDatabase())
//        .host(SERVER.getHost())
//        .port(SERVER.getPort())
//        .password(SERVER.getPassword())
//        .username(SERVER.getUsername())
//        .forceBinary(true)
//        .build());
//
//    @Test
//    void shouldCreateReplicationSlot() {
//
//        GaussDBReplicationConnection replicationConnection = this.connectionFactory.replication().block();
//
//        ReplicationSlotRequest request = ReplicationSlotRequest.logical().slotName("rs" + UUID.randomUUID().toString().replace("-", "")).outputPlugin("test_decoding").temporary().build();
//        replicationConnection.createSlot(request).as(StepVerifier::create).consumeNextWith(actual -> {
//
//            assertThat(actual.getSlotName()).isEqualTo(request.getSlotName());
//            assertThat(actual.getOutputPlugin()).isEqualTo("test_decoding");
//            assertThat(actual.getConsistentPoint()).isNotNull();
//            assertThat(actual.getSnapshotName()).isNotNull();
//
//        }).verifyComplete();
//    }
//
//    @Test
//    void shouldStartReplication() {
//
//        GaussDBReplicationConnection replicationConnection = this.connectionFactory.replication().block();
//        ReplicationSlotRequest request = createSlot(replicationConnection);
//
//        ReplicationRequest replicationRequest = ReplicationRequest.logical().slotName(request.getSlotName()).startPosition(LogSequenceNumber.valueOf(0)).slotOption("skip-empty-xacts", true)
//            .slotOption("include-xids", false).build();
//
//        ReplicationStream replicationStream = replicationConnection.startReplication(replicationRequest).block(Duration.ofSeconds(10));
//
//        assertThat(replicationStream.isClosed()).isFalse();
//        assertThat(replicationStream.getLastReceiveLSN()).isNotNull();
//        assertThat(replicationStream.getLastAppliedLSN()).isNotNull();
//        assertThat(replicationStream.getLastFlushedLSN()).isNotNull();
//
//        replicationStream.close().as(StepVerifier::create).verifyComplete();
//
//        assertThat(replicationStream.isClosed()).isTrue();
//    }
//
//    @Test
//    void shouldReceiveReplication() {
//
//        GaussDBReplicationConnection replicationConnection = this.connectionFactory.replication().block();
//        GaussDBConnection connection = this.connectionFactory.create().block();
//
//        prepare(connection);
//
//        ReplicationSlotRequest request = createSlot(replicationConnection);
//
//        ReplicationRequest replicationRequest = ReplicationRequest.logical().slotName(request.getSlotName()).startPosition(LogSequenceNumber.valueOf(0)).slotOption("skip-empty-xacts", true)
//            .slotOption("include-xids", false).build();
//
//        ReplicationStream replicationStream = replicationConnection.startReplication(replicationRequest).block(Duration.ofSeconds(10));
//
//        connection.createStatement("INSERT INTO logical_decode_test VALUES('Hello World')").execute().flatMap(GaussDBResult::getRowsUpdated).as(StepVerifier::create).expectNext(1L).verifyComplete();
//
//        replicationStream.map(byteBuf -> byteBuf.toString(StandardCharsets.UTF_8))
//            .as(StepVerifier::create)
//            .expectNext("BEGIN")
//            .expectNext("table public.logical_decode_test: INSERT: first_name[character varying]:'Hello World'")
//            .expectNext("COMMIT")
//            .then(() -> replicationStream.close().subscribe())
//            .verifyComplete();
//
//        assertThat(replicationStream.isClosed()).isTrue();
//        replicationStream.close().as(StepVerifier::create).verifyComplete();
//        assertThat(replicationStream.isClosed()).isTrue();
//    }
//
//    @Test
//    void replicationShouldFailWithWrongSlotType() {
//
//        GaussDBReplicationConnection replicationConnection = this.connectionFactory.replication().block();
//        GaussDBConnection connection = this.connectionFactory.create().block();
//
//        prepare(connection);
//
//        ReplicationSlotRequest request = createSlot(replicationConnection);
//
//        ReplicationRequest replicationRequest = ReplicationRequest.physical().slotName(request.getSlotName()).startPosition(LogSequenceNumber.valueOf(0)).build();
//
//        replicationConnection
//            .startReplication(replicationRequest)
//            .as(StepVerifier::create)
//            .verifyError(R2dbcNonTransientResourceException.class);
//    }
//
//    private void prepare(GaussDBConnection connection) {
//        connection.createStatement("DROP TABLE IF EXISTS logical_decode_test").execute().flatMap(GaussDBResult::getRowsUpdated).as(StepVerifier::create).verifyComplete();
//
//        connection.createStatement("CREATE TABLE logical_decode_test (first_name varchar(255))").execute().flatMap(GaussDBResult::getRowsUpdated).as(StepVerifier::create).verifyComplete();
//    }
//
//    private ReplicationSlotRequest createSlot(GaussDBReplicationConnection replicationConnection) {
//        ReplicationSlotRequest request = ReplicationSlotRequest.logical().slotName("RS" + UUID.randomUUID().toString().replace("-", "")).outputPlugin("test_decoding").temporary().build();
//        replicationConnection.createSlot(request).as(StepVerifier::create).expectNextCount(1).verifyComplete();
//        return request;
//    }

}
