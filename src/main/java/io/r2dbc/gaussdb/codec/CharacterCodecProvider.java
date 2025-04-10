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

final class CharacterCodecProvider extends AbstractCodec<Character> implements ArrayCodecDelegate<Character>, PrimitiveWrapperCodecProvider<Character> {

    private final StringCodec delegate;

    CharacterCodecProvider(ByteBufAllocator byteBufAllocator) {
        super(Character.class);

        Assert.requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");
        this.delegate = new StringCodec(byteBufAllocator);
    }

    @Override
    public PrimitiveCodec<Character> getPrimitiveCodec() {
        return new PrimitiveCodec<>(Character.TYPE, Character.class, this);
    }

    @Override
    public EncodedParameter encodeNull() {
        return this.delegate.encodeNull();
    }

    @Override
    boolean doCanDecode(GaussDBObjectId type, Format format) {
        Assert.requireNonNull(format, "format must not be null");
        Assert.requireNonNull(type, "type must not be null");

        return this.delegate.doCanDecode(type, format);
    }

    @Override
    Character doDecode(ByteBuf buffer, GaussDBTypeIdentifier dataType, @Nullable Format format, @Nullable Class<? extends Character> type) {
        Assert.requireNonNull(buffer, "byteBuf must not be null");

        return this.delegate.doDecode(buffer, dataType, format, String.class).charAt(0);
    }

    @Override
    EncodedParameter doEncode(Character value) {
        Assert.requireNonNull(value, "value must not be null");

        return this.delegate.doEncode(value.toString());
    }

    @Override
    EncodedParameter doEncode(Character value, GaussDBTypeIdentifier dataType) {
        Assert.requireNonNull(value, "value must not be null");

        return this.delegate.doEncode(value.toString(), dataType);
    }

    @Override
    public String encodeToText(Character value) {
        Assert.requireNonNull(value, "value must not be null");

        return value.toString();
    }

    @Override
    public GaussDBTypeIdentifier getArrayDataType() {
        return GaussDBObjectId.CHAR_ARRAY;
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
