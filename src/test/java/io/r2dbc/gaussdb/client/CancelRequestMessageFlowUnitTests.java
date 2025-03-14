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

package io.r2dbc.gaussdb.client;

import io.r2dbc.gaussdb.message.frontend.CancelRequest;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link CancelRequest}.
 */
final class CancelRequestMessageFlowUnitTests {

    @Test
    void exchange() {
        Client client = TestClient.builder()
            .expectRequest(new CancelRequest(100, 200)).thenRespond()
            .build();

        CancelRequestMessageFlow
            .exchange(client, 100, 200)
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    void exchangeNoClient() {
        assertThatIllegalArgumentException().isThrownBy(() -> CancelRequestMessageFlow.exchange(null, 100, 200))
            .withMessage("client must not be null");
    }

}
