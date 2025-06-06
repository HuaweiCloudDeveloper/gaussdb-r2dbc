/*
 * Copyright 2019 the original author or authors.
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.gaussdb.api.GaussDBConnection;
import io.r2dbc.gaussdb.api.GaussDBResult;
import io.r2dbc.gaussdb.client.EncodedParameter;
import io.r2dbc.gaussdb.codec.Codec;
import io.r2dbc.gaussdb.codec.CodecRegistry;
import io.r2dbc.gaussdb.codec.GaussDBObjectId;
import io.r2dbc.gaussdb.extension.CodecRegistrar;
import io.r2dbc.gaussdb.message.Format;
import io.r2dbc.gaussdb.util.ByteBufUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CodecRegistrar}.
 */
final class CodecExtensionIntegrationTests extends AbstractIntegrationTests {

    @BeforeEach
    void setUp() {
        JsonCodecRegistrar.REGISTER = true;
        super.setUp();
    }

    @AfterEach
    void tearDown() {
        JsonCodecRegistrar.REGISTER = false;
        super.tearDown();
    }

    @Test
    void shouldRegisterCodec() {

        this.connection.createStatement("DROP TABLE IF EXISTS codec_json_test;CREATE TABLE codec_json_test (my_value json);")
            .execute().flatMap(GaussDBResult::getRowsUpdated).then()
            .as(StepVerifier::create).verifyComplete();

        this.connection.createStatement("INSERT INTO codec_json_test VALUES('{ \"customer\": \"John Doe\", \"items\": {\"product\": \"Beer\",\"qty\": 6}}')")
            .execute().flatMap(GaussDBResult::getRowsUpdated).then()
            .as(StepVerifier::create).verifyComplete();

        this.connection.createStatement("SELECT * FROM codec_json_test")
            .execute()
            .flatMap(it -> it.map((row, rowMetadata) -> row.get(0)))
            .cast(Json.class)
            .as(StepVerifier::create)
            .consumeNextWith(json -> {

                assertThat(json.data).contains("{ \"customer\": \"John Doe\"");

            })
            .verifyComplete();
    }

    public static class JsonCodecRegistrar implements CodecRegistrar {

        /**
         * Prevent other tests from catching this extension.
         */
        static boolean REGISTER = false;

        @Override
        public Publisher<Void> register(GaussDBConnection connection, ByteBufAllocator allocator, CodecRegistry registry) {

            if (!REGISTER) {
                return Mono.empty();
            }

            return Mono.fromRunnable(() -> registry.addFirst(JsonToTextCodec.INSTANCE));
        }

    }

    enum JsonToTextCodec implements Codec<Json> {
        INSTANCE;

        @Override
        public boolean canDecode(int dataType, Format format, Class<?> type) {
            return dataType == GaussDBObjectId.JSON.getObjectId();
        }

        @Override
        public boolean canEncode(Object value) {
            return false;
        }

        @Override
        public boolean canEncodeNull(Class<?> type) {
            return false;
        }

        @Override
        public Json decode(ByteBuf buffer, int dataType, Format format, Class<? extends Json> type) {

            if (buffer == null) {
                return null;
            }
            return new Json(ByteBufUtils.decode(buffer));
        }

        @Override
        public EncodedParameter encode(Object value) {
            return null;
        }

        @Override
        public EncodedParameter encode(Object value, int dataType) {
            return null;
        }

        @Override
        public EncodedParameter encodeNull() {
            return null;
        }

        private EncodedParameter encodeNull(int dataType) {
            return null;
        }

    }

    static class Json {

        private final String data;

        Json(String data) {
            this.data = data;
        }

    }

}
