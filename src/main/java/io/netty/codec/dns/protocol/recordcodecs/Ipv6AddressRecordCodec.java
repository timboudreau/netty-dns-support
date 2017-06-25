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
package io.netty.codec.dns.protocol.recordcodecs;

import io.netty.buffer.ByteBuf;
import io.netty.codec.dns.protocol.DnsRecordCodec;
import io.netty.codec.dns.protocol.types.Ipv6Address;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;

final class Ipv6AddressRecordCodec extends DnsRecordCodec<Ipv6Address> {

    Ipv6AddressRecordCodec() {
        super(Ipv6Address.class);
    }

    @Override
    public Ipv6Address read(ByteBuf buffer, NameCodec forReadingNames, int length) throws DnsDecoderException {
        long high = buffer.readLong();
        long low = buffer.readLong();
        return new Ipv6Address(high, low);
    }

    @Override
    public void write(Ipv6Address value, NameCodec names, ByteBuf into) throws IOException {
        into.writeLong(value.high());
        into.writeLong(value.low());
    }

}
