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
import io.netty.buffer.Unpooled;
import io.netty.codec.dns.protocol.DefaultTypedDnsRecord;
import io.netty.codec.dns.protocol.OptRecordHeaderFields;
import io.netty.codec.dns.protocol.TypedDnsRecord;
import io.netty.codec.dns.protocol.TypedDnsRecordDecoder;
import io.netty.codec.dns.protocol.TypedDnsRecordEncoder;
import io.netty.codec.dns.protocol.optrecords.OptSubrecordType;
import io.netty.codec.dns.protocol.types.ClientSubnet;
import io.netty.codec.dns.protocol.types.Cookies;
import io.netty.codec.dns.protocol.types.Ipv4Address;
import io.netty.codec.dns.protocol.types.OptRecords;
import io.netty.codec.dns.protocol.types.OptSubrecord;
import io.netty.handler.codec.dns.DefaultDnsResponse;
import io.netty.handler.codec.dns.DnsOpCode;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.handler.codec.dns.names.NameCodec;
import io.netty.handler.codec.dns.names.NameCodecFeature;
import io.netty.handler.codec.dns.wire.DnsMessageDecoder;
import io.netty.handler.codec.dns.wire.DnsMessageEncoder;
import java.net.InetSocketAddress;
import org.junit.Test;

import static io.netty.codec.dns.protocol.optrecords.OptSubrecordType.COOKIE;
import static io.netty.codec.dns.protocol.optrecords.OptSubrecordType.ECS;
import static io.netty.handler.codec.dns.DnsOpCode.QUERY;
import static io.netty.handler.codec.dns.DnsOpCode.UPDATE;
import static io.netty.handler.codec.dns.DnsSection.ADDITIONAL;
import static io.netty.handler.codec.dns.names.NameCodecFeature.WRITE_TRAILING_DOT;
import static org.junit.Assert.assertEquals;

public class OptRecordCodecTest {

    static final ClientSubnet<Ipv4Address> subnet = ClientSubnet.ipv4(new Ipv4Address("127.0.0.1"), 0, 0);
    static final OptSubrecord<ClientSubnet<Ipv4Address>> clientSubnetSubrecord
            = new OptSubrecord<>(ECS, subnet);
    static final OptRecordHeaderFields fields = OptRecordHeaderFields.newInstance().setDnsSecOK(true)
            .setEdnsVersion(23).setExtendedRCode(268).setUDPPayloadSize(768);

    static final Cookies cookies = new Cookies(new byte[]{1, 2, 3, 4, 5, 6, 7, 8},
            new byte[]{9, 10, 11, 12, 13, 14, 15, 16});
    static final OptSubrecord<Cookies> cookiesSubrecord = new OptSubrecord<>(COOKIE, cookies);

    static final DefaultTypedDnsRecord<OptRecords> optRecord = new DefaultTypedDnsRecord<OptRecords>(
            OptRecords.of(clientSubnetSubrecord, cookiesSubrecord), fields);

    @Test
    public void testEncodeDecode() throws Exception {
        OptRecordHeaderFields hdrs = OptRecordHeaderFields.newInstance().setDnsSecOK(true).setExtendedRCode(23)
                .setUDPPayloadSize(768).setEdnsVersion(1).setZ(4000);
        OptRecords records = new OptRecords();
        records.add(new OptSubrecord<ClientSubnet>(ECS, ClientSubnet.ipv4(new Ipv4Address("127.0.0.1"),
                0, 0)));
        TypedDnsRecord<OptRecords> optRecord = new DefaultTypedDnsRecord<>(records, hdrs);

        DnsResponse resp = new DefaultDnsResponse(23, UPDATE);
        resp.addRecord(ADDITIONAL, optRecord);

        DnsMessageEncoder enc = DnsMessageEncoder.builder().withRecordEncoder(new TypedDnsRecordEncoder())
                .withNameFeatures(WRITE_TRAILING_DOT)
                .withNameFeatures(WRITE_TRAILING_DOT).build();

        ByteBuf into = Unpooled.buffer();
        enc.encode(resp, into, NameCodec.nonCompressingNameCodec(), 4096);

        DnsMessageDecoder<? extends DnsResponse> decodr = DnsMessageDecoder.builder().withRecordDecoder(
                new TypedDnsRecordDecoder())
                .withNameFeatures(WRITE_TRAILING_DOT).buildResponseDecoder();
        InetSocketAddress addr = InetSocketAddress.createUnresolved("localhost", 53);
        DnsResponse got = decodr.decode(into, addr, addr);
        assertEquals(resp, got);
    }

    @Test
    public void testEncodeDecode2() throws Exception {
        DnsResponse resp = new DefaultDnsResponse(25, QUERY);
        resp.addRecord(ADDITIONAL, optRecord);

        DnsMessageEncoder enc = DnsMessageEncoder.builder().withRecordEncoder(new TypedDnsRecordEncoder())
                .withNameFeatures(WRITE_TRAILING_DOT).build();

        ByteBuf into = Unpooled.buffer();
        enc.encode(resp, into, NameCodec.nonCompressingNameCodec(), 4096);

        DnsMessageDecoder<? extends DnsResponse> decodr = DnsMessageDecoder.builder().withRecordDecoder(
                new TypedDnsRecordDecoder())
                .withNameFeatures(WRITE_TRAILING_DOT).buildResponseDecoder();
        InetSocketAddress addr = InetSocketAddress.createUnresolved("localhost", 53);
        DnsResponse got = decodr.decode(into, addr, addr);
        assertEquals(resp, got);
    }
}
