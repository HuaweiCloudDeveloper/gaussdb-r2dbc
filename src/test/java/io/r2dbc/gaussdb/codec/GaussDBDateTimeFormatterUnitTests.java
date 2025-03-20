/*
 * Copyright 2023 the original author or authors.
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

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GaussDBDateTimeFormatter}.
 */
class GaussDBDateTimeFormatterUnitTests {

    @Test
    void shouldFormatOffsetDateTimeWithEraIfNecessary() {

        assertThat(GaussDBDateTimeFormatter.toString(OffsetDateTime.parse("0000-12-31T01:01:00Z"))).isEqualTo("0001-12-31 01:01:00+00 BC");

        assertThat(GaussDBDateTimeFormatter.toString(OffsetDateTime.parse("2001-12-31T01:01:00Z"))).isEqualTo("2001-12-31 01:01:00+00");
    }

    @Test
    void shouldParseOffsetDateTimeWithEraIfNecessary() {

        assertThat(GaussDBDateTimeFormatter.parseOffsetDateTime("0001-12-31 01:01:00+00 BC")).isEqualTo(OffsetDateTime.parse("0000-12-31T01:01:00Z"));

        OffsetDateTime actual = GaussDBDateTimeFormatter.parseOffsetDateTime("0001-12-31 00:54:28+00:53:28 BC");
        assertThat(actual).isEqualTo(OffsetDateTime.parse("0000-12-31T00:01Z"));

        assertThat(GaussDBDateTimeFormatter.parseOffsetDateTime("2001-12-31 01:01:00+00")).isEqualTo(OffsetDateTime.parse("2001-12-31T01:01:00Z"));
    }

}
