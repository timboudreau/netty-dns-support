/*
 * Copyright 2017 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.codec.dns.protocol.optrecords;

import io.netty.buffer.ByteBuf;
import io.netty.codec.dns.protocol.DnsRecordCodec;
import io.netty.codec.dns.protocol.types.Cookies;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;
import java.nio.charset.UnmappableCharacterException;

import static io.netty.handler.codec.dns.DnsResponseCode.FORMERR;

public class CookieCodec extends DnsRecordCodec<Cookies> {

    private static final int CLIENT_COOKIE_BYTE_COUNT = 8;

    public CookieCodec() {
        super(Cookies.class);
    }

    @Override
    public void write(Cookies cookies, NameCodec names, ByteBuf into) throws IOException {
        into.writeBytes(cookies.clientCookie());
        if (cookies.hasServerCookie()) {
            into.writeBytes(cookies.serverCookie());
        }
    }

    @Override
    public Cookies read(ByteBuf from, NameCodec forReadingNames, int length) throws DnsDecoderException,
            UnmappableCharacterException, IOException {
        if (from.readableBytes() < CLIENT_COOKIE_BYTE_COUNT) {
            throw new DnsDecoderException(FORMERR, "Insufficient remaining bytes to read an "
                    + CLIENT_COOKIE_BYTE_COUNT + " byte cookie");
        }
        if (length < CLIENT_COOKIE_BYTE_COUNT) {
            throw new DnsDecoderException(FORMERR, "Insufficient remaining bytes to read an "
                    + CLIENT_COOKIE_BYTE_COUNT + " byte cookie");
        }
        byte[] clientCookie = new byte[CLIENT_COOKIE_BYTE_COUNT];
        from.readBytes(clientCookie);
        int remaining = length - CLIENT_COOKIE_BYTE_COUNT;
        Cookies result;
        if (remaining > 0) {
            byte[] serverCookie = new byte[remaining];
            from.readBytes(serverCookie);
            result = new Cookies(clientCookie, serverCookie);
        } else {
            result = new Cookies(clientCookie);
        }
        return result;
    }
}
