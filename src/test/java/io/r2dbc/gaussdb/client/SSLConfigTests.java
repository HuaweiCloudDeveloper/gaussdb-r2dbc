/*
 * Copyright 2024 the original author or authors.
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

package io.r2dbc.gaussdb.client;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SSLConfig}.
 */
final class SSLConfigTests {

    @ParameterizedTest
    @ValueSource(strings = {"example.com", "foo"})
    public void shouldAcceptValidSniHostname(String hostname) {
        assertThat(SSLConfig.isValidSniHostname(hostname)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.com.", "example://.com"})
    public void shouldRejectValidSniHostname(String hostname) {
        assertThat(SSLConfig.isValidSniHostname(hostname)).isFalse();
    }
}
