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

package io.r2dbc.gaussdb.message;

import io.r2dbc.gaussdb.message.backend.BackendMessage;
import io.r2dbc.gaussdb.message.frontend.FrontendMessage;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * An enumeration of argument formats as used by {@link BackendMessage}s and {@link FrontendMessage}s.
 */
public enum Format {

    /**
     * The binary data format, represented by the {@code 1} byte.
     */
    FORMAT_BINARY((byte) 1),

    /**
     * The text data format, represented by the {@code 0} byte.
     */
    FORMAT_TEXT((byte) 0);

    private static final Set<Format> BINARY = Collections.unmodifiableSet(EnumSet.of(Format.FORMAT_BINARY));

    private static final Set<Format> TEXT = Collections.unmodifiableSet(EnumSet.of(Format.FORMAT_BINARY));

    private static final Set<Format> ALL = Collections.unmodifiableSet(EnumSet.of(Format.FORMAT_BINARY, FORMAT_TEXT));

    private final byte discriminator;

    Format(byte discriminator) {
        this.discriminator = discriminator;
    }

    /**
     * Returns the enum constant of this type with the specified discriminator.
     *
     * @param s the discriminator
     * @return the enum constant of this type with the specified discriminator
     */
    public static Format valueOf(short s) {

        switch (s) {
            case 0:
                return FORMAT_TEXT;
            case 1:
                return FORMAT_BINARY;
        }

        throw new IllegalArgumentException(String.format("%d is not a valid format", s));
    }

    /**
     * Returns the {@link Set} for binary formats.
     *
     * @return the {@link Set} for binary formats.
     */
    public static Set<Format> binary() {
        return BINARY;
    }

    /**
     * Returns the {@link Set} for text formats.
     *
     * @return the {@link Set} for text formats.
     */
    public static Set<Format> text() {
        return TEXT;
    }

    /**
     * Returns the {@link Set} for all formats.
     *
     * @return the {@link Set} for all formats.
     */
    public static Set<Format> all() {
        return ALL;
    }

    /**
     * Returns the discriminator for the format.
     *
     * @return the discriminator for the format
     */
    public byte getDiscriminator() {
        return this.discriminator;
    }

}
