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
import io.netty.buffer.ByteBufUtil;
import io.netty.codec.dns.protocol.DnsRecordCodec;
import io.netty.codec.dns.protocol.types.UriInfo;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.names.NameCodec;
import io.netty.util.CharsetUtil;
import java.io.IOException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnmappableCharacterException;

import static io.netty.util.CharsetUtil.US_ASCII;

/**
 * Codec for URI records.
 */
class UriRecordCodec extends DnsRecordCodec<UriInfo> {
    private static final CharsetEncoder ENCODER = CharsetUtil.US_ASCII.newEncoder();
    UriRecordCodec() {
        super(UriInfo.class);
    }

    @Override
    public UriInfo read(ByteBuf from, NameCodec forReadingNames, int length) throws DnsDecoderException,
            UnmappableCharacterException, IOException {
        int priority = from.readUnsignedShort();
        int weight = from.readUnsignedShort();
        int uriLength = from.readUnsignedByte();
        CharSequence uri = from.readCharSequence(uriLength, US_ASCII);
        return new UriInfo(weight, priority, uri);
    }

    @Override
    public void write(UriInfo value, NameCodec names, ByteBuf into) throws IOException {
        if (!ENCODER.canEncode(value.uri)) {
            throw new IllegalArgumentException("URI '" + value.uri + "' cannot be encoded in ascii");
        }
        into.writeShort(value.priority);
        into.writeShort(value.weight);
        into.writeByte(value.uri.length());
        ByteBufUtil.writeAscii(into, value.uri);
    }
}
