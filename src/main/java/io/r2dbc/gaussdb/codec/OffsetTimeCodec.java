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

import java.time.OffsetTime;

import static io.r2dbc.gaussdb.codec.GaussDBObjectId.TIMETZ;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.TIMETZ_ARRAY;

public class OffsetTimeCodec extends AbstractTemporalCodec<OffsetTime> {

    OffsetTimeCodec(ByteBufAllocator byteBufAllocator) {
        super(OffsetTime.class, byteBufAllocator, TIMETZ, TIMETZ_ARRAY, GaussDBTimeFormatter::toString);
    }

    @Override
    OffsetTime doDecode(ByteBuf buffer, GaussDBTypeIdentifier dataType, Format format, Class<? extends OffsetTime> type) {
        Assert.requireNonNull(buffer, "byteBuf must not be null");

        return decodeTemporal(buffer, dataType, format, OffsetTime.class, OffsetTime::from);
    }

}
