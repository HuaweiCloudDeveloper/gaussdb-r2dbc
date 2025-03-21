/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.gaussdb.client;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;

/**
 * SSL handler assuming the endpoint is a SSL tunnel and not a GaussDB endpoint.
 */
final class SSLTunnelHandlerAdapter extends AbstractGaussDBSSLHandlerAdapter {

    private final SSLConfig sslConfig;

    SSLTunnelHandlerAdapter(ByteBufAllocator alloc, SocketAddress socketAddress, SSLConfig sslConfig) {
        super(alloc, socketAddress, sslConfig);
        this.sslConfig = sslConfig;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {

        if (this.sslConfig.getSslMode() == SSLMode.DISABLE) {

            GaussDBSslException e = new GaussDBSslException("Server requires SSL handshake, but client was configured with SSL mode DISABLE");
            completeHandshakeExceptionally(e);
            return;
        }
        ctx.channel().pipeline().addFirst(this.getSslHandler());
    }

}
