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
import io.r2dbc.gaussdb.util.ByteBufUtils;
import reactor.util.annotation.Nullable;

import java.math.BigDecimal;

import static io.r2dbc.gaussdb.codec.GaussDBObjectId.NUMERIC;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.NUMERIC_ARRAY;
import static io.r2dbc.gaussdb.message.Format.FORMAT_TEXT;

final class BigDecimalCodec extends AbstractNumericCodec<BigDecimal> {

    private final ByteBufAllocator byteBufAllocator;

    BigDecimalCodec(ByteBufAllocator byteBufAllocator) {
        super(BigDecimal.class, byteBufAllocator);
        this.byteBufAllocator = Assert.requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");
    }

    @Override
    BigDecimal doDecode(ByteBuf buffer, GaussDBTypeIdentifier dataType, @Nullable Format format, @Nullable Class<? extends BigDecimal> type) {
        Assert.requireNonNull(buffer, "byteBuf must not be null");

        return decodeNumber(buffer, dataType, format, BigDecimal.class, it -> new BigDecimal(it.doubleValue()));
    }

    @Override
    EncodedParameter doEncode(BigDecimal value, GaussDBTypeIdentifier dataType) {
        Assert.requireNonNull(value, "value must not be null");

        return create(FORMAT_TEXT, dataType, () -> ByteBufUtils.encode(this.byteBufAllocator, value.toString()));
    }

    @Override
    GaussDBObjectId getDefaultType() {
        return NUMERIC;
    }

    @Override
    public GaussDBTypeIdentifier getArrayDataType() {
        return NUMERIC_ARRAY;
    }

}
