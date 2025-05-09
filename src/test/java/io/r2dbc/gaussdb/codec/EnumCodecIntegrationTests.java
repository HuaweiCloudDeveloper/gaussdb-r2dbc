/*
 * Copyright 2020 the original author or authors.
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

import io.r2dbc.gaussdb.AbstractIntegrationTests;
import io.r2dbc.gaussdb.GaussDBConnectionConfiguration;
import io.r2dbc.gaussdb.GaussDBConnectionFactory;
import io.r2dbc.gaussdb.api.GaussDBConnection;
import io.r2dbc.gaussdb.api.GaussDBResult;
import io.r2dbc.spi.Parameters;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link EnumCodec}.
 */
final class EnumCodecIntegrationTests extends AbstractIntegrationTests {

    @BeforeAll
    static void createEnum() {
        SERVER.getJdbcOperations().execute("CREATE TYPE my_enum_with_codec AS ENUM ('HELLO', 'WORLD')");
        SERVER.getJdbcOperations().execute("CREATE TYPE my_enum_no_codec AS ENUM ('HELLO', 'WORLD')");
    }

    @AfterAll
    static void dropEnum() {
        SERVER.getJdbcOperations().execute("DROP TYPE IF EXISTS my_enum_with_codec CASCADE");
        SERVER.getJdbcOperations().execute("DROP TYPE IF EXISTS my_enum_no_codec CASCADE");
    }

    @Override
    protected void customize(GaussDBConnectionConfiguration.Builder builder) {
        builder.codecRegistrar(EnumCodec.builder().withEnum("my_enum_with_codec", MyEnum.class).build());
    }

    @Test
    void shouldNotRegisterIfEmpty() {

        GaussDBConnectionConfiguration configuration = GaussDBConnectionConfiguration.builder()
            .database(SERVER.getDatabase())
            .host(SERVER.getHost())
            .port(SERVER.getPort())
            .password(SERVER.getPassword())
            .username(SERVER.getUsername())
            .codecRegistrar(EnumCodec.builder().build())
            .build();

        GaussDBConnectionFactory connectionFactory = new GaussDBConnectionFactory(configuration);
        connectionFactory.create().flatMap(GaussDBConnection::close).as(StepVerifier::create).verifyComplete();

        // we cannot really assert logs so that's up to you.
    }

    @Test
    void shouldReportUnresolvableTypes() {

        GaussDBConnectionConfiguration configuration = GaussDBConnectionConfiguration.builder()
            .database(SERVER.getDatabase())
            .host(SERVER.getHost())
            .port(SERVER.getPort())
            .password(SERVER.getPassword())
            .username(SERVER.getUsername())
            .codecRegistrar(EnumCodec.builder().withEnum("do_not_exist", MyEnum.class).build())
            .build();

        GaussDBConnectionFactory connectionFactory = new GaussDBConnectionFactory(configuration);
        connectionFactory.create().flatMap(GaussDBConnection::close).as(StepVerifier::create).verifyComplete();

        // we cannot really assert logs so that's up to you.
    }

    @Test
    void shouldBindEnumTypeAsString() {

        SERVER.getJdbcOperations().execute("DROP TABLE IF EXISTS enum_test");
        SERVER.getJdbcOperations().execute("CREATE TABLE enum_test (the_value my_enum_with_codec);");

        GaussDBTypes types = GaussDBTypes.from(this.connection);
        GaussDBTypes.GaussDBType type = types.lookupType("my_enum_with_codec").block();

        this.connection.createStatement("INSERT INTO enum_test VALUES($1)")
            .bind("$1", Parameters.in(type, "HELLO"))
            .execute()
            .flatMap(GaussDBResult::getRowsUpdated)
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();

        String result = SERVER.getJdbcOperations().queryForObject("SELECT the_value FROM enum_test", String.class);
        assertThat(result).isEqualTo("HELLO");
    }

    @Test
    void shouldBindEnumArrayTypeAsString() {

        SERVER.getJdbcOperations().execute("DROP TABLE IF EXISTS enum_test");
        SERVER.getJdbcOperations().execute("CREATE TABLE enum_test (the_value my_enum_with_codec[]);");

        GaussDBTypes types = GaussDBTypes.from(this.connection);
        GaussDBTypes.GaussDBType type = types.lookupType("my_enum_with_codec").block().asArrayType();

        this.connection.createStatement("INSERT INTO enum_test VALUES($1)")
            .bind("$1", Parameters.in(type, new String[]{"HELLO", "WORLD"}))
            .execute()
            .flatMap(GaussDBResult::getRowsUpdated)
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();

        String result = SERVER.getJdbcOperations().queryForObject("SELECT the_value FROM enum_test", String.class);
        assertThat(result).isEqualTo("{HELLO,WORLD}");
    }

