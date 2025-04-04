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
import reactor.util.annotation.Nullable;

import java.util.UUID;

import static io.r2dbc.gaussdb.message.Format.FORMAT_BINARY;

final class UuidCodec extends BuiltinCodecSupport<UUID> {

    UuidCodec(ByteBufAllocator byteBufAllocator) {
        super(UUID.class, byteBufAllocator, GaussDBObjectId.UUID, GaussDBObjectId.UUID_ARRAY, UUID::toString);
    }

    @Override
    UUID doDecode(ByteBuf buffer, GaussDBTypeIdentifier dataType, @Nullable Format format, @Nullable Class<? extends UUID> type) {
        Assert.requireNonNull(buffer, "byteBuf must not be null");

        if (format == FORMAT_BINARY) {
            return new UUID(buffer.readLong(), buffer.readLong());
        }

        String str = ByteBufUtils.decode(buffer);
        return UUID.fromString(str);
    }

}
