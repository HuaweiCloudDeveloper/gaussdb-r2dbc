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

import java.util.Collections;

import static io.r2dbc.gaussdb.message.backend.BackendMessageAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ParameterDescription}.
 */
final class ParameterDescriptionUnitTests {

    @Test
    void constructorNoParameters() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ParameterDescription(null))
            .withMessage("parameters must not be null");
    }

    @Test
    void decode() {
        assertThat(ParameterDescription.class)
            .decoded(buffer -> buffer
                .writeShort(1)
                .writeInt(100))
            .isEqualTo(new ParameterDescription(Collections.singletonList(100)));
    }

}
