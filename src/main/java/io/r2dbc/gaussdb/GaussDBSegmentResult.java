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

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.r2dbc.gaussdb.api.ErrorDetails;
import io.r2dbc.gaussdb.api.GaussDBResult;
import io.r2dbc.gaussdb.message.backend.BackendMessage;
import io.r2dbc.gaussdb.message.backend.CommandComplete;
import io.r2dbc.gaussdb.message.backend.DataRow;
import io.r2dbc.gaussdb.message.backend.ErrorResponse;
import io.r2dbc.gaussdb.message.backend.NoticeResponse;
import io.r2dbc.gaussdb.message.backend.RowDescription;
import io.r2dbc.gaussdb.util.Assert;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An implementation of {@link Result} based on {@link Segment}.
 *
 * @since 0.9
 */
final class GaussDBSegmentResult extends AbstractReferenceCounted implements GaussDBResult {

    private final Flux<Segment> segments;

    private GaussDBSegmentResult(Flux<Segment> segments) {
        this.segments = segments;
    }

    GaussDBSegmentResult(ConnectionResources resources, Flux<BackendMessage> messages, ExceptionFactory factory) {
        Assert.requireNonNull(resources, "resources must not be null");
        Assert.requireNonNull(messages, "messages must not be null");
        Assert.requireNonNull(factory, "factory must not be null");

        AtomicReference<RowDescription> rowDescriptionHolder = new AtomicReference<>();
        AtomicReference<GaussDBRowMetadata> metadataHolder = new AtomicReference<>();

        this.segments = messages
            .doOnNext(message -> {

                if (message instanceof RowDescription) {
                    rowDescriptionHolder.set((RowDescription) message);
                    metadataHolder.set(GaussDBRowMetadata.toRowMetadata(resources.getCodecs(), (RowDescription) message));
                }

            }).handle((message, sink) -> {

                if (message instanceof ErrorResponse) {
                    sink.next(new GaussDBErrorSegment((ErrorResponse) message, factory));
                    return;
                }

                if (message instanceof NoticeResponse) {
                    sink.next(new GaussDBNoticeSegment((NoticeResponse) message, factory));
                    return;
                }

                if (message instanceof CommandComplete) {

                    Long rowCount = ((CommandComplete) message).getRows();
                    if (rowCount != null) {
                        sink.next(new GaussDBUpdateCountSegment(rowCount));
                    }
                    return;
                }

                if (message instanceof DataRow) {

                    RowDescription rowDescription = rowDescriptionHolder.get();
                    GaussDBRowMetadata metadata = metadataHolder.get();

                    if (rowDescription == null) {
                        sink.error(new IllegalStateException("DataRow without RowDescription"));
                        return;
                    }

                    if (metadata == null) {
                        sink.error(new IllegalStateException("DataRow without GaussDBRowMetadata"));
                        return;
                    }

                    sink.next(new GaussDBRowSegment(GaussDBRow.toRow(resources, (DataRow) message, metadata, rowDescription), (DataRow) message));
                    return;
                }

                ReferenceCountUtil.release(message);
            });
    }

    @Override
    public Mono<Long> getRowsUpdated() {
        return this.segments
            .<Integer>handle((segment, sink) -> {

                try {
                    if (segment instanceof GaussDBErrorSegment) {
                        sink.error(((GaussDBErrorSegment) segment).exception());
                        return;
                    }

                    if (segment instanceof UpdateCount) {
                        sink.next((int) (((UpdateCount) segment).value()));
                    }

                } finally {
                    ReferenceCountUtil.release(segment);
                }
            }).collectList().handle((list, sink) -> {

                if (list.isEmpty()) {
                    return;
                }

                long sum = 0;

                for (Integer integer : list) {
                    sum += integer;
                }

                sink.next(sum);
            });
    }

