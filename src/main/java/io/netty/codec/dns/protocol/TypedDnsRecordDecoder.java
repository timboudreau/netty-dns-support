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
import io.netty.handler.codec.dns.DefaultDnsRecordDecoder;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;
import java.nio.charset.UnmappableCharacterException;

import static io.netty.handler.codec.dns.DnsRecordDecoder.UnderflowPolicy.THROW_ON_UNDERFLOW;
import static io.netty.handler.codec.dns.DnsRecordType.OPT;

/**
 * Decodes DNS records, using a DnsRecordCodecRegistry to look up parsers for the record types, and returning
 * TypedDnsRecords where possible. Before using typed records, consider your needs - for example, if you are writing a
 * cache that does not <i>interpret</i> DNS records, you will save memory storing records as byte arrays rather than
 * Java objects. If you do need this, the DnsCodecRegistry should only contain codecs for records you know you will
 * actually need to use parsed at runtime.
 *
 * @see DnsRecordCodecRegistry
 */
public final class TypedDnsRecordDecoder extends DefaultDnsRecordDecoder {

    private final CodecRegistry<DnsRecordType> registry;

    public TypedDnsRecordDecoder() {
        this(DnsRecordCodecRegistry.DEFAULT, false);
    }

    public TypedDnsRecordDecoder(boolean mdns) {
        this(DnsRecordCodecRegistry.DEFAULT, mdns);
    }

    /**
     * Create a new TypedDnsRecordDecoder that will use codecs registered in the passed registry, and the passed
     * NameCodec for reading names.
     *
     * @param registry A registry
     */
    public TypedDnsRecordDecoder(CodecRegistry<DnsRecordType> registry) {
        this(registry, false);
    }

    public TypedDnsRecordDecoder(CodecRegistry<DnsRecordType> registry, boolean mdns) {
        super(THROW_ON_UNDERFLOW, mdns);
        this.registry = registry;
    }

    @Override
    protected DnsRecord decodeRecord(CharSequence name, DnsRecordType type, int dnsClass, long timeToLive, ByteBuf in,
            int length, NameCodec names) throws Exception {
        DnsRecordCodec<?> codec = registry.get(type);
        if (codec instanceof RecordFactoryCodec<?>) {
            RecordFactoryCodec<?> fac = (RecordFactoryCodec<?>) codec;
            return createRecordWithFactoryCodec(fac, name, type, dnsClass, timeToLive, in, length, names);
        }
        if (codec.type() != ByteBuf.class) {
            return createRecord(codec, name, type, dnsClass, timeToLive, in, length, names);
        }
        return super.decodeRecord(name, type, dnsClass, timeToLive, in, length, names);
    }

    private <T> TypedDnsRecord<T> createRecordWithFactoryCodec(RecordFactoryCodec<T> codec, CharSequence name,
            DnsRecordType type, int dnsClass, long timeToLive, ByteBuf in, int length, NameCodec names) throws
            UnmappableCharacterException, IOException, Exception {
        return codec.decodeRecord(name, type, dnsClass, timeToLive, in, length, names, mdns);
    }

    protected <T> TypedDnsRecord<T> createRecord(DnsRecordCodec<T> codec, CharSequence name, DnsRecordType type,
            int dnsClass, long timeToLive, ByteBuf in, int length, NameCodec names) throws UnmappableCharacterException,
            IOException {
        boolean isUnicastResponse = false;
        if (mdns) {
            isUnicastResponse = (dnsClass & MDNS_UNICAST_RESPONSE_BIT) != 0;
            dnsClass &= MDNS_DNS_CLASS_MASK;
        }
        if (OPT.equals(type)) {
            T obj = codec.read(in, names, length);
            return new DefaultTypedDnsRecord<>(obj, name, type, dnsClass, timeToLive, isUnicastResponse);
        } else {
            T obj = codec.read(in, names, length);
            return new DefaultTypedDnsRecord<>(obj, name, type, dnsClass, timeToLive, isUnicastResponse);
        }
    }
}
