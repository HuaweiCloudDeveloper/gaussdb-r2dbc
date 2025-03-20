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

package io.r2dbc.gaussdb.codec;

import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.gaussdb.client.EncodedParameter;
import io.r2dbc.gaussdb.message.Format;
import io.r2dbc.gaussdb.util.Assert;
import io.r2dbc.gaussdb.util.ByteBufUtils;

import java.util.Collections;
import java.util.function.Function;

import static io.r2dbc.gaussdb.message.Format.FORMAT_TEXT;

/**
 * Base class for typical built-in codecs that support to-text encoding for singular values and array items through {@link ArrayCodecDelegate}.
 */
abstract class BuiltinCodecSupport<T> extends AbstractCodec<T> implements ArrayCodecDelegate<T> {

    private final ByteBufAllocator byteBufAllocator;

    private final GaussDBObjectId gaussDBType;

    private final GaussDBObjectId gaussDBArrayType;

    private final Function<T, String> toTextEncoder;

    /**
     * Create a new {@link BuiltinCodecSupport}.
     *
     * @param type              the type handled by this codec.
     * @param byteBufAllocator  allocator
     * @param gaussDBType      GaussDB OID for singular values
     * @param gaussDBArrayType GaussDB array type OID variant of {@code gaussDBType}
     * @param toTextEncoder     function to encode a value to {@link String}
     */
    BuiltinCodecSupport(Class<T> type, ByteBufAllocator byteBufAllocator, GaussDBObjectId gaussDBType, GaussDBObjectId gaussDBArrayType, Function<T, String> toTextEncoder) {
        super(type);
        this.byteBufAllocator = Assert.requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");
        this.gaussDBType = Assert.requireNonNull(gaussDBType, "gaussDBType must not be null");
        this.gaussDBArrayType = Assert.requireNonNull(gaussDBArrayType, "gaussDBArrayType must not be null");
        this.toTextEncoder = Assert.requireNonNull(toTextEncoder, "toTextEncoder must not be null");
    }

    @Override
    boolean doCanDecode(GaussDBObjectId type, Format format) {
        Assert.requireNonNull(type, "type must not be null");

        return this.gaussDBType == type;
    }

    @Override
    final EncodedParameter doEncode(T value) {
        return doEncode(value, this.gaussDBType);
    }

    @Override
    final EncodedParameter doEncode(T value, GaussDBTypeIdentifier dataType) {
        Assert.requireNonNull(value, "value must not be null");

        return create(FORMAT_TEXT, dataType, () -> ByteBufUtils.encode(this.byteBufAllocator, encodeToText(value)));
    }

    @Override
    public final String encodeToText(T value) {
        Assert.requireNonNull(value, "value must not be null");

        return this.toTextEncoder.apply(value);
    }

    @Override
    public final EncodedParameter encodeNull() {
        return createNull(FORMAT_TEXT, this.gaussDBType);
    }

    @Override
    public Iterable<GaussDBTypeIdentifier> getDataTypes() {
        return Collections.singleton(this.gaussDBType);
    }

    @Override
    public final GaussDBTypeIdentifier getArrayDataType() {
        return this.gaussDBArrayType;
    }

}
