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
import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.gaussdb.client.EncodedParameter;
import io.r2dbc.gaussdb.message.Format;
import io.r2dbc.gaussdb.util.Assert;
import io.r2dbc.gaussdb.util.ByteBufUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Support class for codecs that read/write geometry values.
 *
 * @param <T> the type that is handled by this {@link Codec}
 * @since 0.8.5
 */
abstract class AbstractGeometryCodec<T> extends AbstractCodec<T> implements ArrayCodecDelegate<T> {

    protected final GaussDBObjectId gaussDBObjectId;

    protected final ByteBufAllocator byteBufAllocator;

    AbstractGeometryCodec(Class<T> type, GaussDBObjectId gaussDBObjectId, ByteBufAllocator byteBufAllocator) {
        super(type);
        this.gaussDBObjectId = Assert.requireNonNull(gaussDBObjectId, "GaussDBObjectId must not be null");
        this.byteBufAllocator = Assert.requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");
    }

    @Override
    boolean doCanDecode(GaussDBObjectId type, Format format) {
        Assert.requireNonNull(type, "type must not be null");
        Assert.requireNonNull(format, "format must not be null");

        return this.gaussDBObjectId == type;
    }

    @Override
    T doDecode(ByteBuf buffer, GaussDBTypeIdentifier dataType, Format format, Class<? extends T> type) {
        Assert.requireNonNull(buffer, "byteBuf must not be null");
        Assert.requireNonNull(type, "type must not be null");
        Assert.requireNonNull(format, "format must not be null");

        if (format == Format.FORMAT_BINARY) {
            return doDecodeBinary(buffer);
        }

        return doDecodeText(ByteBufUtils.decode(buffer));
    }

    /**
     * Decode value using binary format.
     *
     * @param byteBuffer buffer containing the binary representation of the value to decode
     * @return decoded value
     */
    abstract T doDecodeBinary(ByteBuf byteBuffer);

    /**
     * Decode value using text format.
     *
     * @param text string containing the textual representation of the value to decode
     * @return decoded value
     */
    abstract T doDecodeText(String text);

    @Override
    EncodedParameter doEncode(T value) {
        return doEncode(value, this.gaussDBObjectId);
    }

    @Override
    EncodedParameter doEncode(T value, GaussDBTypeIdentifier dataType) {
        Assert.requireNonNull(value, "value must not be null");

        return create(Format.FORMAT_BINARY, dataType, () -> doEncodeBinary(value));
    }

    /**
     * Encode the value using binary format.
     *
     * @param value the value to encode
     * @return encoded value
     */
    abstract ByteBuf doEncodeBinary(T value);

    @Override
    public String encodeToText(T value) {
        return "\"" + value + "\"";
    }

    @Override
    public EncodedParameter encodeNull() {
        return createNull(Format.FORMAT_BINARY, this.gaussDBObjectId);
    }

    @Override
    public Iterable<GaussDBTypeIdentifier> getDataTypes() {
        return Collections.singleton(this.gaussDBObjectId);
    }

    /**
     * Create a {@link TokenStream} given {@code content}.
     *
     * @param content the textual representation
     * @return a {@link TokenStream} providing access to a tokenized form of the data structure
     */
    TokenStream getTokenStream(String content) {

        List<String> tokens = tokenizeTextData(content);

        return new TokenStream() {

            int position = 0;

            @Override
            public boolean hasNext() {
                return tokens.size() > this.position;
            }

            @Override
            public String next() {

                if (hasNext()) {
                    return tokens.get(this.position++);
                }

                throw new IllegalStateException(String.format("No token available at index %d. Current tokens are: %s", this.position, tokens));
            }

            @Override
            public double nextDouble() {
                return Double.parseDouble(next());
            }

        };
    }

    /**
     * Remove token wrappers such as {@code <>}, {@code []}, {@code {}}, {@code ()}, {@code <spaces>} and extract tokens into a {@link List}.
     *
     * @param content the content to decode.
     * @return tokenized content.
     */
    private static List<String> tokenizeTextData(String content) {

        List<String> tokens = new ArrayList<>();

        int length = content.length();
        for (int i = 0, tokenStart = 0; i < length; i++) {

            char c = content.charAt(i);
            if (c == '(' || c == '[' || c == '<' || c == '{' || (c == ' ' && tokenStart >= i)) {
                tokenStart++;
            } else if (c == ',' || c == ')' || c == ']' || c == '>' || c == '}' || c == ' ') {
                if (tokenStart != i) {
                    tokens.add(content.substring(tokenStart, i));
                    tokenStart = i + 1;
                } else {
                    tokenStart++;
                }
            } else if (i == length - 1) {
                // for cases where there is no token at the end of the string (i.e. "(1.2,123.1),10")
                tokens.add(content.substring(tokenStart));
            }
        }

        return tokens;
    }

    /**
     * Stream of tokens that represent individual components of the underlying data structure.
     */
    interface TokenStream extends Iterator<String> {

        /**
         * Advance the stream and return the next token.
         *
         * @return the token
         * @throws IllegalStateException if the token stream is exhausted
         * @see #hasNext()
         */
        @Override
        String next();

        /**
         * Advance the stream and return the next token as {@code double} value.
         *
         * @return the token
         * @throws IllegalStateException if the token stream is exhausted
         * @see #hasNext()
         */
        double nextDouble();

    }

}
