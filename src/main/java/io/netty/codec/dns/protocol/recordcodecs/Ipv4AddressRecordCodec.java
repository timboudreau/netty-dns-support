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
import io.netty.codec.dns.protocol.types.Ipv4Address;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;

final class Ipv4AddressRecordCodec extends DnsRecordCodec<Ipv4Address> {

    Ipv4AddressRecordCodec() {
        super(Ipv4Address.class);
    }

    @Override
    public Ipv4Address read(ByteBuf buffer, NameCodec forReadingNames, int length) throws DnsDecoderException {
        return new Ipv4Address(buffer.readInt());
    }

    @Override
    public void write(Ipv4Address value, NameCodec names, ByteBuf into) throws IOException {
        into.writeInt(value.intValue());
    }

}
