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

package io.r2dbc.gaussdb.message.backend;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import io.r2dbc.gaussdb.util.ReferenceCountedCleaner;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ObjectAssert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;

import static io.r2dbc.gaussdb.util.TestByteBufAllocator.TEST;

/**
 * Assertions for {@link BackendMessage}.
 */
final class BackendMessageAssert extends AbstractObjectAssert<BackendMessageAssert, Class<? extends BackendMessage>> {

    private ReferenceCountedCleaner cleaner = new ReferenceCountedCleaner();

    private BackendMessageAssert(Class<? extends BackendMessage> actual) {
        super(actual, BackendMessageAssert.class);
    }

    static BackendMessageAssert assertThat(Class<? extends BackendMessage> actual) {
        return new BackendMessageAssert(actual);
    }

    BackendMessageAssert cleaner(ReferenceCountedCleaner cleaner) {
        this.cleaner = cleaner;
        return this;
    }

    @SuppressWarnings("unchecked")
    <T extends BackendMessage> ObjectAssert<T> decoded(Function<ByteBuf, ByteBuf> decoded) {
        Method method = ReflectionUtils.findMethod(this.actual, "decode", ByteBuf.class);

        if (Objects.isNull(method)) {
            failWithMessage("Expected %s to have a decode(ByteBuf) method but does not", this.actual);
        }

        ReflectionUtils.makeAccessible(method);
        T actual = (T) ReflectionUtils.invokeMethod(method, null, decoded.apply(TEST.buffer()));

        return new ObjectAssert<>((T) (actual instanceof ReferenceCounted ? this.cleaner.capture((ReferenceCounted) actual) : actual));
    }

    public ReferenceCountedCleaner cleaner() {
        return this.cleaner;
    }

}
