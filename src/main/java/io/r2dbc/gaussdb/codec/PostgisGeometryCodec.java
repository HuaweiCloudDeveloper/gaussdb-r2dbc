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

package io.r2dbc.gaussdb.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.r2dbc.gaussdb.client.EncodedParameter;
import io.r2dbc.gaussdb.message.Format;
import io.r2dbc.gaussdb.util.Assert;
import io.r2dbc.gaussdb.util.ByteBufUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTWriter;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Collections;

import static io.r2dbc.gaussdb.client.EncodedParameter.NULL_VALUE;
import static io.r2dbc.gaussdb.message.Format.FORMAT_BINARY;
import static io.r2dbc.gaussdb.message.Format.FORMAT_TEXT;

/**
 * PostGIS codec using {@link WKBReader} and {@link WKTWriter}.
 */
final class PostgisGeometryCodec implements Codec<Geometry>, CodecMetadata {

    private static final Class<Geometry> TYPE = Geometry.class;

    private final GeometryFactory geometryFactory = new GeometryFactory();

    private final int oid;

    /**
     * Create a new {@link PostgisGeometryCodec}.
     */
    PostgisGeometryCodec(int oid) {
        this.oid = oid;
    }

    @Override
    public boolean canDecode(int dataType, Format format, Class<?> type) {
        Assert.requireNonNull(format, "format must not be null");
        Assert.requireNonNull(type, "type must not be null");

        // Object = Geometry or Geometry = type (Geometry subtype)
        return dataType == this.oid && (type.isAssignableFrom(TYPE) || TYPE.isAssignableFrom(type));
    }

    @Override
    public boolean canEncode(Object value) {
        Assert.requireNonNull(value, "value must not be null");

        return TYPE.isInstance(value);
    }

    @Override
    public boolean canEncodeNull(Class<?> type) {
        Assert.requireNonNull(type, "type must not be null");

        return TYPE.isAssignableFrom(type);
    }

    @Override
    public Geometry decode(@Nullable ByteBuf buffer, int dataType, Format format, Class<? extends Geometry> type) {
        if (buffer == null) {
            return null;
        }

        Assert.isTrue(format == FORMAT_TEXT, "format must be FORMAT_TEXT");

        try {
            return new WKBReader(this.geometryFactory).read(WKBReader.hexToBytes(ByteBufUtils.decode(buffer)));
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public EncodedParameter encode(Object value) {
        Assert.requireType(value, Geometry.class, "value must be Geometry type");
        Geometry geometry = (Geometry) value;

        WKBWriter writer = new WKBWriter(2, true);

        return new EncodedParameter(FORMAT_BINARY, this.oid, Mono.fromSupplier(
            () -> Unpooled.wrappedBuffer(writer.write(geometry))
        ));
    }

    @Override
    public EncodedParameter encode(Object value, int dataType) {
        return encode(value);
    }

    @Override
    public EncodedParameter encodeNull() {
        return new EncodedParameter(FORMAT_BINARY, this.oid, NULL_VALUE);
    }

    @Override
    public Class<?> type() {
        return TYPE;
    }

    @Override
    public Iterable<GaussDBTypeIdentifier> getDataTypes() {
        return Collections.singleton(AbstractCodec.getDataType(this.oid));
    }

}
