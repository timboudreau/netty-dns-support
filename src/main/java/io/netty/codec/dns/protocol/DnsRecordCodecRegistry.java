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

import io.netty.codec.dns.protocol.CodecRegistry.CodecRegistryBuilder;
import io.netty.codec.dns.protocol.recordcodecs.DnsRecordCodecs;
import io.netty.handler.codec.dns.DnsRecordType;

import static io.netty.handler.codec.dns.DnsRecordType.*;

/**
 * Used with TypedDnsRecordEncoder/Decoder to decode the raw bytes of DNS records into objects and back. Implementations
 * that want to parse DNS records that are not defined in this library can register their own DnsRecordCodec
 * implementations and have those records decoded and encoded.
 * <p>
 * Record types which do not have a DnsRecordCodec associated with them here will be decoded by a fallback
 * DnsRecordCodec parameterized on ByteBuf.
 * <p>
 * New codecs can be registered at runtime. When called, the ByteBuf's readerIndex() will be at the beginning of the
 * <i>payload</i>, after the payload-length field (which, in the case of writers, will be rewritten based on the
 * writerIndex() post-call, and in the case of readers, is contained in the passed length parameter.
 *
 * @see DnsRecordCodec
 */
public final class DnsRecordCodecRegistry {

    public static CodecRegistry<DnsRecordType> DEFAULT = builderWithDefaultCodecs().build();

    private DnsRecordCodecRegistry() {
    }

    protected int intValueFor(DnsRecordType type) {
        return type.intValue();
    }

    static final class DnsRecordInternal implements CodecRegistry.RegistryInternal<DnsRecordType> {

        @Override
        public int intValueFor(DnsRecordType type) {
            return type.intValue();
        }

        @Override
        public DnsRecordCodec<?> fallback() {
            return DnsRecordCodecs.fallback();
        }

        @Override
        public DnsRecordType fromString(String name) {
            return DnsRecordType.valueOf(name);
        }

        @Override
        public DnsRecordType valueOf(int val) {
            return DnsRecordType.valueOf(val);
        }
    }

    /**
     * Create a new empty registry builder for assembling a registry. Standard codecs are available from static methods
     * on DnsRecordCodec.
     *
     * @return A registry builder
     */
    public static CodecRegistryBuilder<DnsRecordType> builder() {
        return CodecRegistry.builder(new DnsRecordInternal());
    }

    /**
     * Create a builder with the default codecs for A, AAAA, CNAME, DNAME, PTR, NS, SOA, TXT and LOC records already
     * registered.
     *
     * @return A builder
     */
    public static CodecRegistryBuilder<DnsRecordType> builderWithDefaultCodecs() {
        CodecRegistryBuilder<DnsRecordType> result = builder();
        result.add(DnsRecordCodecs.ipv4Address(), A)
                .add(DnsRecordCodecs.ipv6Address(), AAAA)
                .add(DnsRecordCodecs.name(), CNAME, DNAME, PTR, NS)
                .add(DnsRecordCodecs.startOfAuthority(), SOA)
                .add(DnsRecordCodecs.text(), TXT)
                .add(DnsRecordCodecs.location(), LOC)
                .add(DnsRecordCodecs.opt(), OPT)
                .add(DnsRecordCodecs.service(), SRV)
                .add(DnsRecordCodecs.mailExchanger(), MX)
                .add(DnsRecordCodecs.nsec(), NSEC)
                .add(DnsRecordCodecs.uri(), URI);
        return result;
    }
}
