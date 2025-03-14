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

package io.r2dbc.gaussdb.api;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

public final class MockPostgresqlStatement implements PostgresqlStatement {

    private final Flux<PostgresqlResult> results;

    public MockPostgresqlStatement(Flux<PostgresqlResult> results) {
        this.results = results;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public PostgresqlStatement add() {
        return this;
    }

    @Override
    public PostgresqlStatement bind(String identifier, Object value) {
        return this;
    }

    @Override
    public PostgresqlStatement bind(int index, Object value) {
        return this;
    }

    @Override
    public PostgresqlStatement bindNull(String identifier, Class<?> type) {
        return this;
    }

    @Override
    public PostgresqlStatement bindNull(int index, Class<?> type) {
        return this;
    }

    @Override
    public Flux<PostgresqlResult> execute() {
        return this.results;
    }

    @Override
    public PostgresqlStatement returnGeneratedValues(String... columns) {
        return this;
    }

    public static final class Builder {

        private final List<PostgresqlResult> results = new ArrayList<>();

        private Builder() {
        }

        public MockPostgresqlStatement build() {
            return new MockPostgresqlStatement(Flux.fromIterable(this.results));
        }

        public Builder result(PostgresqlResult result) {
            this.results.add(result);
            return this;
        }

    }

}
