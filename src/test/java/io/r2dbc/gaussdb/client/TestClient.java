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

package io.r2dbc.gaussdb.client;

import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.gaussdb.message.backend.BackendMessage;
import io.r2dbc.gaussdb.message.backend.NotificationResponse;
import io.r2dbc.gaussdb.message.frontend.FrontendMessage;
import io.r2dbc.gaussdb.util.Assert;
import io.r2dbc.gaussdb.util.TestByteBufAllocator;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.r2dbc.gaussdb.client.TransactionStatus.IDLE;

/**
 * Test {@link Client} implementation that allows specification of expectations and assertions.
 */
public final class TestClient implements Client {

    public static final TestClient NO_OP = new TestClient(false, true, null, null, Flux.empty(), IDLE, new Version("9.4"));

    private final boolean expectClose;

    private final boolean connected;

    private final Sinks.Many<NotificationResponse> notificationProcessor = Sinks.many().multicast().onBackpressureBuffer();

    private final Integer processId;

    private final Sinks.Many<FrontendMessage> requestProcessor = Sinks.many().multicast().onBackpressureBuffer();

    private final Sinks.Many<Flux<BackendMessage>> responseProcessor = Sinks.many().replay().all();

    private final Integer secretKey;

    private final TransactionStatus transactionStatus;

    private final Version version;

