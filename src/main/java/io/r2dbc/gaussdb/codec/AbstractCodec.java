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
import io.r2dbc.gaussdb.client.EncodedParameter;
import io.r2dbc.gaussdb.message.Format;
import io.r2dbc.gaussdb.util.Assert;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.function.Supplier;

import static io.r2dbc.gaussdb.client.EncodedParameter.NULL_VALUE;

/**
 * Abstract codec class that provides a basis for all concrete
 * implementations of a {@link Codec} for well-known {@link GaussDBObjectId}.
 *
 * @param <T> the type that is handled by this {@link Codec}
 */
abstract class AbstractCodec<T> implements Codec<T>, CodecMetadata {

    private final Class<T> type;

    /**
     * Create a new {@link AbstractCodec}.
     *
     * @param type the type handled by this codec
     */
    AbstractCodec(Class<T> type) {
        this.type = Assert.requireNonNull(type, "type must not be null");
    }

    @Override
    public boolean canDecode(int dataType, Format format, Class<?> type) {
        Assert.requireNonNull(format, "format must not be null");
        Assert.requireNonNull(type, "type must not be null");

        return GaussDBObjectId.isValid(dataType) && (type == Object.class || isTypeAssignable(type)) &&
            doCanDecode(GaussDBObjectId.valueOf(dataType), format);
    }

    @Override
    public boolean canEncode(Object value) {
        Assert.requireNonNull(value, "value must not be null");

        return this.type.isInstance(value);
    }

    @Override
    public boolean canEncodeNull(Class<?> type) {
        Assert.requireNonNull(type, "type must not be null");

        return this.type.isAssignableFrom(type);
    }

    @Nullable
    @Override
    public final T decode(@Nullable ByteBuf buffer, int dataType, Format format, Class<? extends T> type) {
        if (buffer == null) {
            return null;
        }

        return doDecode(buffer, getDataType(dataType), format, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final EncodedParameter encode(Object value) {
        Assert.requireNonNull(value, "value must not be null");

        return doEncode((T) value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EncodedParameter encode(Object value, int dataType) {
        Assert.requireNonNull(value, "value must not be null");

        return doEncode((T) value, getDataType(dataType));
    }

    public static GaussDBTypeIdentifier getDataType(int dataType) {
        return GaussDBObjectId.isValid(dataType) ? GaussDBObjectId.valueOf(dataType) : () -> dataType;
    }

    public EncodedParameter encodeNull(int dataType) {
        return new EncodedParameter(Format.FORMAT_BINARY, dataType, NULL_VALUE);
    }

    @Override
    public Class<?> type() {
        return this.type;
    }

    /**
     * Create a {@link EncodedParameter}.
     *
     * @param format the format to use
     * @param type   the well-known {@link GaussDBObjectId type OID}
     * @param value  {@link Publisher} emitting {@link ByteBuf buffers}
     * @return the encoded  {@link EncodedParameter}
     * @implNote use deferred buffer creation instead of {@link Mono#just(Object)} and {@link Flux#just(Object)} to avoid memory
     * leaks
     */
    static EncodedParameter create(Format format, GaussDBTypeIdentifier type, Publisher<? extends ByteBuf> value) {
        Assert.requireNonNull(type, "type must not be null");
        return new EncodedParameter(format, type.getObjectId(), value);
    }

    /**
     * Create a {@link EncodedParameter}.
     *
     * @param format         the format to use
     * @param type           the well-known {@link GaussDBTypeIdentifier type OID}
     * @param bufferSupplier {@link Supplier} supplying the encoded {@link ByteBuf buffer}
     * @return the encoded  {@link EncodedParameter}
     */
    static EncodedParameter create(Format format, GaussDBTypeIdentifier type, Supplier<? extends ByteBuf> bufferSupplier) {
        Assert.requireNonNull(type, "type must not be null");
        return create(format, type.getObjectId(), bufferSupplier);
    }

    /**
     * Create a {@link EncodedParameter}.
     *
     * @param format         the format to use
     * @param type           the well-known type OID
     * @param bufferSupplier {@link Supplier} supplying the encoded {@link ByteBuf buffer}
     * @return the encoded  {@link EncodedParameter}
     */
    static EncodedParameter create(Format format, int type, Supplier<? extends ByteBuf> bufferSupplier) {
        return new EncodedParameter(format, type, Mono.fromSupplier(bufferSupplier));
    }

    /**
     * Encode a {@code null} value.
     *
     * @param format the data type {@link Format}, text or binary
     * @param type   the well-known {@link GaussDBTypeIdentifier type OID}
     * @return the encoded {@code null} value
     */
    static EncodedParameter createNull(Format format, GaussDBTypeIdentifier type) {
        return create(format, type, NULL_VALUE);
    }

    /**
     * Determine whether this {@link Codec} is capable of decoding column values based on the given {@link Format} and {@link GaussDBTypeIdentifier}.
     *
     * @param type   the well-known {@link GaussDBTypeIdentifier type OID}
     * @param format the data type {@link Format}, text or binary
     * @return {@code true} if this codec is able to decode values of {@link Format} and {@link GaussDBTypeIdentifier}
     */
    abstract boolean doCanDecode(GaussDBObjectId type, Format format);

    /**
     * Decode the {@link ByteBuf data} into the {@link Class value type}.
     *
     * @param buffer   the data buffer
     * @param dataType the well-known {@link GaussDBTypeIdentifier type OID}
     * @param format   data type format
     * @param type     the desired value type
     * @return the decoded value, can be {@code null} if the column value is {@code null}
     */
    abstract T doDecode(ByteBuf buffer, GaussDBTypeIdentifier dataType, Format format, Class<? extends T> type);

    /**
     * Forwarding method to {@link #doDecode(ByteBuf, GaussDBTypeIdentifier, Format, Class)} for subclasses that want to implement {@link ArrayCodecDelegate}.
     *
     * @param buffer   the data buffer
     * @param dataType the well-known {@link GaussDBTypeIdentifier type OID}
     * @param format   data type format
     * @param type     the desired value type
     * @return the decoded value, can be {@code null} if the column value is {@code null}
     */
    public T decode(ByteBuf buffer, GaussDBTypeIdentifier dataType, Format format, Class<? extends T> type) {
        return doDecode(buffer, dataType, format, type);
    }

    /**
     * @param value the  {@code value}
     * @return the encoded value
     */
    abstract EncodedParameter doEncode(T value);

    /**
     * @param value    the  {@code value}
     * @param dataType the GaussDB OID to encode
     * @return the encoded value
     * @since 0.9
     */
    abstract EncodedParameter doEncode(T value, GaussDBTypeIdentifier dataType);

    boolean isTypeAssignable(Class<?> type) {
        Assert.requireNonNull(type, "type must not be null");

        return type.isAssignableFrom(this.type);
    }

}
