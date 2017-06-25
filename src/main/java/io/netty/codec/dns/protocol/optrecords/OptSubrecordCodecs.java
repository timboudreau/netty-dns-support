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
import io.netty.codec.dns.protocol.types.ClientSubnet;
import io.netty.codec.dns.protocol.types.Cookies;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;
import java.nio.charset.UnmappableCharacterException;

public final class OptSubrecordCodecs {

    private OptSubrecordCodecs() {
    }

    public static DnsRecordCodec<Cookies> cookie() {
        return new CookieCodec();
    }

    public static DnsRecordCodec<ClientSubnet<?>> ecs() {
        return new ClientSubnetCodec();
    }

    public static DnsRecordCodec<ByteBuf> fallback() {
        return new FallbackOptCodec();
    }

    public static boolean isFallbackCodec(DnsRecordCodec<?> codec) {
        return codec instanceof FallbackOptCodec;
    }

    private static final class FallbackOptCodec extends DnsRecordCodec<ByteBuf> {

        FallbackOptCodec() {
            super(ByteBuf.class);
        }

        @Override
        public void write(ByteBuf value, NameCodec names, ByteBuf into) throws IOException {
            if (into != value) {
                into.writeBytes(value);
            }
        }

        @Override
        public ByteBuf read(ByteBuf from, NameCodec forReadingNames, int length) throws DnsDecoderException,
                UnmappableCharacterException, IOException {
            return from.slice(from.readerIndex(), length);
        }
    }
}
