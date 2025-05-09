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

package io.r2dbc.gaussdb;

import io.r2dbc.spi.Result;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Integration tests {@link Result.Segment}.
 */
final class SegmentIntegrationTests extends AbstractIntegrationTests {

    @Test
    void shouldConsumeNotice() {

        this.connection.createStatement("DO language plpgsql $$\n" +
                "BEGIN\n" +
                "  RAISE NOTICE 'hello, world!';\n" +
                "END\n" +
                "$$;").execute().flatMap(it -> it.flatMap(segment -> {
                if (segment instanceof Result.Message) {
                    return Mono.just(((Result.Message) segment).message());
                }
                return Mono.empty();
            })).as(StepVerifier::create)
            .expectNext("hello, world!")
            .verifyComplete();
    }

}
