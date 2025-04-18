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

package io.r2dbc.gaussdb.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link GeneratedValuesUtils}.
 */
final class GeneratedValuesUtilsUnitTests {

    @Test
    void augmentEmptyGeneratedColumns() {
        assertThat(GeneratedValuesUtils.augment("test-sql", new String[0])).isEqualTo("test-sql RETURNING *");
    }

    @Test
    void augmentGeneratedColumns() {
        assertThat(GeneratedValuesUtils.augment("test-sql", new String[]{"alpha", "bravo"})).isEqualTo("test-sql RETURNING alpha, bravo");
    }

    @Test
    void augmentNoGeneratedColumns() {
        assertThatIllegalArgumentException().isThrownBy(() -> GeneratedValuesUtils.augment("test-sql", null))
            .withMessage("generatedColumns must not be null");
    }

    @Test
    void augmentNoSql() {
        assertThatIllegalArgumentException().isThrownBy(() -> GeneratedValuesUtils.augment(null, new String[0]))
            .withMessage("sql must not be null");
    }

    @Test
    void hasReturning() {
        assertThat(GeneratedValuesUtils.hasReturningClause("test-sql")).isFalse();
        assertThat(GeneratedValuesUtils.hasReturningClause("test-sql RETURNING *")).isTrue();
        assertThat(GeneratedValuesUtils.hasReturningClause("select * from userReturning")).isFalse();
        assertThat(GeneratedValuesUtils.hasReturningClause("select * from ReturningUser")).isFalse();
    }

    @Test
    void hasReturningClauseNoSql() {
        assertThatIllegalArgumentException().isThrownBy(() -> GeneratedValuesUtils.hasReturningClause(null))
            .withMessage("sql must not be null");
    }

    @Test
    void isSupported() {
        assertThat(GeneratedValuesUtils.isSupportedCommand("SELECT test-sql")).isFalse();
        assertThat(GeneratedValuesUtils.isSupportedCommand("DELETE test-sql")).isTrue();
        assertThat(GeneratedValuesUtils.isSupportedCommand("INSERT test-sql")).isTrue();
        assertThat(GeneratedValuesUtils.isSupportedCommand("UPDATE test-sql")).isTrue();
    }

    @Test
    void isSupportedCommandNoSql() {
        assertThatIllegalArgumentException().isThrownBy(() -> GeneratedValuesUtils.isSupportedCommand(null))
            .withMessage("sql must not be null");
    }

}
