/*
 * Copyright 2020 the original author or authors.
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
import io.r2dbc.gaussdb.client.EncodedParameter;
import io.r2dbc.gaussdb.client.ParameterAssert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.r2dbc.gaussdb.client.EncodedParameter.NULL_VALUE;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.LINE;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.PATH;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.POINT;
import static io.r2dbc.gaussdb.message.Format.FORMAT_BINARY;
import static io.r2dbc.gaussdb.message.Format.FORMAT_TEXT;
import static io.r2dbc.gaussdb.util.ByteBufUtils.encode;
import static io.r2dbc.gaussdb.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link PathCodec}.
 */
final class PathCodecUnitTests {

    private static final int dataType = PATH.getObjectId();

    @Test
    void constructorNoByteBufAllocator() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PathCodec(null))
            .withMessage("byteBufAllocator must not be null");
    }

    @Test
    void decode() {
        Path closedPath = Path.closed(Point.of(-10.42, 3.14), Point.of(10.42, -3.14));
        Path openPath = Path.open(Point.of(-10.42, 3.14), Point.of(10.42, -3.14));

        assertThat(new PathCodec(TEST).decode(encode(TEST, "((-10.42,3.14),(10.42,-3.14))"), dataType, FORMAT_TEXT, Path.class)).isEqualTo(closedPath);
        assertThat(new PathCodec(TEST).decode(encode(TEST, "[(-10.42,3.14),(10.42,-3.14)]"), dataType, FORMAT_TEXT, Path.class)).isEqualTo(openPath);

        ByteBuf closedPathByteFormat = TEST.buffer(37)
            .writeBoolean(true)
            .writeInt(2)
            .writeDouble(-10.42)
            .writeDouble(3.14)
            .writeDouble(10.42)
            .writeDouble(-3.14);
        assertThat(new PathCodec(TEST).decode(closedPathByteFormat, dataType, FORMAT_BINARY, Path.class)).isEqualTo(closedPath);
        ByteBuf openPathByteFormat = TEST.buffer(37)
            .writeBoolean(false)
            .writeInt(2)
            .writeDouble(-10.42)
            .writeDouble(3.14)
            .writeDouble(10.42)
            .writeDouble(-3.14);
        assertThat(new PathCodec(TEST).decode(openPathByteFormat, dataType, FORMAT_BINARY, Path.class)).isEqualTo(openPath);
    }

    @Test
    void decodeNoByteBuf() {
        assertThat(new PathCodec(TEST).decode(null, dataType, FORMAT_TEXT, Path.class)).isNull();
    }

    @Test
    void doCanDecode() {
        PathCodec codec = new PathCodec(TEST);
        assertThat(codec.doCanDecode(PATH, FORMAT_BINARY)).isTrue();
        assertThat(codec.doCanDecode(PATH, FORMAT_TEXT)).isTrue();
        assertThat(codec.doCanDecode(LINE, FORMAT_TEXT)).isFalse();
        assertThat(codec.doCanDecode(POINT, FORMAT_TEXT)).isFalse();
    }

    @Test
    void doCanDecodeNoFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PathCodec(TEST).doCanDecode(PATH, null))
            .withMessage("format must not be null");
    }

    @Test
    void doCanDecodeNoType() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PathCodec(TEST).doCanDecode(null, FORMAT_TEXT))
            .withMessage("type must not be null");
    }

    @Test
    void doEncode() {
        Path closedPath = Path.closed(Arrays.asList(Point.of(-10.42, 3.14), Point.of(10.42, -3.14)));
        ParameterAssert.assertThat(new PathCodec(TEST).doEncode(closedPath))
            .hasFormat(FORMAT_BINARY)
            .hasType(dataType)
            .hasValue(TEST.buffer(37)
                .writeBoolean(true)
                .writeInt(2)
                .writeDouble(-10.42)
                .writeDouble(3.14)
                .writeDouble(10.42)
                .writeDouble(-3.14)
            );

        Path openPath = Path.open(Arrays.asList(Point.of(-10.42, 3.14), Point.of(10.42, -3.14)));
        ParameterAssert.assertThat(new PathCodec(TEST).doEncode(openPath))
            .hasFormat(FORMAT_BINARY)
            .hasType(dataType)
            .hasValue(TEST.buffer(37)
                .writeBoolean(false)
                .writeInt(2)
                .writeDouble(-10.42)
                .writeDouble(3.14)
                .writeDouble(10.42)
                .writeDouble(-3.14)
            );
    }

    @Test
    void doEncodeNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PathCodec(TEST).doEncode(null))
            .withMessage("value must not be null");
    }

    @Test
    void encodeNull() {
        ParameterAssert.assertThat(new PathCodec(TEST).encodeNull())
            .isEqualTo(new EncodedParameter(FORMAT_BINARY, dataType, NULL_VALUE));
    }

}
