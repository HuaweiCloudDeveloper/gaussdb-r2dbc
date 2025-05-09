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

import io.r2dbc.gaussdb.api.GaussDBConnectionMetadata;
import io.r2dbc.gaussdb.api.GaussDBReplicationConnection;
import io.r2dbc.gaussdb.client.Client;
import io.r2dbc.gaussdb.message.backend.BackendMessage;
import io.r2dbc.gaussdb.message.backend.EmptyQueryResponse;
import io.r2dbc.gaussdb.message.backend.ErrorResponse;
import io.r2dbc.gaussdb.message.backend.ReadyForQuery;
import io.r2dbc.gaussdb.message.frontend.FrontendMessage;
import io.r2dbc.gaussdb.message.frontend.Query;
import io.r2dbc.gaussdb.replication.LogSequenceNumber;
import io.r2dbc.gaussdb.replication.ReplicationRequest;
import io.r2dbc.gaussdb.replication.ReplicationSlot;
import io.r2dbc.gaussdb.replication.ReplicationSlotRequest;
import io.r2dbc.gaussdb.replication.ReplicationStream;
import io.r2dbc.gaussdb.util.Assert;
import io.r2dbc.spi.Row;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.function.Predicate;

import static io.r2dbc.gaussdb.util.PredicateUtils.or;

/**
 * GaussDB replication connection.
 */
final class DefaultGaussDBReplicationConnection implements GaussDBReplicationConnection {

    private static final Predicate<BackendMessage> WINDOW_UNTIL = or(ReadyForQuery.class::isInstance, EmptyQueryResponse.class::isInstance, ErrorResponse.class::isInstance);

    private final GaussDBConnection delegate;

    private final Client client;

    DefaultGaussDBReplicationConnection(GaussDBConnection delegate) {
        this.delegate = delegate;
        this.client = delegate.getClient();
    }

    @Override
    public Mono<Void> close() {
        return this.delegate.close();
    }

    @Override
    public Mono<ReplicationSlot> createSlot(ReplicationSlotRequest request) {

        Assert.requireNonNull(request, "request must not be null");

        return this.delegate.createStatement(request.asSQL()).execute().flatMap(it -> {

            return it.map((row, rowMetadata) -> getReplicationSlot(request, row));
        }).last();
    }

    private static ReplicationSlot getReplicationSlot(ReplicationSlotRequest request, Row row) {
        return new ReplicationSlot(
            getString(row, "slot_name"),
            request.getReplicationType(),
            LogSequenceNumber.valueOf(getString(row, "consistent_point")),
            row.get("snapshot_name", String.class),
            row.get("output_plugin", String.class));
    }

    @Override
    public Mono<ReplicationStream> startReplication(ReplicationRequest request) {

        Assert.requireNonNull(request, "request must not be null");

        String sql = request.asSQL();
        ExceptionFactory exceptionFactory = ExceptionFactory.withSql(sql);

        Sinks.Many<FrontendMessage> requestSink = Sinks.many().unicast().onBackpressureBuffer();

        return Mono.fromDirect(this.client.exchange(requestSink.asFlux())
            .handle(exceptionFactory::handleErrorResponse)
            .windowUntil(WINDOW_UNTIL)
            .map(messages -> {
                return (ReplicationStream) new GaussDBReplicationStream(this.client.getByteBufAllocator(), request, requestSink, messages);
            })).doOnSubscribe(it -> {
            requestSink.emitNext(new Query(sql), Sinks.EmitFailureHandler.FAIL_FAST);
        });
    }

    @Override
    public GaussDBConnectionMetadata getMetadata() {
        return this.delegate.getMetadata();
    }

    private static String getString(Row row, String column) {
        String value = row.get(column, String.class);
        if (value == null) {
            throw new IllegalStateException(String.format("No value found for column %s", column));
        }
        return value;
    }

}
