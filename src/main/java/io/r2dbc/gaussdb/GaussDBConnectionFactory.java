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

import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.gaussdb.api.ErrorDetails;
import io.r2dbc.gaussdb.api.GaussDBReplicationConnection;
import io.r2dbc.gaussdb.api.GaussDBException;
import io.r2dbc.gaussdb.client.Client;
import io.r2dbc.gaussdb.client.ConnectionSettings;
import io.r2dbc.gaussdb.client.ReactorNettyClient;
import io.r2dbc.gaussdb.codec.DefaultCodecs;
import io.r2dbc.gaussdb.extension.CodecRegistrar;
import io.r2dbc.gaussdb.util.Assert;
import io.r2dbc.gaussdb.util.Operators;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * An implementation of {@link ConnectionFactory} for creating connections to a GaussDB database.
 */
public final class GaussDBConnectionFactory implements ConnectionFactory {

    private static final ConnectionFunction DEFAULT_CONNECTION_FUNCTION = (endpoint, settings) ->
        ReactorNettyClient.connect(endpoint, settings).cast(Client.class);

    private static final String REPLICATION_OPTION = "replication";

    private static final String REPLICATION_DATABASE = "database";

    private final ConnectionFunction connectionFunction;

    private final GaussDBConnectionConfiguration configuration;

    private final Extensions extensions;

    /**
     * Create a new connection factory.
     *
     * @param configuration the configuration to use
     * @throws IllegalArgumentException if {@code configuration} is {@code null}
     */
    public GaussDBConnectionFactory(GaussDBConnectionConfiguration configuration) {
        this(DEFAULT_CONNECTION_FUNCTION, configuration);
    }

    /**
     * Create a new connection factory.
     *
     * @param connectionFunction the connectionFunction to establish
     * @param configuration      the configuration to use
     * @throws IllegalArgumentException if {@code configuration} is {@code null}
     */
    GaussDBConnectionFactory(ConnectionFunction connectionFunction, GaussDBConnectionConfiguration configuration) {
        this.connectionFunction = Assert.requireNonNull(connectionFunction, "connectionFunction must not be null");
        this.configuration = Assert.requireNonNull(configuration, "configuration must not be null");
        this.extensions = getExtensions(configuration);
    }

    private static Extensions getExtensions(GaussDBConnectionConfiguration configuration) {
        Extensions extensions = Extensions.from(configuration.getExtensions());

        if (configuration.isAutodetectExtensions()) {
            extensions = extensions.mergeWith(Extensions.autodetect());
        }

        return extensions;
    }

    @Override
    public Mono<io.r2dbc.gaussdb.api.GaussDBConnection> create() {

        if (isReplicationConnection()) {
            throw new UnsupportedOperationException("Cannot create replication connection through create(). Use replication() method instead.");
        }

        ConnectionStrategy connectionStrategy = ConnectionStrategyFactory.getConnectionStrategy(this.connectionFunction, this.configuration, this.configuration.getConnectionSettings());

        return doCreateConnection(false, connectionStrategy).cast(io.r2dbc.gaussdb.api.GaussDBConnection.class);
    }

    /**
     * Create a new {@link GaussDBReplicationConnection} for interaction with replication streams.
     *
     * @return a new {@link GaussDBReplicationConnection} for interaction with replication streams.
     */
    public Mono<GaussDBReplicationConnection> replication() {

        Map<String, String> options = new LinkedHashMap<>(this.configuration.getOptions());
        options.put(REPLICATION_OPTION, REPLICATION_DATABASE);

        ConnectionSettings connectionSettings = this.configuration.getConnectionSettings().mutate(builder -> builder.startupOptions(options));

        ConnectionStrategy connectionStrategy = ConnectionStrategyFactory.getConnectionStrategy(this.connectionFunction, this.configuration, connectionSettings);

        return doCreateConnection(true, connectionStrategy).map(DefaultGaussDBReplicationConnection::new);
    }

