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

import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * A {@link Result} representing the results of a query against a GaussDB database.
 */
public interface GaussDBResult extends Result {

    /**
     * {@inheritDoc}
     */
    @Override
    Mono<Long> getRowsUpdated();

    /**
     * {@inheritDoc}
     */
    @Override
    <T> Flux<T> map(BiFunction<Row, RowMetadata, ? extends T> mappingFunction);

    /**
     * {@inheritDoc}
     */
    @Override
    GaussDBResult filter(Predicate<Segment> predicate);

}
