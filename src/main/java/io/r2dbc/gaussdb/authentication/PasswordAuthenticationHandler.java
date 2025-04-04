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

package io.r2dbc.gaussdb.authentication;

import io.r2dbc.gaussdb.message.backend.AuthenticationCleartextPassword;
import io.r2dbc.gaussdb.message.backend.AuthenticationMD5Password;
import io.r2dbc.gaussdb.message.backend.AuthenticationMD5SHA256Password;
import io.r2dbc.gaussdb.message.backend.AuthenticationMessage;
import io.r2dbc.gaussdb.message.backend.AuthenticationSHA256Password;
import io.r2dbc.gaussdb.message.frontend.FrontendMessage;
import io.r2dbc.gaussdb.message.frontend.MD5SHA256PasswordMessage;
import io.r2dbc.gaussdb.message.frontend.PasswordMessage;
import io.r2dbc.gaussdb.message.frontend.SHA256PasswordMessage;
import io.r2dbc.gaussdb.util.Assert;

/**
 * An implementation of {@link AuthenticationHandler} that handles {@link AuthenticationCleartextPassword} and {@link AuthenticationMD5Password} messages.
 */
public final class PasswordAuthenticationHandler implements AuthenticationHandler {

    private final CharSequence password;

    private final String username;

    /**
     * Create a new handler.
     *
     * @param password the password to use for authentication
     * @param username the username to use for authentication
     * @throws IllegalArgumentException if {@code password} or {@code user} is {@code null}
     */
    public PasswordAuthenticationHandler(CharSequence password, String username) {
        this.password = Assert.requireNonNull(password, "password must not be null");
        this.username = Assert.requireNonNull(username, "username must not be null");
    }

    /**
     * Returns whether this {@link AuthenticationHandler} can support authentication for a given authentication message response.
     *
     * @param message the message to inspect
     * @return whether this {@link AuthenticationHandler} can support authentication for a given authentication message response
     * @throws IllegalArgumentException if {@code message} is {@code null}
     */
    public static boolean supports(AuthenticationMessage message) {
        Assert.requireNonNull(message, "message must not be null");

        return message instanceof AuthenticationCleartextPassword
            || message instanceof AuthenticationMD5Password
            || message instanceof AuthenticationSHA256Password
            || message instanceof AuthenticationMD5SHA256Password;
    }

    @Override
    public FrontendMessage handle(AuthenticationMessage message) {
        Assert.requireNonNull(message, "message must not be null");

        if (message instanceof AuthenticationCleartextPassword) {
            return handleAuthenticationClearTextPassword();
        } else if (message instanceof AuthenticationMD5Password) {
            return handleAuthenticationMD5Password((AuthenticationMD5Password) message);
        } else if (message instanceof AuthenticationSHA256Password) {
            return handleAuthenticationSHA256Password((AuthenticationSHA256Password) message);
        } else if (message instanceof AuthenticationMD5SHA256Password) {
          return handleAuthenticationMD5SHA256Password((AuthenticationMD5SHA256Password) message);
        } else {
            throw new IllegalArgumentException(String.format("Cannot handle %s message", message.getClass().getSimpleName()));
        }
    }

    private FrontendMessage handleAuthenticationMD5SHA256Password(AuthenticationMD5SHA256Password message) {
        return new MD5SHA256PasswordMessage(this.password, message);
    }

    private FrontendMessage handleAuthenticationSHA256Password(AuthenticationSHA256Password message) {
      return new SHA256PasswordMessage(this.username, this.password, message);
  }

  private PasswordMessage handleAuthenticationClearTextPassword() {
        return new PasswordMessage(this.password);
    }

    private FrontendMessage handleAuthenticationMD5Password(AuthenticationMD5Password message) {
        String shadow = new FluentMessageDigest("md5")
            .update("%s%s", this.password, this.username)
            .digest();

        String transfer = new FluentMessageDigest("md5")
            .update(shadow)
            .update(message.getSalt())
            .digest();

        return new PasswordMessage(String.format("md5%s", transfer));
    }

}
