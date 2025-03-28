/*
 * Copyright 2020 the original author or authors.
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
import io.r2dbc.gaussdb.client.Client;
import io.r2dbc.gaussdb.client.PortalNameSupplier;
import io.r2dbc.gaussdb.codec.Codecs;
import io.r2dbc.gaussdb.util.Assert;

/**
 * Value object capturing contextual connection resources such as {@link Client}, {@link Codecs} and the {@link GaussDBConnection connection facade}.
 */
final class ConnectionResources {

    private final Client client;

    private final Codecs codecs;

    private final GaussDBConnection connection;

    private final GaussDBConnectionConfiguration configuration;

    private final StatementCache statementCache;

    private final PortalNameSupplier portalNameSupplier;

    ConnectionResources(Client client, Codecs codecs, GaussDBConnection connection, GaussDBConnectionConfiguration configuration, PortalNameSupplier portalNameSupplier,
                        StatementCache statementCache) {
        this.client = client;
        this.codecs = codecs;
        this.connection = connection;
        this.configuration = configuration;
        this.portalNameSupplier = Assert.requireNonNull(portalNameSupplier, "portalNameSupplier must not be null");
        this.statementCache = Assert.requireNonNull(statementCache, "statementCache must not be null");
    }

    public Client getClient() {
        return this.client;
    }

    public Codecs getCodecs() {
        return this.codecs;
    }

    public GaussDBConnection getConnection() {
        return this.connection;
    }

    public GaussDBConnectionConfiguration getConfiguration() {
        return this.configuration;
    }

    public PortalNameSupplier getPortalNameSupplier() {
        return this.portalNameSupplier;
    }

    public StatementCache getStatementCache() {
        return this.statementCache;
    }

    @Override
    public String toString() {
        return "ConnectionContext{" +
            "client=" + this.client +
            ", codecs=" + this.codecs +
            ", connection=" + this.connection +
            ", configuration=" + this.configuration +
            ", portalNameSupplier=" + this.portalNameSupplier +
            ", statementCache=" + this.statementCache +
            '}';
    }

}
