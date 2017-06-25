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
import io.netty.codec.dns.protocol.CodecRegistry;
import io.netty.codec.dns.protocol.DnsRecordCodec;
import io.netty.codec.dns.protocol.optrecords.OptSubrecordCodecs;
import io.netty.codec.dns.protocol.optrecords.OptSubrecordType;
import io.netty.codec.dns.protocol.types.OptRecords;
import io.netty.codec.dns.protocol.types.OptSubrecord;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;
import java.nio.charset.UnmappableCharacterException;

import static io.netty.handler.codec.dns.DnsResponseCode.FORMERR;

public class OptRecordCodec extends DnsRecordCodec<OptRecords> {

    private final CodecRegistry<OptSubrecordType> optCodecs;

    OptRecordCodec(CodecRegistry<OptSubrecordType> optCodecs) {
        super(OptRecords.class);
        this.optCodecs = optCodecs;
    }

    @Override
    public OptRecords read(ByteBuf from, NameCodec forReadingNames, int length) throws DnsDecoderException,
            UnmappableCharacterException, IOException {
        int optRecordHead = from.readerIndex();

        int endPosition = from.readerIndex() + length;
        if (length > from.readableBytes()) {
            throw new DnsDecoderException(FORMERR, "Opt payload length "
                    + length + " greater than remaining bytes in buffer "
                    + from.readableBytes());
        }

        OptRecords records = new OptRecords();
        while (from.readerIndex() < endPosition) {
            int thisRecordStart = from.readerIndex();
            int type = from.readUnsignedShort();
            int subdataLength = from.readUnsignedShort();
            // Back up so the codec sees its data length
//        from.readerIndex(from.readerIndex() - 2);
            OptSubrecordType subrecordType = OptSubrecordType.valueOf(type);
            DnsRecordCodec<?> codec = optCodecs.get(subrecordType);
            ByteBuf readFrom = from;
            if (OptSubrecordCodecs.isFallbackCodec(codec)) {
                readFrom = from.slice(from.readerIndex(), length - (from.readerIndex() - optRecordHead));
            }
            OptSubrecord<?> result = doRead(readFrom, subrecordType, codec, forReadingNames, subdataLength);
//            from.readerIndex(optRecordHead + fullPayloadLength);
            records.add(result);
            from.readerIndex(thisRecordStart + subdataLength + 4);
        }
        return records;
    }

    private <T> OptSubrecord<T> doRead(ByteBuf from, OptSubrecordType ofType,
            DnsRecordCodec<T> with, NameCodec forReadingNames, int subdataLength) throws
            UnmappableCharacterException, IOException {

        T content = with.read(from, forReadingNames, subdataLength);
        return new OptSubrecord<>(ofType, content);
    }

    @Override
    public void write(OptRecords value, NameCodec names, ByteBuf into) throws IOException {
        for (OptSubrecord<?> subrecord : value) {
            doWrite(subrecord, names, into);
        }
    }

    private <T> void doWrite(OptSubrecord<T> value, NameCodec names, ByteBuf into) throws IOException {
        OptSubrecordType subrecordType = value.type();
        DnsRecordCodec<T> codec = (DnsRecordCodec<T>) optCodecs.get(subrecordType); //XXX cast
        int recordBodyTop = into.writerIndex();
//        into.writeShort(0); // come back for this
//        int recordContentStart = into.writerIndex();
        into.writeShort(subrecordType.intValue());
        into.writeShort(0); // come back for this
        codec.write(value.content(), names, into);
        int recordBodyTail = into.writerIndex();
        int bodyLength = recordBodyTail - (recordBodyTop + 2);
        try {
            into.writerIndex(recordBodyTop + 2);
            into.writeShort(bodyLength - 2);
        } finally {
            into.writerIndex(recordBodyTail);
        }
    }
}
