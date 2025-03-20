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
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.r2dbc.gaussdb.codec.GaussDBObjectId.INT2;
import static io.r2dbc.gaussdb.codec.GaussDBObjectId.INT2_ARRAY;
import static io.r2dbc.gaussdb.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArrayCodec<Short>}.
 */
final class ShortArrayCodecUnitTests extends AbstractArrayCodecUnitTests<Short> {

    private final ByteBuf SINGLE_DIM_BINARY_ARRAY = TEST
        .buffer()
        .writeInt(1)
        .writeInt(0)
        .writeInt(21)
        .writeInt(2)
        .writeInt(2)
        .writeInt(2)
        .writeShort(100)
        .writeInt(2)
        .writeShort(200);

    private final ByteBuf TWO_DIM_BINARY_ARRAY = TEST
        .buffer()
        .writeInt(2) // num of dims
        .writeInt(1) // flag: has nulls
        .writeInt(21) // oid
        .writeInt(2) // dim 1 length
        .writeInt(1) // dim 1 lower bound
        .writeInt(1) // dim 2 length
        .writeInt(1) // dim 2 lower bound
        .writeInt(2) // length of element
        .writeShort(100) // value
        .writeInt(-1); // length of null element

    @Override
    ArrayCodec<Short> createInstance() {
        return new ArrayCodec<>(TEST, INT2_ARRAY, new ShortCodec(TEST), Short.class);
    }

    @Override
    GaussDBObjectId getPostgresqlObjectId() {
        return INT2;
    }

    @Override
    GaussDBObjectId getArrayPostgresqlObjectId() {
        return INT2_ARRAY;
    }

    @Override
    ByteBuf getSingleDimensionBinaryArray() {
        return SINGLE_DIM_BINARY_ARRAY;
    }

    @Override
    ByteBuf getTwoDimensionBinaryArray() {
        return TWO_DIM_BINARY_ARRAY;
    }

    @Override
    Class<? extends Short[]> getSingleDimensionArrayType() {
        return Short[].class;
    }

    @Override
    Class<? extends Short[][]> getTwoDimensionArrayType() {
        return Short[][].class;
    }

    @Override
    Short[] getExpectedSingleDimensionArray() {
        return new Short[]{100, 200};
    }

    @Override
    Short[][] getExpectedTwoDimensionArray() {
        return new Short[][]{{100}, {null}};
    }

    @Override
    String getSingleDimensionStringInput() {
        return "{100,200}";
    }

    @Override
    String getTwoDimensionStringInput() {
        return "{{100},{NULL}}";
    }

    @Test
    void canEncode() {
        assertThat(codec.canEncode(new Short[0])).isTrue();
        assertThat(codec.canEncode(new UUID[0])).isFalse();
    }

    @Test
    void canEncodeNull() {
        assertThat(codec.canEncodeNull(Short[].class)).isTrue();
        assertThat(codec.canEncodeNull(UUID[].class)).isFalse();
    }

}
