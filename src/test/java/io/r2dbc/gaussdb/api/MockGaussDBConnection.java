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

import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.TransactionDefinition;
import io.r2dbc.spi.ValidationDepth;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public final class MockGaussDBConnection implements GaussDBConnection {

    private final MockGaussDBStatement statement;

    public MockGaussDBConnection(MockGaussDBStatement statement) {
        this.statement = statement;
    }

    @Override
    public Mono<Void> beginTransaction() {
        return Mono.empty();
    }

    @Override
    public Mono<Void> beginTransaction(TransactionDefinition definition) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> close() {
        return Mono.empty();
    }

    @Override
    public Mono<Void> commitTransaction() {
        return Mono.empty();
    }

    @Override
    public CopyInBuilder copyIn(String sql) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GaussDBBatch createBatch() {
        return null;
    }

    @Override
    public Mono<Void> createSavepoint(String name) {
        return Mono.empty();
    }

    @Override
    public GaussDBStatement createStatement(String sql) {
        return this.statement;
    }

    @Override
    public Flux<Notification> getNotifications() {
        return Flux.empty();
    }

    @Override
    public Mono<Void> cancelRequest() {
        return Mono.empty();
    }

    @Override
    public GaussDBConnectionMetadata getMetadata() {
        return null;
    }

    @Override
    public IsolationLevel getTransactionIsolationLevel() {
        return null;
    }

    @Override
    public boolean isAutoCommit() {
        return false;
    }

    @Override
    public Mono<Void> setLockWaitTimeout(Duration lockTimeout) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> releaseSavepoint(String name) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> rollbackTransaction() {
        return Mono.empty();
    }

    @Override
    public Mono<Void> rollbackTransactionToSavepoint(String name) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> setAutoCommit(boolean autoCommit) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> setTransactionIsolationLevel(IsolationLevel isolationLevel) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> setStatementTimeout(Duration statementTimeout) {
        return Mono.empty();
    }

    @Override
    public Mono<Boolean> validate(ValidationDepth depth) {
        return Mono.empty();
    }

}
