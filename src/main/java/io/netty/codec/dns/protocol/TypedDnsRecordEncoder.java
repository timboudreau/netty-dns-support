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
package io.netty.codec.dns.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.handler.codec.dns.DefaultDnsRecordEncoder;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;

import static io.netty.handler.codec.dns.DnsRecordType.OPT;

/**
 * Create an encoder which can accept TypedDnsRecord instances and use a DnsRecordCodecRegistry to find codecs to
 * convert them to on-the-wire format. Before using typed records, consider your needs - for example, if you are writing
 * a cache that does not <i>interpret</i> DNS records, you will save memory storing records as byte arrays rather than
 * Java objects. If you do need this, the DnsCodecRegistry should only contain codecs for records you know you will
 * actually need to use parsed at runtime.
 */
public class TypedDnsRecordEncoder extends DefaultDnsRecordEncoder {

    private final CodecRegistry<DnsRecordType> registry;

    public TypedDnsRecordEncoder() {
        this(false);
    }

    public TypedDnsRecordEncoder(boolean mdns) {
        this(DnsRecordCodecRegistry.DEFAULT, mdns);
    }

    public TypedDnsRecordEncoder(CodecRegistry<DnsRecordType> registry) {
        this(registry, false);
    }

    /**
     * Create a new TypedDnsRecordEncoder which will use the passed registry to look up codecs.
     *
     * @param registry A registry
     */
    public TypedDnsRecordEncoder(CodecRegistry<DnsRecordType> registry, boolean mdns) {
        super(mdns);
        this.registry = registry;
    }

    @Override
    public void encodeRecord(NameCodec names, DnsRecord record, ByteBuf into, int maxPacketSize) throws Exception {
        if (record instanceof TypedDnsRecord<?>) {
            TypedDnsRecord<?> rec = (TypedDnsRecord<?>) record;
            if (OPT.equals(record.type())) {
                writeOptRecord(rec, names, into, maxPacketSize);
            } else {
                names.writeName(record.name(), into);

                into.writeShort(record.type().intValue());
                encodeDnsClass(record, into);
                into.writeInt((int) record.timeToLive());

                // Write a temporary length field of 0 - go back
                // and fill it in once we know the byte buf length
                int lengthFieldPosition = into.writerIndex();
                into.writeShort(0);

                writePayload(rec, names, into);

                // Now rewrite the length field with the number of
                // bytes appended to the byte buf
                int loc = into.writerIndex();
                into.writerIndex(lengthFieldPosition);
                into.writeShort(loc - (lengthFieldPosition + 2));
                // And reset the location
                into.writerIndex(loc);
            }
        } else {
            if (record instanceof DnsRawRecord) {
                ((ByteBufHolder) record).content().touch();
            }
            super.encodeRecord(names, record, into, maxPacketSize);
        }
    }

    protected <T> void writeOptRecord(TypedDnsRecord<T> record, NameCodec names, ByteBuf into, int maxPacketSize) throws
            IOException {
        DnsRecordCodec<T> codec = registry.get(record.type(), record.content());
        names.writeName(".", into);
        into.writeShort(record.type().intValue());
        // Temporarily writePseudoRecord in values from the record - they
        // may be correct if it was serialized;  otherwise
        // they will be overwritten by the codec.
        int dnsClassValue = record.dnsClassValue();
        long timeToLiveValue = record.timeToLive();

        into.writeShort(dnsClassValue);
        into.writeInt((int) timeToLiveValue);

        // Write a temporary length field of 0 - go back
        // and fill it in once we know the byte buf length
        int lengthFieldPosition = into.writerIndex();
        into.writeShort(0);

        codec.write(record.content(), names, into);

        int loc = into.writerIndex();
        into.writerIndex(lengthFieldPosition);
        into.writeShort(loc - (lengthFieldPosition + 2));
        // Put the cursor back at the end of the buffer
        into.writerIndex(loc);
    }

    private <T> void writePayload(TypedDnsRecord<T> record, NameCodec names, ByteBuf into) throws IOException {
        DnsRecordCodec<T> codec = registry.get(record.type(), record.content());
        codec.write(record.content(), names, into);
    }
}