    @Test
    void shouldBindEnumArrayType() {

        SERVER.getJdbcOperations().execute("DROP TABLE IF EXISTS enum_test");
        SERVER.getJdbcOperations().execute("CREATE TABLE enum_test (the_value my_enum_with_codec[]);");

        this.connection.createStatement("INSERT INTO enum_test VALUES($1)")
            .bind("$1", MyEnum.values())
            .execute()
            .flatMap(GaussDBResult::getRowsUpdated)
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();

        this.connection.createStatement("SELECT * FROM enum_test")
            .execute()
            .flatMap(it -> it.map(((row, rowMetadata) -> row.get(0))))
            .as(StepVerifier::create)
            .consumeNextWith(actual -> {

                assertThat(actual).isInstanceOf(MyEnum[].class);
                assertThat((MyEnum[]) actual).contains(MyEnum.values());

            })
            .verifyComplete();

        this.connection.createStatement("SELECT * FROM enum_test")
            .execute()
            .flatMap(it -> it.map(((row, rowMetadata) -> row.get(0, MyEnum[].class))))
            .as(StepVerifier::create)
            .consumeNextWith(actual -> {
                assertThat(actual).contains(MyEnum.values());
            })
            .verifyComplete();
    }

    @Test
    void shouldReadEnumAsString() {

        SERVER.getJdbcOperations().execute("DROP TABLE IF EXISTS enum_test");
        SERVER.getJdbcOperations().execute("CREATE TABLE enum_test (single_value my_enum_no_codec, array_value my_enum_no_codec[]);");
        SERVER.getJdbcOperations().execute("INSERT INTO enum_test VALUES('HELLO'::my_enum_no_codec, '{\"HELLO\", \"WORLD\"}'::my_enum_no_codec[]);");

        this.connection.createStatement("SELECT single_value FROM enum_test")
            .execute()
            .flatMap(it -> it.map(((row, rowMetadata) -> row.get(0, String.class))))
            .as(StepVerifier::create)
            .consumeNextWith(actual -> {
                assertThat(actual).isEqualTo("HELLO");
            })
            .verifyComplete();

        this.connection.createStatement("SELECT single_value FROM enum_test")
            .execute()
            .flatMap(it -> it.map(((row, rowMetadata) -> row.get(0))))
            .as(StepVerifier::create)
            .consumeNextWith(actual -> {
                assertThat(actual).isEqualTo("HELLO");
            })
            .verifyComplete();

        this.connection.createStatement("SELECT array_value FROM enum_test")
            .execute()
            .flatMap(it -> it.map(((row, rowMetadata) -> row.get(0))))
            .as(StepVerifier::create)
            .consumeNextWith(actual -> {
                assertThat(actual).isEqualTo("{HELLO,WORLD}");
            })
            .verifyComplete();
    }

    @Test
    void shouldReadEnumAsStringArray() {

        SERVER.getJdbcOperations().execute("DROP TABLE IF EXISTS enum_test");
        SERVER.getJdbcOperations().execute("CREATE TABLE enum_test (single_value my_enum_no_codec, array_value my_enum_no_codec[]);");
        SERVER.getJdbcOperations().execute("INSERT INTO enum_test VALUES('HELLO'::my_enum_no_codec, '{\"HELLO\", \"WORLD\"}'::my_enum_no_codec[]);");

        this.connection.createStatement("SELECT array_value FROM enum_test")
            .execute()
            .flatMap(it -> it.map(((row, rowMetadata) -> row.get(0, String[].class))))
            .as(StepVerifier::create)
            .consumeNextWith(actual -> {
                assertThat(actual).contains("HELLO", "WORLD");
            })
            .verifyComplete();

        this.connection.createStatement("SELECT array_value FROM enum_test")
            .execute()
            .flatMap(it -> it.map(((row, rowMetadata) -> row.get(0, Object[].class))))
            .as(StepVerifier::create)
            .consumeNextWith(actual -> {
                assertThat(actual).contains("HELLO", "WORLD");
            })
            .verifyComplete();
    }

    enum MyEnum {
        HELLO, WORLD;
    }

}
