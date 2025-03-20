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

import io.r2dbc.gaussdb.api.GaussDBResult;
import io.r2dbc.gaussdb.message.backend.CommandComplete;
import io.r2dbc.gaussdb.message.backend.DataRow;
import io.r2dbc.gaussdb.message.backend.EmptyQueryResponse;
import io.r2dbc.gaussdb.message.backend.RowDescription;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link io.r2dbc.gaussdb.GaussDBResult}.
 */
final class GaussDBResultUnitTests {

    @Test
    void toResultCommandComplete() {
        io.r2dbc.gaussdb.GaussDBResult result = io.r2dbc.gaussdb.GaussDBResult.toResult(MockContext.empty(), Flux.just(new CommandComplete("test", null, 1L)), ExceptionFactory.INSTANCE);

        result.map((row, rowMetadata) -> row)
            .as(StepVerifier::create)
            .verifyComplete();

        result.getRowsUpdated()
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    void toResultCommandCompleteUsingSegments() {
        GaussDBResult result = io.r2dbc.gaussdb.GaussDBResult.toResult(MockContext.empty(), Flux.just(new CommandComplete("test", null, 1L)), ExceptionFactory.INSTANCE).filter(it -> true);

        result.map((row, rowMetadata) -> row)
            .as(StepVerifier::create)
            .verifyComplete();

        result.getRowsUpdated()
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    void toResultEmptyQueryResponse() {
        io.r2dbc.gaussdb.GaussDBResult result = io.r2dbc.gaussdb.GaussDBResult.toResult(MockContext.empty(), Flux.just(EmptyQueryResponse.INSTANCE), ExceptionFactory.INSTANCE);

        result.map((row, rowMetadata) -> row)
            .as(StepVerifier::create)
            .verifyComplete();

        result.getRowsUpdated()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    void toResultNoContext() {
        assertThatIllegalArgumentException().isThrownBy(() -> io.r2dbc.gaussdb.GaussDBResult.toResult(null, Flux.empty(), ExceptionFactory.INSTANCE))
            .withMessage("resources must not be null");
    }

    @Test
    void toResultNoMessages() {
        assertThatIllegalArgumentException().isThrownBy(() -> io.r2dbc.gaussdb.GaussDBResult.toResult(MockContext.empty(), null, ExceptionFactory.INSTANCE))
            .withMessage("messages must not be null");
    }

    @Test
    void toResultRowDescriptionRowsUpdated() {
        io.r2dbc.gaussdb.GaussDBResult result = io.r2dbc.gaussdb.GaussDBResult.toResult(MockContext.empty(), Flux.just(new RowDescription(Collections.emptyList()), new DataRow(), new CommandComplete
            ("test", null, null)), ExceptionFactory.INSTANCE);

        result.getRowsUpdated()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    void toResultRowDescriptionRowsUpdatedUsingSegments() {
        GaussDBResult result = io.r2dbc.gaussdb.GaussDBResult.toResult(MockContext.empty(), Flux.just(new RowDescription(Collections.emptyList()), new DataRow(), new CommandComplete
            ("test", null, null)), ExceptionFactory.INSTANCE).filter(it -> true);

        result.getRowsUpdated()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    void toResultRowDescriptionMap() {
        io.r2dbc.gaussdb.GaussDBResult result = io.r2dbc.gaussdb.GaussDBResult.toResult(MockContext.empty(), Flux.just(new RowDescription(Collections.emptyList()), new DataRow(), new CommandComplete
            ("test", null, null)), ExceptionFactory.INSTANCE);

        result.map((row, rowMetadata) -> row)
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void toResultRowDescriptionMapUsingSegments() {
        GaussDBResult result = io.r2dbc.gaussdb.GaussDBResult.toResult(MockContext.empty(), Flux.just(new RowDescription(Collections.emptyList()), new DataRow(), new CommandComplete
            ("test", null, null)), ExceptionFactory.INSTANCE).filter(it -> true);

        result.map((row, rowMetadata) -> row)
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

}
