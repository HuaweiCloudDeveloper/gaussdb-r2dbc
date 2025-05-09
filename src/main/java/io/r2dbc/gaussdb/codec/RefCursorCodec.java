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

package io.r2dbc.gaussdb.codec;

import io.netty.buffer.ByteBuf;
import io.r2dbc.gaussdb.api.GaussDBResult;
import io.r2dbc.gaussdb.api.RefCursor;
import io.r2dbc.gaussdb.client.EncodedParameter;
import io.r2dbc.gaussdb.message.Format;
import io.r2dbc.gaussdb.util.Assert;
import io.r2dbc.gaussdb.util.ByteBufUtils;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.Collections;

import static io.r2dbc.gaussdb.codec.GaussDBObjectId.REF_CURSOR;

final class RefCursorCodec extends AbstractCodec<RefCursor> {

    static final RefCursorCodec INSTANCE = new RefCursorCodec();

    RefCursorCodec() {
        super(RefCursor.class);
    }

    @Override
    public EncodedParameter encodeNull() {
        throw new UnsupportedOperationException("RefCursor cannot be encoded");
    }

    @Override
    public Iterable<GaussDBTypeIdentifier> getDataTypes() {
        return Collections.singleton(REF_CURSOR);
    }

    @Override
    boolean doCanDecode(GaussDBObjectId type, Format format) {
        Assert.requireNonNull(format, "format must not be null");
        Assert.requireNonNull(type, "type must not be null");

        return REF_CURSOR == type;
    }

    @Override
    RefCursor doDecode(ByteBuf buffer, GaussDBTypeIdentifier dataType, @Nullable Format format, @Nullable Class<? extends RefCursor> type) {
        Assert.requireNonNull(buffer, "byteBuf must not be null");

        return new SimpleRefCursor(ByteBufUtils.decode(buffer));
    }

    @Override
    EncodedParameter doEncode(RefCursor value) {
        throw new UnsupportedOperationException("RefCursor cannot be encoded");
    }

    @Override
    EncodedParameter doEncode(RefCursor value, GaussDBTypeIdentifier dataType) {
        throw new UnsupportedOperationException("RefCursor cannot be encoded");
    }

    static class SimpleRefCursor implements RefCursor {

        private final String portal;

        SimpleRefCursor(String portal) {
            this.portal = portal;
        }

        @Override
        public String getCursorName() {
            return this.portal;
        }

        @Override
        public Mono<GaussDBResult> fetch() {
            throw new UnsupportedOperationException("Stateless RefCursor does not support fetch()");
        }

        @Override
        public Mono<Void> close() {
            throw new UnsupportedOperationException("Stateless RefCursor does not support close()");
        }

        @Override
        public String toString() {
            return "SimpleRefCursor{" +
                "portal='" + this.portal + '\'' +
                '}';
        }

    }

}
