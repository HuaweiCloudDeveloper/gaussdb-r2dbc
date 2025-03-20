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
import io.r2dbc.gaussdb.util.ByteBufUtils;

import java.util.EnumSet;

import static io.r2dbc.gaussdb.codec.GaussDBObjectId.INTERVAL;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.INTERVAL_ARRAY;
import static io.r2dbc.gaussdb.message.Format.FORMAT_TEXT;

final class IntervalCodec extends BuiltinCodecSupport<Interval> {

    IntervalCodec(ByteBufAllocator byteBufAllocator) {
        super(Interval.class, byteBufAllocator, INTERVAL, INTERVAL_ARRAY, Interval::getValue);
    }

    @Override
    boolean doCanDecode(GaussDBObjectId type, Format format) {
        Assert.requireNonNull(type, "type must not be null");
        Assert.requireNonNull(format, "format must not be null");

        return INTERVAL == type && FORMAT_TEXT == format;
    }

    @Override
    Interval doDecode(ByteBuf buffer, GaussDBTypeIdentifier dataType, Format format, Class<? extends Interval> type) {
        return Interval.parse(ByteBufUtils.decode(buffer));
    }

    @Override
    public Iterable<Format> getFormats() {
        return EnumSet.of(FORMAT_TEXT);
    }

}
