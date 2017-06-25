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
import io.netty.codec.dns.protocol.types.StartOfAuthority;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;
import java.nio.charset.UnmappableCharacterException;

final class StartOfAuthorityRecordCodec extends DnsRecordCodec<StartOfAuthority> {

    StartOfAuthorityRecordCodec() {
        super(StartOfAuthority.class);
    }

    @Override
    public StartOfAuthority read(ByteBuf buf, NameCodec forReadingNames, int length) throws DnsDecoderException,
            UnmappableCharacterException {
        CharSequence primaryNs = forReadingNames.readName(buf);
        CharSequence adminMailbox = forReadingNames.readName(buf);
        long serialNumber = DnsRecordCodec.toUnsignedLong(buf.readInt());
        long refreshInterval = DnsRecordCodec.toUnsignedLong(buf.readInt());
        long retryInterval = DnsRecordCodec.toUnsignedLong(buf.readInt());
        long expirationLimit = DnsRecordCodec.toUnsignedLong(buf.readInt());
        long minimumTtl = DnsRecordCodec.toUnsignedLong(buf.readInt());
        return new StartOfAuthority(primaryNs, adminMailbox, serialNumber, refreshInterval, retryInterval,
                expirationLimit, minimumTtl);
    }

    @Override
    public void write(StartOfAuthority auth, NameCodec writer, ByteBuf buf) throws IOException {
        writer.writeName(auth.primaryNs, buf);
        writer.writeName(auth.adminMailbox, buf);
        buf.writeInt((int) auth.serialNumber);
        buf.writeInt((int) auth.refreshInterval);
        buf.writeInt((int) auth.retryInterval);
        buf.writeInt((int) auth.expirationLimit);
        buf.writeInt((int) auth.minimumTtl);
    }

}