    @Override
    public <T> Flux<T> map(BiFunction<Row, RowMetadata, ? extends T> f) {
        Assert.requireNonNull(f, "f must not be null");

        return this.segments
            .handle((segment, sink) -> {

                try {
                    if (segment instanceof GaussDBErrorSegment) {
                        sink.error(((GaussDBErrorSegment) segment).exception());
                        return;
                    }

                    if (segment instanceof RowSegment) {
                        RowSegment row = (RowSegment) segment;
                        sink.next(f.apply(row.row(), row.row().getMetadata()));
                    }

                } finally {
                    ReferenceCountUtil.release(segment);
                }
            });
    }

    @Override
    public GaussDBSegmentResult filter(Predicate<Segment> filter) {
        Assert.requireNonNull(filter, "filter must not be null");
        return new GaussDBSegmentResult(this.segments.filter(it -> {

            boolean result = filter.test(it);

            if (!result) {
                ReferenceCountUtil.release(it);
            }

            return result;
        }));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Publisher<T> flatMap(Function<Segment, ? extends Publisher<? extends T>> mappingFunction) {
        Assert.requireNonNull(mappingFunction, "mappingFunction must not be null");
        return this.segments.concatMap(segment -> {

            Publisher<? extends T> result = mappingFunction.apply(segment);

            if (result == null) {
                return Mono.error(new IllegalStateException("The mapper returned a null Publisher"));
            }

            // doAfterTerminate to not release resources before they had a chance to get emitted
            if (result instanceof Mono) {
                return ((Mono<T>) result).doFinally(s -> ReferenceCountUtil.release(segment));
            }

            return Flux.from(result).doFinally(s -> ReferenceCountUtil.release(segment));
        });
    }

    @Override
    protected void deallocate() {

        // drain messages for cleanup
        this.getRowsUpdated().subscribe();
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }

    @Override
    public String toString() {
        return "GaussDBSegmentResult{" +
            "segments=" + this.segments +
            '}';
    }

    static GaussDBSegmentResult toResult(ConnectionResources resources, Flux<BackendMessage> messages, ExceptionFactory factory) {
        return new GaussDBSegmentResult(resources, messages, factory);
    }

    static class GaussDBRowSegment extends AbstractReferenceCounted implements Result.RowSegment {

        private final Row row;

        private final ReferenceCounted releaseable;

        public GaussDBRowSegment(Row row, ReferenceCounted releaseable) {
            this.row = row;
            this.releaseable = releaseable;
        }

        @Override
        public Row row() {
            return this.row;
        }

        @Override
        protected void deallocate() {
            ReferenceCountUtil.release(this.releaseable);
        }

        @Override
        public ReferenceCounted touch(Object hint) {
            return this;
        }

    }

    static class GaussDBUpdateCountSegment implements Result.UpdateCount {

        private final long value;

        public GaussDBUpdateCountSegment(long value) {
            this.value = value;
        }

        @Override
        public long value() {
            return this.value;
        }

    }

    static class GaussDBErrorSegment implements Result.Message {

        private final ExceptionFactory factory;

        private final ErrorDetails details;

        public GaussDBErrorSegment(ErrorResponse response, ExceptionFactory factory) {
            this.factory = factory;
            this.details = new ErrorDetails(response.getFields());
        }

        @Override
        public R2dbcException exception() {
            return this.factory.createException(this.details);
        }

        @Override
        public int errorCode() {
            return 0;
        }

        @Override
        public String sqlState() {
            return this.details.getCode();
        }

        @Override
        public String message() {
            return this.details.getMessage();
        }

    }

    static class GaussDBNoticeSegment implements Result.Message {

        private final ExceptionFactory factory;

        private final ErrorDetails details;

        public GaussDBNoticeSegment(NoticeResponse response, ExceptionFactory factory) {
            this.factory = factory;
            this.details = new ErrorDetails(response.getFields());
        }

        @Override
        public R2dbcException exception() {
            return this.factory.createException(this.details);
        }

        @Override
        public int errorCode() {
            return 0;
        }

        @Override
        public String sqlState() {
            return this.details.getCode();
        }

        @Override
        public String message() {
            return this.details.getMessage();
        }

    }

}
