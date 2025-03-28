/*
 * Copyright 2022 the original author or authors.
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

import io.r2dbc.gaussdb.authentication.AuthenticationHandler;
import io.r2dbc.gaussdb.authentication.PasswordAuthenticationHandler;
import io.r2dbc.gaussdb.authentication.SASLAuthenticationHandler;
import io.r2dbc.gaussdb.client.Client;
import io.r2dbc.gaussdb.client.ConnectionContext;
import io.r2dbc.gaussdb.client.ConnectionSettings;
import io.r2dbc.gaussdb.client.GaussDBStartupParameterProvider;
import io.r2dbc.gaussdb.client.StartupMessageFlow;
import io.r2dbc.gaussdb.message.backend.AuthenticationMessage;
import io.r2dbc.gaussdb.util.Assert;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.net.SocketAddress;

final class SingleHostConnectionFunction implements ConnectionFunction {

    private final ConnectionFunction upstreamFunction;

    private final GaussDBConnectionConfiguration configuration;

    SingleHostConnectionFunction(ConnectionFunction upstreamFunction, GaussDBConnectionConfiguration configuration) {
        this.upstreamFunction = upstreamFunction;
        this.configuration = configuration;
    }

    @Override
    public Mono<Client> connect(SocketAddress endpoint, ConnectionSettings settings) {

        return this.upstreamFunction.connect(endpoint, settings)
            .delayUntil(client -> getCredentials().flatMapMany(credentials -> StartupMessageFlow
                    .exchange(auth -> getAuthenticationHandler(auth, credentials, client.getContext()), client, this.configuration.getDatabase(), credentials.getUsername(),
                        getParameterProvider(this.configuration, settings)))
                .handle(ExceptionFactory.INSTANCE::handleErrorResponse));
    }

    private static GaussDBStartupParameterProvider getParameterProvider(GaussDBConnectionConfiguration configuration, ConnectionSettings settings) {
        return new GaussDBStartupParameterProvider(configuration.getApplicationName(), configuration.getTimeZone(), settings);
    }

    protected AuthenticationHandler getAuthenticationHandler(AuthenticationMessage message, UsernameAndPassword usernameAndPassword, ConnectionContext context) {
        if (PasswordAuthenticationHandler.supports(message)) {
            CharSequence password = Assert.requireNonNull(usernameAndPassword.getPassword(), "Password must not be null");
            return new PasswordAuthenticationHandler(password, usernameAndPassword.getUsername());
        } else if (SASLAuthenticationHandler.supports(message)) {
            CharSequence password = Assert.requireNonNull(usernameAndPassword.getPassword(), "Password must not be null");
            return new SASLAuthenticationHandler(password, usernameAndPassword.getUsername(), context);
        } else {
            throw new IllegalStateException(String.format("Unable to provide AuthenticationHandler capable of handling %s", message));
        }
    }

    Mono<UsernameAndPassword> getCredentials() {

        return Mono.zip(Mono.from(this.configuration.getUsername()).single(), Mono.from(this.configuration.getPassword()).singleOptional()).map(it -> {
            return new UsernameAndPassword(it.getT1(), it.getT2().orElse(null));
        });
    }

    static class UsernameAndPassword {

        final String username;

        final @Nullable CharSequence password;

        public UsernameAndPassword(String username, @Nullable CharSequence password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return this.username;
        }

        @Nullable
        public CharSequence getPassword() {
            return this.password;
        }
    }

}
