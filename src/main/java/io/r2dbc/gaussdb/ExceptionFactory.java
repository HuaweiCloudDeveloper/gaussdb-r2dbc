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

import io.r2dbc.gaussdb.api.ErrorDetails;
import io.r2dbc.gaussdb.api.GaussDBException;
import io.r2dbc.gaussdb.message.backend.BackendMessage;
import io.r2dbc.gaussdb.message.backend.ErrorResponse;
import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcPermissionDeniedException;
import io.r2dbc.spi.R2dbcRollbackException;
import io.r2dbc.spi.R2dbcTransientException;
import reactor.core.publisher.SynchronousSink;
import reactor.util.annotation.Nullable;

/**
 * Factory for Postgres-specific {@link R2dbcException}s.
 */
final class ExceptionFactory {

    public static final ExceptionFactory INSTANCE = new ExceptionFactory("");

    private final String sql;

    private ExceptionFactory(String sql) {
        this.sql = sql;
    }

    /**
     * Create a {@link ExceptionFactory} associated with a SQL query.
     *
     * @param sql
     * @return
     */
    static ExceptionFactory withSql(String sql) {
        return new ExceptionFactory(sql);
    }

    /**
     * Create a {@link R2dbcException} from an {@link ErrorResponse}.
     *
     * @param response the response that contains the error details.
     * @param sql      underlying SQL.
     * @return the {@link R2dbcException}.
     * @see ErrorResponse
     */
    static R2dbcException createException(ErrorResponse response, String sql) {
        return createException(new ErrorDetails(response.getFields()), sql);
    }

    /**
     * Create a {@link R2dbcException} from an {@link ErrorDetails}.
     *
     * @param errorDetails the error details.
     * @param sql          underlying SQL.
     * @return the {@link R2dbcException}.
     * @see ErrorResponse
     */
    private static R2dbcException createException(ErrorDetails errorDetails, String sql) {

        switch (errorDetails.getCode()) {
            case "42501":
                return new GaussDBPermissionDeniedException(errorDetails, sql);
            case "40000":
            case "40001":
                return new GaussDBRollbackException(errorDetails, sql);
            case "28000":
            case "28P01":
                return new GaussDBAuthenticationFailure(errorDetails, sql);
        }

        String codeClass = errorDetails.getCode().length() > 2 ? errorDetails.getCode().substring(0, 2) : "99";

        switch (codeClass) {
            case "03": // SQL Statement Not Yet Complete
            case "42": // Syntax Error or Access Rule Violation
            case "22": // Data Exception
            case "26": // Invalid SQL Statement Name
                return new GaussDBBadGrammarException(errorDetails, sql);
            case "08": // Connection Exception
                return new GaussDBNonTransientResourceException(errorDetails, sql);
            case "21": // Cardinality Violation
            case "23": // Integrity Constraint Violation
            case "27": // Integrity Constraint Violation
                return new GaussDBDataIntegrityViolationException(errorDetails, sql);
            case "28": // Invalid Authorization Specification
                return new GaussDBPermissionDeniedException(errorDetails, sql);
            case "40": // Invalid Authorization Specification
                return new GaussDBTransientException(errorDetails, sql);
        }

        return new GaussDBNonTransientResourceException(errorDetails, sql);
    }

    /**
     * Create a {@link R2dbcException} from an {@link ErrorDetails}.
     *
     * @param errorDetails the error details.
     * @return the {@link R2dbcException}.
     * @see ErrorResponse
     */
    public R2dbcException createException(ErrorDetails errorDetails) {
        return createException(errorDetails, this.sql);
    }

    /**
     * Handle {@link BackendMessage}s and inspect for {@link ErrorResponse} to emit a {@link R2dbcException}.
     *
     * @param message the message.
     * @param sink    the outbound sink.
     */
    void handleErrorResponse(BackendMessage message, SynchronousSink<BackendMessage> sink) {

        if (message instanceof ErrorResponse) {
            sink.error(createException((ErrorResponse) message, this.sql));
        } else {
            sink.next(message);
        }
    }

