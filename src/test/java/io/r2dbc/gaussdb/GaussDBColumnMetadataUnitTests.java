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

package io.r2dbc.gaussdb;

import io.r2dbc.gaussdb.codec.MockCodecs;
import io.r2dbc.gaussdb.message.backend.RowDescription.Field;
import org.junit.jupiter.api.Test;

import static io.r2dbc.gaussdb.message.Format.FORMAT_TEXT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GaussDBColumnMetadata}.
 */
final class GaussDBColumnMetadataUnitTests {

    @Test
    void toColumnMetadata() {
        MockCodecs codecs = MockCodecs.builder()
            .preferredType(200, FORMAT_TEXT, String.class)
            .build();

        GaussDBColumnMetadata columnMetadata = GaussDBColumnMetadata.toColumnMetadata(codecs, new Field((short) 100, 200, 300, (short) 400, FORMAT_TEXT, "test-name", 500));

        assertThat(columnMetadata.getJavaType()).isEqualTo(String.class);
        assertThat(columnMetadata.getName()).isEqualTo("test-name");
        assertThat(columnMetadata.getNativeTypeMetadata()).isEqualTo(200);
        assertThat(columnMetadata.getPrecision()).isEqualTo(400);
    }

}
