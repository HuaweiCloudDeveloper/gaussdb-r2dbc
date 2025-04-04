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

import io.r2dbc.gaussdb.authentication.AuthenticationHandler;
import io.r2dbc.gaussdb.message.backend.AuthenticationMessage;
import io.r2dbc.gaussdb.message.backend.AuthenticationOk;
import io.r2dbc.gaussdb.message.backend.BackendMessage;
import io.r2dbc.gaussdb.message.frontend.FrontendMessage;
import io.r2dbc.gaussdb.message.frontend.StartupMessage;
import io.r2dbc.gaussdb.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * A utility class that encapsulates the <a href="https://www.postgresql.org/docs/10/static/protocol-flow.html#idm46428664018352">Start-up</a> message flow.
 */
public final class StartupMessageFlow {

    private StartupMessageFlow() {
    }

    /**
     * Execute the <a href="https://www.postgresql.org/docs/10/static/protocol-flow.html#idm46428664018352">Start-up</a> message flow.
     *
     * @param applicationName               the name of the application connecting to the server
     * @param authenticationHandlerProvider the {@link Function} used to provide an {@link AuthenticationHandler} to use for authentication
     * @param client                        the {@link Client} to exchange messages with
     * @param database                      the database to connect to
     * @param username                      the username to authenticate with
     * @return the messages received after authentication is complete, in response to this exchange
     * @throws IllegalArgumentException if {@code applicationName}, {@code authenticationHandler}, {@code client}, or {@code username} is {@code null}
     */
    public static Flux<BackendMessage> exchange(String applicationName, Function<AuthenticationMessage, AuthenticationHandler> authenticationHandlerProvider, Client client,
                                                @Nullable String database, String username) {
        return exchange(authenticationHandlerProvider, client, database, username, new GaussDBStartupParameterProvider(applicationName, TimeZone.getDefault(), (Map<String, String>) null));
    }

    /**
     * Execute the <a href="https://www.postgresql.org/docs/10/static/protocol-flow.html#idm46428664018352">Start-up</a> message flow.
     *
     * @param authenticationHandlerProvider the {@link Function} used to provide an {@link AuthenticationHandler} to use for authentication
     * @param client                        the {@link Client} to exchange messages with
     * @param database                      the database to connect to
     * @param username                      the username to authenticate with
     * @param parameterProvider             the parameter provider providing connection options
     * @return the messages received after authentication is complete, in response to this exchange
     * @throws IllegalArgumentException if {@code applicationName}, {@code authenticationHandler}, {@code client}, or {@code username} is {@code null}
     */
    public static Flux<BackendMessage> exchange(Function<AuthenticationMessage, AuthenticationHandler> authenticationHandlerProvider, Client client,
                                                @Nullable String database, String username, StartupMessage.StartupParameterProvider parameterProvider) {

        Assert.requireNonNull(authenticationHandlerProvider, "authenticationHandlerProvider must not be null");
        Assert.requireNonNull(client, "client must not be null");
        Assert.requireNonNull(username, "username must not be null");
        Assert.requireNonNull(parameterProvider, "parameterProvider must not be null");

        Sinks.Many<FrontendMessage> requests = Sinks.many().unicast().onBackpressureBuffer();
        AtomicReference<AuthenticationHandler> authenticationHandler = new AtomicReference<>(null);

        return client.exchange(requests.asFlux().startWith(new StartupMessage(database, username, parameterProvider)))
            .handle((message, sink) -> {
                if (message instanceof AuthenticationOk) {
                    requests.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                } else if (message instanceof AuthenticationMessage) {
                    try {
                        AuthenticationMessage authenticationMessage = (AuthenticationMessage) message;

                        if (authenticationHandler.get() == null) {
                            authenticationHandler.compareAndSet(null, authenticationHandlerProvider.apply(authenticationMessage));
                        }

                        FrontendMessage response = authenticationHandler.get().handle(authenticationMessage);
                        if (response != null) {
                            requests.emitNext(response, Sinks.EmitFailureHandler.FAIL_FAST);
                        }
                    } catch (Exception e) {
                        requests.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST);
                        sink.error(e);
                    }
                } else {
                    sink.next(message);
                }
            });
    }

}
