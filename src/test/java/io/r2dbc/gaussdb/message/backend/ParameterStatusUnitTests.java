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

import org.junit.jupiter.api.Test;

import static io.netty.util.CharsetUtil.UTF_8;
import static io.r2dbc.gaussdb.message.backend.BackendMessageAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ParameterStatus}.
 */
final class ParameterStatusUnitTests {

    @Test
    void constructorNoName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ParameterStatus(null, "test-value"))
            .withMessage("name must not be null");
    }

    @Test
    void constructorNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ParameterStatus(null, "test-value"))
            .withMessage("name must not be null");
    }

    @Test
    void decode() {
        assertThat(ParameterStatus.class)
            .decoded(buffer -> {
                buffer.writeCharSequence("test-name", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("test-value", UTF_8);
                buffer.writeByte(0);

                return buffer;
            })
            .isEqualTo(new ParameterStatus("test-name", "test-value"));
    }

}
