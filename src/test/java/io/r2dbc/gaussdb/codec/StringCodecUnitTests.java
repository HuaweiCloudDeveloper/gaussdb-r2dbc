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

import static io.r2dbc.gaussdb.client.EncodedParameter.NULL_VALUE;
import static io.r2dbc.gaussdb.client.ParameterAssert.assertThat;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.BPCHAR;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.CHAR;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.MONEY;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.UNKNOWN;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.VARCHAR;
import static io.r2dbc.gaussdb.message.Format.FORMAT_BINARY;
import static io.r2dbc.gaussdb.message.Format.FORMAT_TEXT;
import static io.r2dbc.gaussdb.util.ByteBufUtils.encode;
import static io.r2dbc.gaussdb.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link StringCodec}.
 */
final class StringCodecUnitTests {

    private static final int dataType = VARCHAR.getObjectId();

    @Test
    void constructorNoByteBufAllocator() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StringCodec(null))
            .withMessage("byteBufAllocator must not be null");
    }

    @Test
    void decode() {
        assertThat(new StringCodec(TEST).decode(encode(TEST, "test"), dataType, FORMAT_TEXT, String.class))
            .isEqualTo("test");
    }

    @Test
    void decodeNoByteBuf() {
        assertThat(new StringCodec(TEST).decode(null, dataType, FORMAT_TEXT, String.class)).isNull();
    }

    @Test
    void doCanDecode() {
        StringCodec codec = new StringCodec(TEST);

        assertThat(codec.doCanDecode(VARCHAR, FORMAT_BINARY)).isTrue();
        assertThat(codec.doCanDecode(MONEY, FORMAT_TEXT)).isFalse();
        assertThat(codec.doCanDecode(BPCHAR, FORMAT_TEXT)).isTrue();
        assertThat(codec.doCanDecode(CHAR, FORMAT_TEXT)).isTrue();
        assertThat(codec.doCanDecode(GaussDBObjectId.TEXT, FORMAT_TEXT)).isTrue();
        assertThat(codec.doCanDecode(UNKNOWN, FORMAT_TEXT)).isTrue();
        assertThat(codec.doCanDecode(VARCHAR, FORMAT_TEXT)).isTrue();
    }

    @Test
    void doCanDecodeNoFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StringCodec(TEST).doCanDecode(VARCHAR, null))
            .withMessage("format must not be null");
    }

    @Test
    void doCanDecodeNoType() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StringCodec(TEST).doCanDecode(null, FORMAT_TEXT))
            .withMessage("type must not be null");
    }

    @Test
    void doEncode() {
        String string = "test";

        assertThat(new StringCodec(TEST).doEncode(string))
            .hasFormat(FORMAT_TEXT)
            .hasType(VARCHAR.getObjectId())
            .hasValue(encode(TEST, "test"));
    }

    @Test
    void doEncodeNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StringCodec(TEST).doEncode(null))
            .withMessage("value must not be null");
    }

    @Test
    void encodeItem() {
        assertThat(new StringCodec(TEST).encodeToText("alpha")).isEqualTo("\"alpha\"");
    }

    @Test
    void encodeItemMultibyte() {
        assertThat(new StringCodec(TEST).encodeToText("АБ")).isEqualTo("\"АБ\"");
    }

    @Test
    void encodeItemNULL() {
        assertThat(new StringCodec(TEST).encodeToText("NULL")).isEqualTo("\"NULL\"");
    }

    @Test
    void encodeItemNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StringCodec(TEST).encodeToText(null))
            .withMessage("value must not be null");
    }

    @Test
    void encodeItemWithEscapes() {
        assertThat(new StringCodec(TEST).encodeToText("R \"2\" DBC")).isEqualTo("\"R \\\"2\\\" DBC\"");
    }

    @Test
    void encodeNull() {
        assertThat(new StringCodec(TEST).encodeNull())
            .isEqualTo(new EncodedParameter(FORMAT_TEXT, VARCHAR.getObjectId(), NULL_VALUE));
    }

}
