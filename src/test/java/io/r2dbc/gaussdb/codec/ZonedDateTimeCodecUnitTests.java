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

import io.r2dbc.gaussdb.client.EncodedParameter;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static io.r2dbc.gaussdb.client.EncodedParameter.NULL_VALUE;
import static io.r2dbc.gaussdb.client.ParameterAssert.assertThat;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.MONEY;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.TIMESTAMPTZ;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.VARCHAR;
import static io.r2dbc.gaussdb.message.Format.FORMAT_BINARY;
import static io.r2dbc.gaussdb.message.Format.FORMAT_TEXT;
import static io.r2dbc.gaussdb.util.ByteBufUtils.encode;
import static io.r2dbc.gaussdb.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ZonedDateTimeCodec}.
 */
final class ZonedDateTimeCodecUnitTests {

    private static final int dataType = TIMESTAMPTZ.getObjectId();

    @Test
    void constructorNoByteBufAllocator() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ZonedDateTimeCodec(null))
            .withMessage("byteBufAllocator must not be null");
    }

    @Test
    void decode() {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2018-11-05T00:20:25.039883+09:00[Asia/Tokyo]");

        assertThat(new ZonedDateTimeCodec(TEST).decode(encode(TEST, "2018-11-05 00:20:25.039883+09:00"), dataType, FORMAT_TEXT, ZonedDateTime.class))
            .isEqualTo(zonedDateTime);
        assertThat(new ZonedDateTimeCodec(TEST).decode(encode(TEST, "2018-11-05 00:20:25.039883+09"), dataType, FORMAT_TEXT, ZonedDateTime.class))
            .isEqualTo(zonedDateTime);
        assertThat(new ZonedDateTimeCodec(TEST).decode(encode(TEST, "2018-11-05 00:20:25.039883+09:00:00"), dataType, FORMAT_TEXT, ZonedDateTime.class))
                .isEqualTo(zonedDateTime);
    }

    @Test
    void decodeUTC() {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2018-11-05T00:20:25.039883+00:00");

        assertThat(new ZonedDateTimeCodec(TEST).decode(encode(TEST, "2018-11-05 00:20:25.039883+00:00"), dataType, FORMAT_TEXT, ZonedDateTime.class))
            .isEqualTo(zonedDateTime);
        assertThat(new ZonedDateTimeCodec(TEST).decode(encode(TEST, "2018-11-05 00:20:25.039883+00"), dataType, FORMAT_TEXT, ZonedDateTime.class))
            .isEqualTo(zonedDateTime);
    }

    @Test
    void decodeNoByteBuf() {
        assertThat(new ZonedDateTimeCodec(TEST).decode(null, dataType, FORMAT_TEXT, ZonedDateTime.class)).isNull();
    }

    @Test
    void doCanDecode() {
        ZonedDateTimeCodec codec = new ZonedDateTimeCodec(TEST);

        assertThat(codec.doCanDecode(TIMESTAMPTZ, FORMAT_BINARY)).isTrue();
        assertThat(codec.doCanDecode(MONEY, FORMAT_TEXT)).isFalse();
        assertThat(codec.doCanDecode(TIMESTAMPTZ, FORMAT_TEXT)).isTrue();
    }

    @Test
    void doCanDecodeNoFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ZonedDateTimeCodec(TEST).doCanDecode(VARCHAR, null))
            .withMessage("format must not be null");
    }

    @Test
    void doCanDecodeNoType() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ZonedDateTimeCodec(TEST).doCanDecode(null, FORMAT_TEXT))
            .withMessage("type must not be null");
    }

    @Test
    void doEncode() {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2023-02-16T15:11:43.356+01:00");

        assertThat(new ZonedDateTimeCodec(TEST).doEncode(zonedDateTime))
            .hasFormat(FORMAT_TEXT)
            .hasType(TIMESTAMPTZ.getObjectId())
            .hasValue(encode(TEST, "2023-02-16 15:11:43.356+01"));
    }

    @Test
    void doEncodeNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ZonedDateTimeCodec(TEST).doEncode(null))
            .withMessage("value must not be null");
    }

    @Test
    void encodeNull() {
        assertThat(new ZonedDateTimeCodec(TEST).encodeNull())
            .isEqualTo(new EncodedParameter(FORMAT_TEXT, TIMESTAMPTZ.getObjectId(), NULL_VALUE));
    }

}
