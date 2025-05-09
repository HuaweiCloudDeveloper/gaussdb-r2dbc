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

package io.r2dbc.gaussdb.message.frontend;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.gaussdb.util.Assert;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.Objects;

import static io.r2dbc.gaussdb.message.frontend.FrontendMessageUtils.MESSAGE_OVERHEAD;
import static io.r2dbc.gaussdb.message.frontend.FrontendMessageUtils.writeByte;
import static io.r2dbc.gaussdb.message.frontend.FrontendMessageUtils.writeBytes;
import static io.r2dbc.gaussdb.message.frontend.FrontendMessageUtils.writeLengthPlaceholder;
import static io.r2dbc.gaussdb.message.frontend.FrontendMessageUtils.writeSize;

/**
 * The SASLResponse message.
 */
public final class SASLResponse implements FrontendMessage {

    private final ByteBuffer data;

    /**
     * Create a new message.
     *
     * @param data SASL mechanism specific message data
     * @throws IllegalArgumentException if {@code data} is {@code null}
     */
    public SASLResponse(ByteBuffer data) {
        Assert.requireNonNull(data, "data must not be null");
        data.flip();
        this.data = data;
    }

    @Override
    public Publisher<ByteBuf> encode(ByteBufAllocator byteBufAllocator) {
        Assert.requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");

        return Mono.fromSupplier(() -> {
            ByteBuf out = byteBufAllocator.ioBuffer(MESSAGE_OVERHEAD + this.data.remaining());

            writeByte(out, 'p');
            writeLengthPlaceholder(out);
            writeBytes(out, this.data);

            return writeSize(out);
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SASLResponse that = (SASLResponse) o;
        return Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.data);
    }

    @Override
    public String toString() {
        return "SASLResponse{" +
            "data=" + this.data +
            '}';
    }

}
