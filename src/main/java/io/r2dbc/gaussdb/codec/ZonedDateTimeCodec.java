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
import io.r2dbc.gaussdb.message.Format;
import io.r2dbc.gaussdb.util.Assert;
import reactor.util.annotation.Nullable;

import java.time.ZonedDateTime;

import static io.r2dbc.gaussdb.codec.GaussDBObjectId.TIMESTAMPTZ;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.TIMESTAMPTZ_ARRAY;

final class ZonedDateTimeCodec extends AbstractTemporalCodec<ZonedDateTime> implements ArrayCodecDelegate<ZonedDateTime> {

    ZonedDateTimeCodec(ByteBufAllocator byteBufAllocator) {
        super(ZonedDateTime.class, byteBufAllocator, TIMESTAMPTZ, TIMESTAMPTZ_ARRAY, value -> GaussDBDateTimeFormatter.toString(value.toOffsetDateTime()));
    }

    @Override
    ZonedDateTime doDecode(ByteBuf buffer, GaussDBTypeIdentifier dataType, @Nullable Format format, Class<? extends ZonedDateTime> type) {
        Assert.requireNonNull(buffer, "byteBuf must not be null");

        return decodeTemporal(buffer, dataType, format, ZonedDateTime.class, ZonedDateTime::from);
    }

    // Avoid defaulting, use OffsetDateTime as default instead.
    @Override
    GaussDBObjectId getDefaultType() {
        return null;
    }

}
