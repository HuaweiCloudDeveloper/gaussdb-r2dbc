/*
 * Copyright 2021 the original author or authors.
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.r2dbc.gaussdb.api.GaussDBResult;
import io.r2dbc.gaussdb.client.Binding;
import io.r2dbc.gaussdb.client.ConnectionContext;
import io.r2dbc.gaussdb.client.EncodedParameter;
import io.r2dbc.gaussdb.client.SimpleQueryMessageFlow;
import io.r2dbc.gaussdb.message.backend.BackendMessage;
import io.r2dbc.gaussdb.message.backend.CommandComplete;
import io.r2dbc.gaussdb.message.backend.EmptyQueryResponse;
import io.r2dbc.gaussdb.message.backend.ErrorResponse;
import io.r2dbc.gaussdb.message.frontend.Bind;
import io.r2dbc.gaussdb.util.Assert;
import io.r2dbc.gaussdb.util.GeneratedValuesUtils;
import io.r2dbc.gaussdb.util.Operators;
import io.r2dbc.spi.Statement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static io.r2dbc.gaussdb.message.frontend.Execute.NO_LIMIT;
import static io.r2dbc.gaussdb.util.PredicateUtils.or;

/**
 * A generic {@link Statement}.
 *
 * @since 0.9
 */
final class GaussDBStatement implements io.r2dbc.gaussdb.api.GaussDBStatement {

    private static final Predicate<BackendMessage> WINDOW_UNTIL = or(CommandComplete.class::isInstance, EmptyQueryResponse.class::isInstance, ErrorResponse.class::isInstance);

    private final ArrayDeque<Binding> bindings;

    private final ConnectionResources resources;

    private final ConnectionContext connectionContext;

    private final ParsedSql parsedSql;

    private int fetchSize;

    private String[] generatedColumns;

    GaussDBStatement(ConnectionResources resources, String sql) {
        this.resources = Assert.requireNonNull(resources, "resources must not be null");
        this.parsedSql = GaussDBSqlParser.parse(Assert.requireNonNull(sql, "sql must not be null"));
        this.connectionContext = resources.getClient().getContext();
        this.bindings = new ArrayDeque<>(this.parsedSql.getParameterCount());

        if (this.parsedSql.getStatementCount() > 1 && this.parsedSql.getParameterCount() > 0) {
            throw new IllegalArgumentException(String.format("Statement '%s' cannot be created. This is often due to the presence of both multiple statements and parameters at the same time.", sql));
        }

        fetchSize(this.resources.getConfiguration().getFetchSize(sql));
    }

    @Override
    public GaussDBStatement add() {
        Binding binding = this.bindings.peekLast();
        if (binding != null) {
            binding.validate();
        }
        this.bindings.add(new Binding(this.parsedSql.getParameterCount()));
        return this;
    }

    @Override
    public GaussDBStatement bind(String identifier, Object value) {
        return bind(getIdentifierIndex(identifier), value);
    }

    @Override
    public GaussDBStatement bind(int index, Object value) {
        Assert.requireNonNull(value, "value must not be null");

        BindingLogger.logBind(this.connectionContext, index, value);
        getCurrentOrFirstBinding().add(index, this.resources.getCodecs().encode(value));
        return this;
    }

    @Override
    public GaussDBStatement bindNull(String identifier, Class<?> type) {
        return bindNull(getIdentifierIndex(identifier), type);
    }

    @Override
    public GaussDBStatement bindNull(int index, Class<?> type) {
        Assert.requireNonNull(type, "type must not be null");

        if (index >= this.parsedSql.getParameterCount()) {
            throw new UnsupportedOperationException(String.format("Cannot bind parameter %d, statement has %d parameters", index, this.parsedSql.getParameterCount()));
        }

        BindingLogger.logBindNull(this.connectionContext, index, type);
        getCurrentOrFirstBinding().add(index, this.resources.getCodecs().encodeNull(type));
        return this;
    }

    @Nonnull
    private Binding getCurrentOrFirstBinding() {
        Binding binding = this.bindings.peekLast();
        if (binding == null) {
            Binding newBinding = new Binding(this.parsedSql.getParameterCount());
            this.bindings.add(newBinding);
            return newBinding;
        } else {
            return binding;
        }
    }

    @Override
    public Flux<GaussDBResult> execute() {
        if (this.generatedColumns == null) {
            return execute(this.parsedSql.getSql());
        }
        return execute(GeneratedValuesUtils.augment(this.parsedSql.getSql(), this.generatedColumns));
    }

    @Override
    public GaussDBStatement returnGeneratedValues(String... columns) {
        Assert.requireNonNull(columns, "columns must not be null");

        if (this.parsedSql.hasDefaultTokenValue("RETURNING")) {
            throw new IllegalStateException("Statement already includes RETURNING clause");
        }

        if (!this.parsedSql.hasDefaultTokenValue("DELETE", "INSERT", "UPDATE")) {
            throw new IllegalStateException("Statement is not a DELETE, INSERT, or UPDATE command");
        }

        this.generatedColumns = columns;
        return this;
    }

    @Override
    public GaussDBStatement fetchSize(int rows) {
        Assert.isTrue(rows >= 0, "fetch size must be greater or equal zero");
        this.fetchSize = rows;
        return this;
    }

