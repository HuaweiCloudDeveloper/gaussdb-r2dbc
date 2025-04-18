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

import io.r2dbc.gaussdb.client.Version;

/**
 * Connection metadata for a connection connected to a GaussDB database.
 */
final class GaussDBConnectionMetadata implements io.r2dbc.gaussdb.api.GaussDBConnectionMetadata {

    private final Version version;

    GaussDBConnectionMetadata(Version version) {
        this.version = version;
    }

    @Override
    public String getDatabaseProductName() {
        return "GaussDB";
    }

    @Override
    public String getDatabaseVersion() {
        return this.version.getVersion();
    }

}