    private Mono<GaussDBConnection> doCreateConnection(boolean forReplication, ConnectionStrategy connectionStrategy) {

        ZoneId defaultZone = TimeZone.getDefault().toZoneId();

        return connectionStrategy.connect()
            .flatMap(client -> {

                DefaultCodecs codecs = new DefaultCodecs(client.getByteBufAllocator(), this.configuration.isPreferAttachedBuffers(),
                    () -> client.getTimeZone().map(TimeZone::toZoneId).orElse(defaultZone));
                StatementCache statementCache = StatementCache.fromPreparedStatementCacheQueries(client, this.configuration.getPreparedStatementCacheQueries());

                // early connection object to retrieve initialization details
                GaussDBConnection earlyConnection = new GaussDBConnection(client, codecs, DefaultPortalNameSupplier.INSTANCE, statementCache, IsolationLevel.READ_COMMITTED,
                    this.configuration);

                Mono<IsolationLevel> isolationLevelMono = Mono.just(IsolationLevel.READ_COMMITTED);
                if (!forReplication) {
                    isolationLevelMono = getIsolationLevel(earlyConnection);
                }
                return isolationLevelMono
                    // actual connection to be used
                    .map(isolationLevel -> new GaussDBConnection(client, codecs, DefaultPortalNameSupplier.INSTANCE, statementCache, isolationLevel, this.configuration))
                    .delayUntil(connection -> {
                        return prepareConnection(connection, client.getByteBufAllocator(), codecs, forReplication);
                    })
                    .onErrorResume(throwable -> this.closeWithError(client, throwable));
            }).onErrorMap(e -> cannotConnect(e, connectionStrategy))
            .flux()
            .as(Operators::discardOnCancel)
            .single()
            .doOnDiscard(GaussDBConnection.class, client -> client.close().subscribe());
    }

    private boolean isReplicationConnection() {
        Map<String, String> options = this.configuration.getOptions();
        return REPLICATION_DATABASE.equalsIgnoreCase(options.get(REPLICATION_OPTION));
    }

    private Publisher<?> prepareConnection(GaussDBConnection connection, ByteBufAllocator byteBufAllocator, DefaultCodecs codecs, boolean forReplication) {

        List<Publisher<?>> publishers = new ArrayList<>();

        if (!forReplication) {
            this.extensions.forEach(CodecRegistrar.class, it -> {
                publishers.add(it.register(connection, byteBufAllocator, codecs));
            });
        }

        return Flux.concat(publishers).then();
    }

    private Mono<GaussDBConnection> closeWithError(Client client, Throwable throwable) {
        return client.close().then(Mono.error(throwable));
    }

    private Throwable cannotConnect(Throwable throwable, ConnectionStrategy strategy) {

        if (throwable instanceof R2dbcException) {
            return throwable;
        }

        return new GaussDBConnectionException(String.format("Cannot connect to %s", strategy), throwable);
    }

    @Override
    public ConnectionFactoryMetadata getMetadata() {
        return GaussDBConnectionFactoryMetadata.INSTANCE;
    }

    GaussDBConnectionConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public String toString() {
        return "GaussDBConnectionFactory{" +
            ", configuration=" + this.configuration +
            ", extensions=" + this.extensions +
            '}';
    }

    private Mono<IsolationLevel> getIsolationLevel(io.r2dbc.gaussdb.api.GaussDBConnection connection) {
        return connection.createStatement("SHOW TRANSACTION ISOLATION LEVEL")
            .fetchSize(0)
            .execute()
            .flatMap(it -> it.map((row, rowMetadata) -> {
                String level = row.get(0, String.class);

                if (level == null) {
                    return IsolationLevel.READ_COMMITTED; // Best guess.
                }

                return IsolationLevel.valueOf(level.toUpperCase(Locale.US));
            })).defaultIfEmpty(IsolationLevel.READ_COMMITTED).last();
    }

    static class GaussDBConnectionException extends R2dbcNonTransientResourceException implements GaussDBException {

        private static final String CONNECTION_DOES_NOT_EXIST = "08003";

        private final ErrorDetails errorDetails;

        public GaussDBConnectionException(String reason, @Nullable Throwable cause) {
            super(reason, CONNECTION_DOES_NOT_EXIST, 0, null, cause);
            this.errorDetails = ErrorDetails.fromCodeAndMessage(CONNECTION_DOES_NOT_EXIST, reason);
        }

        @Override
        public ErrorDetails getErrorDetails() {
            return this.errorDetails;
        }

    }

}
