/*
 * Copyright 2019 the original author or authors.
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
import reactor.util.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import static io.r2dbc.gaussdb.codec.GaussDBObjectId.DATE;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.TIME;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.TIMESTAMP;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.TIMESTAMPTZ;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.TIMETZ;
import static io.r2dbc.gaussdb.message.Format.FORMAT_BINARY;

/**
 * Codec to decode all known temporal types.
 *
 * @param <T> the type that is handled by this {@link Codec}
 */
abstract class AbstractTemporalCodec<T extends Temporal> extends BuiltinCodecSupport<T> {

    private static final Set<GaussDBObjectId> SUPPORTED_TYPES = EnumSet.of(DATE, TIMESTAMP, TIMESTAMPTZ, TIME, TIMETZ);

    private final GaussDBObjectId gaussDBType;

    /**
     * Create a new {@link AbstractTemporalCodec}.
     *
     * @param type the type handled by this codec
     */
    AbstractTemporalCodec(Class<T> type, ByteBufAllocator byteBufAllocator, GaussDBObjectId gaussDBType, GaussDBObjectId gaussDBArrayType, Function<T, String> toTextEncoder) {
        super(type, byteBufAllocator, gaussDBType, gaussDBArrayType, toTextEncoder);
        this.gaussDBType = gaussDBType;
    }

    @Override
    public boolean canDecode(int dataType, Format format, Class<?> type) {
        Assert.requireNonNull(format, "format must not be null");
        Assert.requireNonNull(type, "type must not be null");

        if (type == Object.class) {
            if (GaussDBObjectId.isValid(dataType) && GaussDBObjectId.valueOf(dataType) != getDefaultType()) {
                return false;
            }
        }
        return super.canDecode(dataType, format, type);
    }

    @Override
    final boolean doCanDecode(GaussDBObjectId type, Format format) {
        Assert.requireNonNull(format, "format must not be null");
        Assert.requireNonNull(type, "type must not be null");

        return SUPPORTED_TYPES.contains(type);
    }

    /**
     * Decode {@code buffer} to {@link Temporal} and potentially convert it to {@link Class expectedType} using {@link Function converter} if the decoded type does not match {@code expectedType}.
     *
     * @param buffer       the buffer
     * @param dataType     the GaussDB type
     * @param format       value format
     * @param expectedType expected result type
     * @param converter    converter to convert from {@link Temporal} into {@code expectedType}
     * @return the decoded value
     */
    T decodeTemporal(ByteBuf buffer, GaussDBTypeIdentifier dataType, @Nullable Format format, Class<T> expectedType, Function<Temporal, T> converter) {
        Temporal number = decodeTemporal(buffer, GaussDBObjectId.from(dataType), format);
        return potentiallyConvert(number, expectedType, converter);
    }

    /**
     * Decode {@code buffer} to {@link Temporal} according to {@link GaussDBObjectId}.
     *
     * @param buffer   the buffer
     * @param dataType the GaussDB type
     * @param format   value format
     * @return the decoded value
     */
    private Temporal decodeTemporal(ByteBuf buffer, GaussDBObjectId dataType, @Nullable Format format) {
        Assert.requireNonNull(buffer, "byteBuf must not be null");

        switch (dataType) {

            case TIMESTAMP:
            case TIMESTAMP_ARRAY:
                if (FORMAT_BINARY == format) {
                    return EpochTime.fromLong(buffer.readLong()).toLocalDateTime();
                }

                return GaussDBDateTimeFormatter.parseLocalDateTime(ByteBufUtils.decode(buffer));
            case DATE:
            case DATE_ARRAY:
                if (FORMAT_BINARY == format) {
                    return LocalDate.ofEpochDay(EpochTime.fromInt(buffer.readInt()).getJavaDays());
                }

                return GaussDBDateTimeFormatter.parseLocalDate(ByteBufUtils.decode(buffer));
            case TIME:
            case TIME_ARRAY:
                if (FORMAT_BINARY == format) {
                    return LocalTime.ofNanoOfDay(buffer.readLong() * 1000);
                }

                return GaussDBTimeFormatter.parseLocalTime(ByteBufUtils.decode(buffer));
            case TIMESTAMPTZ:
            case TIMESTAMPTZ_ARRAY:
                if (FORMAT_BINARY == format) {
                    return EpochTime.fromLong(buffer.readLong()).toInstant().atOffset(OffsetDateTime.now().getOffset());
                }

                return GaussDBDateTimeFormatter.parseOffsetDateTime(ByteBufUtils.decode(buffer));
            case TIMETZ:
            case TIMETZ_ARRAY:
                if (FORMAT_BINARY == format) {
                    long timeNano = buffer.readLong() * 1000;
                    int offsetSec = -buffer.readInt();
                    return OffsetTime.of(LocalTime.ofNanoOfDay(timeNano), ZoneOffset.ofTotalSeconds(offsetSec));
                }

                return GaussDBTimeFormatter.parseOffsetTime(ByteBufUtils.decode(buffer));
        }

        throw new UnsupportedOperationException(String.format("Cannot decode value for type %s, format %s", dataType, format));
    }

    /**
     * Returns the {@link GaussDBObjectId} for to identify whether this codec is the default codec.
     *
     * @return the {@link GaussDBObjectId} for to identify whether this codec is the default codec
     */
    @Nullable
    GaussDBObjectId getDefaultType() {
        return this.gaussDBType;
    }

    static <T> T potentiallyConvert(Temporal temporal, Class<T> expectedType, Function<Temporal, T> converter) {
        return expectedType.isInstance(temporal) ? expectedType.cast(temporal) : converter.apply(temporal);
    }

}