    @Override
    public String toString() {
        return "GaussDBStatement{" +
            "bindings=" + this.bindings +
            ", context=" + this.resources +
            ", sql='" + this.parsedSql.getSql() + '\'' +
            ", generatedColumns=" + Arrays.toString(this.generatedColumns) +
            '}';
    }

    Binding getCurrentBinding() {
        return getCurrentOrFirstBinding();
    }

    private int getIdentifierIndex(String identifier) {
        Assert.requireNonNull(identifier, "identifier must not be null");
        Assert.requireType(identifier, String.class, "identifier must be a String");
        if (!identifier.startsWith("$")) {
            throw new NoSuchElementException(String.format("\"%s\" is not a valid identifier", identifier));
        }
        try {
            return Integer.parseInt(identifier.substring(1)) - 1;
        } catch (NumberFormatException e) {
            throw new NoSuchElementException(String.format("\"%s\" is not a valid identifier", identifier));
        }
    }

    private Flux<GaussDBResult> execute(String sql) {
        ExceptionFactory factory = ExceptionFactory.withSql(sql);

        CompletableFuture<Void> onCancel = new CompletableFuture<>();
        AtomicBoolean canceled = new AtomicBoolean();

        if (this.parsedSql.getParameterCount() != 0) {
            // Extended query protocol
            if (this.bindings.size() == 0) {
                throw new IllegalStateException("No parameters have been bound");
            }

            this.bindings.forEach(Binding::validate);
            int fetchSize = this.fetchSize;
            return Flux.defer(() -> {

                // possible optimization: fetch all when statement is already prepared or first statement to be prepared
                if (this.bindings.size() == 1) {

                    Binding binding = this.bindings.peekFirst();
                    Flux<BackendMessage> messages = collectBindingParameters(binding).flatMapMany(values -> ExtendedFlowDelegate.runQuery(this.resources, factory, sql, binding, values, fetchSize,
                        new AtomicBoolean()));
                    return Flux.just(io.r2dbc.gaussdb.GaussDBResult.toResult(this.resources, messages, factory));
                }

                Iterator<Binding> iterator = this.bindings.iterator();
                Sinks.Many<Binding> bindings = Sinks.many().unicast().onBackpressureBuffer();

                onCancel.whenComplete((unused, throwable) -> {
                    clearBindings(iterator, canceled);
                });

                return bindings.asFlux()
                    .map(it -> {
                        Flux<BackendMessage> messages =
                            collectBindingParameters(it).flatMapMany(values -> ExtendedFlowDelegate.runQuery(this.resources, factory, sql, it, values, this.fetchSize, canceled)).doOnComplete(() -> tryNextBinding(iterator, bindings, canceled));

                        return io.r2dbc.gaussdb.GaussDBResult.toResult(this.resources, messages, factory);
                    })
                    .doOnCancel(() -> clearBindings(iterator, canceled))
                    .doOnError(e -> clearBindings(iterator, canceled))
                    .doOnSubscribe(it -> bindings.emitNext(iterator.next(), Sinks.EmitFailureHandler.FAIL_FAST));

            }).cast(GaussDBResult.class);
        }

        Flux<BackendMessage> exchange;
        // Simple Query protocol
        if (this.fetchSize != NO_LIMIT) {
            exchange = ExtendedFlowDelegate.runQuery(this.resources, factory, sql, Binding.EMPTY, Collections.emptyList(), this.fetchSize, canceled);
        } else {
            exchange = SimpleQueryMessageFlow.exchange(this.resources.getClient(), sql);
        }

        return exchange.windowUntil(WINDOW_UNTIL)
            .doOnDiscard(ReferenceCounted.class, ReferenceCountUtil::release) // ensure release of rows within WindowPredicate
            .map(messages -> io.r2dbc.gaussdb.GaussDBResult.toResult(this.resources, messages, factory))
            .as(source -> Operators.discardOnCancel(source, () -> {
                canceled.set(true);
                onCancel.complete(null);
            }));
    }

    private static void tryNextBinding(Iterator<Binding> iterator, Sinks.Many<Binding> bindingSink, AtomicBoolean canceled) {

        if (canceled.get()) {
            return;
        }

        try {
            if (iterator.hasNext()) {
                bindingSink.emitNext(iterator.next(), Sinks.EmitFailureHandler.FAIL_FAST);
            } else {
                bindingSink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
            }
        } catch (Exception e) {
            bindingSink.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST);
        }
    }

    private static Mono<List<ByteBuf>> collectBindingParameters(Binding binding) {

        return Flux.fromIterable(binding.getParameterValues())
            .concatMap(f -> {
                if (f == EncodedParameter.NULL_VALUE) {
                    return Flux.just(Bind.NULL_VALUE);
                } else {
                    return Flux.from(f)
                        .reduce(Unpooled.compositeBuffer(), (c, b) -> c.addComponent(true, b));
                }
            })
            .collectList();
    }

    private void clearBindings(Iterator<Binding> iterator, AtomicBoolean canceled) {

        canceled.set(true);

        while (iterator.hasNext()) {
            // exhaust iterator, ignore returned elements
            iterator.next();
        }

        this.bindings.forEach(Binding::clear);
    }

}
