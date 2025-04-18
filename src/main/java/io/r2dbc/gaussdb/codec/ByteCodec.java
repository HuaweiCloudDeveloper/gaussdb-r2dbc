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
import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.gaussdb.client.EncodedParameter;
import io.r2dbc.gaussdb.message.Format;
import io.r2dbc.gaussdb.util.Assert;
import reactor.util.annotation.Nullable;

final class ByteCodec extends AbstractCodec<Byte> implements ArrayCodecDelegate<Byte>, PrimitiveWrapperCodecProvider<Byte> {

    private final ShortCodec delegate;

    ByteCodec(ByteBufAllocator byteBufAllocator) {
        super(Byte.class);

        Assert.requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");
        this.delegate = new ShortCodec(byteBufAllocator);
    }

    @Override
    public PrimitiveCodec<Byte> getPrimitiveCodec() {
        return new PrimitiveCodec<>(Byte.TYPE, Byte.class, this);
    }

    @Override
    public EncodedParameter encodeNull() {
        return this.delegate.encodeNull();
    }

    @Override
    boolean doCanDecode(GaussDBObjectId type, @Nullable Format format) {
        Assert.requireNonNull(type, "type must not be null");

        return this.delegate.doCanDecode(type, format);
    }

    @Override
    Byte doDecode(ByteBuf buffer, GaussDBTypeIdentifier dataType, Format format, @Nullable Class<? extends Byte> type) {
        Assert.requireNonNull(buffer, "byteBuf must not be null");
        Assert.requireNonNull(format, "format must not be null");

        return this.delegate.doDecode(buffer, dataType, format, Short.class).byteValue();
    }

    @Override
    EncodedParameter doEncode(Byte value) {
        Assert.requireNonNull(value, "value must not be null");

        return this.delegate.doEncode((short) value);
    }

    @Override
    EncodedParameter doEncode(Byte value, GaussDBTypeIdentifier dataType) {
        return this.delegate.doEncode((short) value, dataType);
    }

    @Override
    public String encodeToText(Byte value) {
        Assert.requireNonNull(value, "value must not be null");

        return value.toString();
    }

    @Override
    public GaussDBTypeIdentifier getArrayDataType() {
        return GaussDBObjectId.INT2_ARRAY;
    }

    @Override
    public Iterable<Format> getFormats() {
        return this.delegate.getFormats();
    }

    @Override
    public Iterable<? extends GaussDBTypeIdentifier> getDataTypes() {
        return this.delegate.getDataTypes();
    }

}