    /**
     * Postgres-specific {@link R2dbcBadGrammarException}.
     */
    static final class GaussDBBadGrammarException extends R2dbcBadGrammarException implements GaussDBException {

        private final ErrorDetails errorDetails;

        GaussDBBadGrammarException(ErrorDetails errorDetails, String sql) {
            super(errorDetails.getMessage(), errorDetails.getCode(), 0, sql);
            this.errorDetails = errorDetails;
        }

        @Override
        public ErrorDetails getErrorDetails() {
            return this.errorDetails;
        }

    }

    /**
     * Postgres-specific {@link R2dbcDataIntegrityViolationException}.
     */
    static final class GaussDBDataIntegrityViolationException extends R2dbcDataIntegrityViolationException implements GaussDBException {

        private final ErrorDetails errorDetails;

        GaussDBDataIntegrityViolationException(ErrorDetails errorDetails, @Nullable String sql) {
            super(errorDetails.getMessage(), errorDetails.getCode(), 0, sql);
            this.errorDetails = errorDetails;
        }

        @Override
        public ErrorDetails getErrorDetails() {
            return this.errorDetails;
        }

    }

    /**
     * Postgres-specific {@link R2dbcNonTransientResourceException}.
     */
    static final class GaussDBNonTransientResourceException extends R2dbcNonTransientResourceException implements GaussDBException {

        private final ErrorDetails errorDetails;

        GaussDBNonTransientResourceException(ErrorDetails errorDetails, @Nullable String sql) {
            super(errorDetails.getMessage(), errorDetails.getCode(), 0, sql);
            this.errorDetails = errorDetails;
        }

        @Override
        public ErrorDetails getErrorDetails() {
            return this.errorDetails;
        }

    }

    /**
     * Postgres-specific {@link R2dbcPermissionDeniedException}.
     */
    static final class GaussDBPermissionDeniedException extends R2dbcPermissionDeniedException implements GaussDBException {

        private final ErrorDetails errorDetails;

        GaussDBPermissionDeniedException(ErrorDetails errorDetails, @Nullable String sql) {
            super(errorDetails.getMessage(), errorDetails.getCode(), 0, sql);
            this.errorDetails = errorDetails;
        }

        @Override
        public ErrorDetails getErrorDetails() {
            return this.errorDetails;
        }

    }

    /**
     * Postgres-specific {@link R2dbcRollbackException}.
     */
    static final class GaussDBRollbackException extends R2dbcRollbackException implements GaussDBException {

        private final ErrorDetails errorDetails;

        GaussDBRollbackException(ErrorDetails errorDetails, @Nullable String sql) {
            super(errorDetails.getMessage(), errorDetails.getCode(), 0, sql);
            this.errorDetails = errorDetails;
        }

        @Override
        public ErrorDetails getErrorDetails() {
            return this.errorDetails;
        }

    }

    /**
     * Postgres-specific {@link R2dbcTransientException}.
     */
    static final class GaussDBTransientException extends R2dbcTransientException implements GaussDBException {

        private final ErrorDetails errorDetails;

        GaussDBTransientException(ErrorDetails errorDetails, @Nullable String sql) {
            super(errorDetails.getMessage(), errorDetails.getCode(), 0, sql);
            this.errorDetails = errorDetails;
        }

        @Override
        public ErrorDetails getErrorDetails() {
            return this.errorDetails;
        }

    }

    /**
     * Postgres-specific {@link R2dbcPermissionDeniedException}.
     */
    static final class GaussDBAuthenticationFailure extends R2dbcPermissionDeniedException implements GaussDBException {

        private final ErrorDetails errorDetails;

        GaussDBAuthenticationFailure(ErrorDetails errorDetails, @Nullable String sql) {
            super(errorDetails.getMessage(), errorDetails.getCode(), 0, sql);
            this.errorDetails = errorDetails;
        }

        @Override
        public ErrorDetails getErrorDetails() {
            return this.errorDetails;
        }

    }

}
