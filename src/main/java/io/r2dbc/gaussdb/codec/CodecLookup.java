/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.gaussdb.codec;

import io.r2dbc.gaussdb.message.Format;
import reactor.util.annotation.Nullable;

/**
 * Helper used to find a {@link Codec} for encoding or decoding actions from a provided collection of codecs such as a {@link CodecRegistry}.
 *
 * @since 0.9
 */
interface CodecLookup extends Iterable<Codec<?>> {

    @Nullable
    <T> Codec<T> findDecodeCodec(int dataType, Format format, Class<? extends T> type);

    @Nullable
    <T> Codec<T> findEncodeCodec(T value);

    @Nullable
    <T> Codec<T> findEncodeNullCodec(Class<T> type);

    /**
     * Hook called after a codec was added.
     */
    default void afterCodecAdded() {

    }

}
