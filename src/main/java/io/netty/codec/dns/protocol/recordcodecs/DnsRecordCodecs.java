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
import io.netty.codec.dns.protocol.optrecords.OptSubrecordType;
import io.netty.codec.dns.protocol.types.Ipv4Address;
import io.netty.codec.dns.protocol.types.Ipv6Address;
import io.netty.codec.dns.protocol.types.Location;
import io.netty.codec.dns.protocol.types.MailExchanger;
import io.netty.codec.dns.protocol.types.NextSecureRecord;
import io.netty.codec.dns.protocol.types.OptRecords;
import io.netty.codec.dns.protocol.types.ServiceDetails;
import io.netty.codec.dns.protocol.types.StartOfAuthority;
import io.netty.codec.dns.protocol.types.UriInfo;

import static io.netty.codec.dns.protocol.OptSubrecordCodecRegistry.DEFAULT;

/**
 * Has static methods for fetching individual record codecs.
 */
public final class DnsRecordCodecs {

    static final DnsRecordCodec<ByteBuf> FALLBACK = new FallbackRecordCodec();

    private DnsRecordCodecs() {
    }

    public static DnsRecordCodec<UriInfo> uri() {
        return new UriRecordCodec();
    }

    public static DnsRecordCodec<NextSecureRecord> nsec() {
        return new NSECRecordCodec();
    }

    public static DnsRecordCodec<ByteBuf> fallback() {
        return FALLBACK;
    }

    public static DnsRecordCodec<ServiceDetails> service() {
        return new ServiceRecordCodec();
    }

    public static DnsRecordCodec<OptRecords> opt(CodecRegistry<OptSubrecordType> registry) {
        return new OptRecordCodec(registry);
    }

    public static DnsRecordCodec<OptRecords> opt() {
        return new OptRecordCodec(DEFAULT);
    }

    /**
     * Get a codec for DNS location records.
     *
     * @return A codec
     */
    public static DnsRecordCodec<Location> location() {
        return new LocationRecordCodec();
    }

    /**
     * Get a codec for reading DNS names.
     *
     * @return A codec
     */
    public static DnsRecordCodec<CharSequence> name() {
        return new NameRecordCodec();
    }

    /**
     * Get a codec for reading IPv4 addresses.
     *
     * @return A codec
     */
    public static DnsRecordCodec<Ipv4Address> ipv4Address() {
        return new Ipv4AddressRecordCodec();
    }

    /**
     * Get a codec for reading IPv6 addresses.
     *
     * @return A codec
     */
    public static DnsRecordCodec<Ipv6Address> ipv6Address() {
        return new Ipv6AddressRecordCodec();
    }

    /**
     * Get a codec for reading mail exchanger records.
     *
     * @return A codec
     */
    public static DnsRecordCodec<MailExchanger> mailExchanger() {
        return new MailExchangerRecordCodec();
    }

    /**
     * Get a codec for reading DNS text records as arrays of strings, where
     * <ul>
     * <li>Quote-delimited elements are a single string</li>
     * <li>If no quotes are present, whitespace delimits individual strings</li>
     * </ul>
     * <p>
     * Per the RFC, text encoded by this codec must be ASCII and must be no more than 255 bytes in length, including any
     * quotes that need to be added to properly delimit individual strings and backslashes used to escape any quote
     * characters that occur in the passed strings. To calculate the number of bytes needed, take the number of
     * characters in all strings; then add one for each quote character, and two for any string that contains
     * whitespace.
     * </p>
     *
     * @return A codec
     */
    public static DnsRecordCodec<CharSequence[]> text() {
        return new TextRecordCodec();
    }

    /**
     * Get a codec for reading start of authority records.
     *
     * @return A codec
     */
    public static DnsRecordCodec<StartOfAuthority> startOfAuthority() {
        return new StartOfAuthorityRecordCodec();
    }
}
