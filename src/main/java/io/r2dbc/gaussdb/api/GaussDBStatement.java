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

package io.r2dbc.gaussdb.api;

import io.r2dbc.spi.Statement;
import reactor.core.publisher.Flux;

/**
 * A strongly typed implementation of {@link Statement} for a GaussDB database.
 */
public interface GaussDBStatement extends Statement {

    /**
     * {@inheritDoc}
     */
    @Override
    GaussDBStatement add();

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code identifier} is not a {@link String} like {@code $1}, {@code $2}, …
     */
    @Override
    GaussDBStatement bind(String identifier, Object value);

    /**
     * {@inheritDoc}
     */
    @Override
    GaussDBStatement bind(int index, Object value);

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code identifier} is not a {@link String} like {@code $1}, {@code $2}, …
     */
    @Override
    GaussDBStatement bindNull(String identifier, Class<?> type);

    /**
     * {@inheritDoc}
     */
    @Override
    GaussDBStatement bindNull(int index, Class<?> type);

    /**
     * {@inheritDoc}
     */
    @Override
    Flux<GaussDBResult> execute();

    /**
     * {@inheritDoc}
     */
    @Override
    default GaussDBStatement fetchSize(int rows) {
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if this {@link Statement} already has a {@code RETURNING clause} or isn't a {@code DELETE}, {@code INSERT}, or {@code UPDATE} command
     */
    @Override
    GaussDBStatement returnGeneratedValues(String... columns);

}
