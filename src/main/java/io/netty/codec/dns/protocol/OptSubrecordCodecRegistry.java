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

import io.netty.codec.dns.protocol.optrecords.OptSubrecordCodecs;
import io.netty.codec.dns.protocol.optrecords.OptSubrecordType;

import static io.netty.codec.dns.protocol.optrecords.OptSubrecordType.COOKIE;
import static io.netty.codec.dns.protocol.optrecords.OptSubrecordType.ECS;

/**
 * Registry of codecs for reading the contents of well-known OPT subrecords.
 * <p>
 * The writer cursor in ByteBufs passed to the methods are positioned at the start of the payload (the caller will write
 * the type and length data - the codec must not), after the payload length property.
 */
public final class OptSubrecordCodecRegistry {

    private OptSubrecordCodecRegistry() {
    }

    public static final CodecRegistry<OptSubrecordType> DEFAULT
            = builderWithDefaultCodecs().build();

    /**
     * Create a new empty registry builder for assembling a registry. Standard codecs are available from static methods
     * on DnsRecordCodec.
     *
     * @return A registry builder
     */
    public static CodecRegistry.CodecRegistryBuilder<OptSubrecordType> builder() {
        return CodecRegistry.builder(new OptSubrecordInternal());
    }

    /**
     * Create a builder with the default codecs for A, AAAA, CNAME, DNAME, PTR, NS, SOA, TXT and LOC records already
     * registered.
     *
     * @return A builder
     */
    public static CodecRegistry.CodecRegistryBuilder<OptSubrecordType> builderWithDefaultCodecs() {
        CodecRegistry.CodecRegistryBuilder<OptSubrecordType> result = builder();
        result.add(OptSubrecordCodecs.ecs(), ECS)
                .add(OptSubrecordCodecs.cookie(), COOKIE);
        return result;
    }

    private static final class OptSubrecordInternal implements CodecRegistry.RegistryInternal<OptSubrecordType> {

        @Override
        public int intValueFor(OptSubrecordType type) {
            return type.intValue();
        }

        @Override
        public DnsRecordCodec<?> fallback() {
            return OptSubrecordCodecs.fallback();
        }

        @Override
        public OptSubrecordType fromString(String s) {
            return OptSubrecordType.valueOf(s);
        }

        @Override
        public OptSubrecordType valueOf(int val) {
            return OptSubrecordType.valueOf(val);
        }
    }
}