    private TestClient(boolean expectClose, boolean connected, @Nullable Integer processId, @Nullable Integer secretKey, Flux<Window> windows, TransactionStatus transactionStatus, Version version) {
        this.expectClose = expectClose;
        this.connected = connected;
        this.processId = processId;
        this.secretKey = secretKey;
        this.transactionStatus = Assert.requireNonNull(transactionStatus, "transactionStatus must not be null");
        this.version = version;

        Assert.requireNonNull(windows, "windows must not be null")
            .map(window -> window.exchanges)
            .map(exchanges -> exchanges
                .concatMap(exchange ->

                    this.requestProcessor.asFlux().zipWith(exchange.requests)
                        .handle((tuple, sink) -> {
                            FrontendMessage actual = tuple.getT1();
                            FrontendMessage expected = tuple.getT2();

                            if (!actual.equals(expected)) {
                                sink.error(new AssertionError(String.format("Request %s was not the expected request %s", actual, expected)));
                            }
                        })
                        .thenMany(exchange.responses)))
            .subscribe(this.responseProcessor::tryEmitNext, this.responseProcessor::tryEmitError, this.responseProcessor::tryEmitComplete);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Mono<Void> close() {
        return this.expectClose ? Mono.empty() : Mono.error(new AssertionError("close called unexpectedly"));
    }

    @Override
    public Flux<BackendMessage> exchange(Publisher<FrontendMessage> requests) {
        Assert.requireNonNull(requests, "requests must not be null");

        return this.responseProcessor.asFlux()
            .doOnSubscribe(s ->
                Flux.from(requests)
                    .subscribe(this.requestProcessor::tryEmitNext, this.requestProcessor::tryEmitError))
            .next()
            .flatMapMany(Function.identity());
    }

    @Override
    public Flux<BackendMessage> exchange(Predicate<BackendMessage> takeUntil, Publisher<FrontendMessage> requests) {
        return this.responseProcessor.asFlux()
            .doOnSubscribe(s ->
                Flux.from(requests)
                    .subscribe(this.requestProcessor::tryEmitNext, this.requestProcessor::tryEmitError))
            .next()
            .flatMapMany(Function.identity())
            .takeWhile(takeUntil.negate());
    }

    @Override
    public ByteBufAllocator getByteBufAllocator() {
        return TestByteBufAllocator.TEST;
    }

    @Override
    public ConnectionContext getContext() {
        return new ConnectionContext();
    }

    @Override
    public Optional<Integer> getProcessId() {
        return Optional.ofNullable(this.processId);
    }

    @Override
    public Scheduler getScheduler() {
        return Schedulers.immediate();
    }

    @Override
    public Optional<Integer> getSecretKey() {
        return Optional.ofNullable(this.secretKey);
    }

    @Override
    public Optional<TimeZone> getTimeZone() {
        return Optional.empty();
    }

    @Override
    public TransactionStatus getTransactionStatus() {
        return this.transactionStatus;
    }

    @Override
    public Version getVersion() {
        return this.version;
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public Mono<Void> cancelRequest() {
        return Mono.empty();
    }

    @Override
    public void send(FrontendMessage message) {
        this.requestProcessor.tryEmitNext(message);
    }

    @Override
    public Disposable addNotificationListener(Consumer<NotificationResponse> consumer) {
        return this.notificationProcessor.asFlux().subscribe(consumer);
    }

    @Override
    public void addNotificationListener(Subscriber<NotificationResponse> consumer) {
        this.notificationProcessor.asFlux().subscribe(consumer);
    }

    public void notify(NotificationResponse notification) {
        this.notificationProcessor.tryEmitNext(notification);
    }

    public static final class Builder {

        private final List<Window.Builder<?>> windows = new ArrayList<>();

        private boolean expectClose = false;

        private boolean connected = true;

        private Integer processId = null;

        private Integer secretKey = null;

        private TransactionStatus transactionStatus = IDLE;

        private Version version = new Version("9.4beta1");

        private Builder() {
        }

        public TestClient build() {
            return new TestClient(this.expectClose, this.connected, this.processId, this.secretKey, Flux.fromIterable(this.windows).map(Window.Builder::build), this.transactionStatus, this.version);
        }

        public Builder expectClose() {
            this.expectClose = true;
            return this;
        }

        public Builder withConnected(boolean connected) {
            this.connected = connected;
            return this;
        }

        public Exchange.Builder<Builder> expectRequest(FrontendMessage... requests) {
            Assert.requireNonNull(requests, "requests must not be null");

            Window.Builder<Builder> window = new Window.Builder<>(this);
            this.windows.add(window);

            Exchange.Builder<Builder> exchange = new Exchange.Builder<>(this, requests);
            window.exchanges.add(exchange);

            return exchange;
        }

        public Builder processId(Integer processId) {
            this.processId = Assert.requireNonNull(processId, "processId must not be null");
            return this;
        }

        public Builder secretKey(Integer secretKey) {
            this.secretKey = Assert.requireNonNull(secretKey, "secretKey must not be null");
            return this;
        }

        public Builder transactionStatus(TransactionStatus transactionStatus) {
            this.transactionStatus = Assert.requireNonNull(transactionStatus, "transactionStatus must not be null");
            return this;
        }

        public Window.Builder<Builder> window() {
            Window.Builder<Builder> window = new Window.Builder<>(this);
            this.windows.add(window);
            return window;
        }

        public Builder withVersion(Version version) {
            this.version = version;
            return this;
        }

    }

    private static final class Exchange {

        private final Flux<FrontendMessage> requests;

        private final Publisher<BackendMessage> responses;

        private Exchange(Flux<FrontendMessage> requests, Publisher<BackendMessage> responses) {
            this.requests = Assert.requireNonNull(requests, "requests must not be null");
            this.responses = Assert.requireNonNull(responses, "responses must not be null");
        }

        public static final class Builder<T> {

            private final T chain;

            private final Flux<FrontendMessage> requests;

            private Publisher<BackendMessage> responses;

            private Builder(T chain, FrontendMessage... requests) {
                this.chain = Assert.requireNonNull(chain, "chain must not be null");
                this.requests = Flux.just(Assert.requireNonNull(requests, "requests must not be null"));
            }

            public T thenRespond(BackendMessage... responses) {
                Assert.requireNonNull(responses, "responses must not be null");

                return thenRespond(Flux.just(responses));
            }

            T thenRespond(Publisher<BackendMessage> responses) {
                Assert.requireNonNull(responses, "responses must not be null");

                this.responses = responses;
                return this.chain;
            }

            private Exchange build() {
                return new Exchange(this.requests, this.responses);
            }

        }

    }

    private static final class Window {

        private final Flux<Exchange> exchanges;

        private Window(Flux<Exchange> exchanges) {
            this.exchanges = Assert.requireNonNull(exchanges, "exchanges must not be null");
        }

        public static final class Builder<T> {

            private final T chain;

            private final List<Exchange.Builder<?>> exchanges = new ArrayList<>();

            private Builder(T chain) {
                this.chain = Assert.requireNonNull(chain, "chain must not be null");
            }

            public T done() {
                return this.chain;
            }

            public Exchange.Builder<Builder<T>> expectRequest(FrontendMessage... requests) {
                Assert.requireNonNull(requests, "requests must not be null");

                Exchange.Builder<Builder<T>> exchange = new Exchange.Builder<>(this, requests);
                this.exchanges.add(exchange);
                return exchange;
            }

            private Window build() {
                return new Window(Flux.fromIterable(this.exchanges).map(Exchange.Builder::build));
            }

        }

    }

}
