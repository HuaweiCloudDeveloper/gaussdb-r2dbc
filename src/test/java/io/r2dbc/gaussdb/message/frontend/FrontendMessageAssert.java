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
import io.netty.buffer.ByteBufUtil;
import org.assertj.core.api.AbstractObjectAssert;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;
import java.util.function.Function;

import static io.r2dbc.gaussdb.util.TestByteBufAllocator.TEST;

/**
 * Assertions for {@link FrontendMessage}.
 */
final class FrontendMessageAssert extends AbstractObjectAssert<FrontendMessageAssert, FrontendMessage> {

    private FrontendMessageAssert(FrontendMessage actual) {
        super(actual, FrontendMessageAssert.class);
    }

    static FrontendMessageAssert assertThat(FrontendMessage actual) {
        return new FrontendMessageAssert(actual);
    }

    EncodedAssert encoded() {
        return new EncodedAssert(this.actual.encode(TEST));
    }

    static final class EncodedAssert extends AbstractObjectAssert<EncodedAssert, Publisher<ByteBuf>> {

        private static final Class<?> MONO_SUPPLIER;

        static {
            try {
                MONO_SUPPLIER = Class.forName("reactor.core.publisher.MonoSupplier");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private EncodedAssert(Publisher<ByteBuf> actual) {
            super(actual, EncodedAssert.class);
        }

        EncodedAssert isDeferred() {
            isNotNull();
            isInstanceOf(MONO_SUPPLIER);

            return this;
        }

        EncodedAssert isEncodedAs(Function<ByteBuf, ByteBuf> encoded) {
            isNotNull();

            Mono.from(this.actual)
                .as(StepVerifier::create)
                .consumeNextWith(actual -> {
                    ByteBuf expected = encoded.apply(TEST.buffer());

                    if (!Objects.deepEquals(actual, expected)) {
                        failWithMessage("\nExpected:\n%s\nActual:\n%s", ByteBufUtil.prettyHexDump(expected), ByteBufUtil.prettyHexDump(actual));
                    }
                })
                .verifyComplete();

            return this;
        }

    }

}
