/*
 * Copyright 2021 the original author or authors.
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

package io.r2dbc.gaussdb.api;

import io.r2dbc.gaussdb.util.Assert;
import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.Option;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
final class SimpleTransactionDefinition implements GaussDBTransactionDefinition {

    public static final SimpleTransactionDefinition EMPTY = new SimpleTransactionDefinition(Collections.emptyMap());

    private final Map<Option<?>, Object> options;

    SimpleTransactionDefinition(Map<Option<?>, Object> options) {
        this.options = options;
    }

    @Override
    public <T> T getAttribute(Option<T> option) {
        return (T) this.options.get(option);
    }

    public GaussDBTransactionDefinition with(Option<?> option, Object value) {

        Map<Option<?>, Object> options = new HashMap<>(this.options);
        options.put(Assert.requireNonNull(option, "option must not be null"), Assert.requireNonNull(value, "value must not be null"));

        return new SimpleTransactionDefinition(options);
    }

    @Override
    public GaussDBTransactionDefinition deferrable() {
        return with(GaussDBTransactionDefinition.DEFERRABLE, true);
    }

    @Override
    public GaussDBTransactionDefinition notDeferrable() {
        return with(GaussDBTransactionDefinition.DEFERRABLE, false);
    }

    @Override
    public GaussDBTransactionDefinition isolationLevel(IsolationLevel isolationLevel) {
        return with(GaussDBTransactionDefinition.ISOLATION_LEVEL, isolationLevel);
    }

    @Override
    public GaussDBTransactionDefinition readOnly() {
        return with(GaussDBTransactionDefinition.READ_ONLY, true);
    }

    @Override
    public GaussDBTransactionDefinition readWrite() {
        return with(GaussDBTransactionDefinition.READ_ONLY, false);
    }

}
