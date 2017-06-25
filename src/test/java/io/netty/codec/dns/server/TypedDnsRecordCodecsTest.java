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
package io.netty.codec.dns.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.codec.dns.protocol.DefaultTypedDnsRecord;
import io.netty.codec.dns.protocol.OptRecordHeaderFields;
import io.netty.codec.dns.protocol.ServiceRecord;
import io.netty.codec.dns.protocol.TypedDnsRecord;
import io.netty.codec.dns.protocol.TypedDnsRecordDecoder;
import io.netty.codec.dns.protocol.TypedDnsRecordEncoder;
import io.netty.codec.dns.protocol.types.ClientSubnet;
import io.netty.codec.dns.protocol.types.Cookies;
import io.netty.codec.dns.protocol.types.Ipv4Address;
import io.netty.codec.dns.protocol.types.Ipv6Address;
import io.netty.codec.dns.protocol.types.Location;
import io.netty.codec.dns.protocol.types.MailExchanger;
import io.netty.codec.dns.protocol.types.OptRecords;
import io.netty.codec.dns.protocol.types.OptSubrecord;
import io.netty.codec.dns.protocol.types.ServiceDetails;
import io.netty.codec.dns.protocol.types.ServiceLocation;
import io.netty.codec.dns.protocol.types.StartOfAuthority;
import io.netty.codec.dns.protocol.types.UriInfo;
import io.netty.handler.codec.dns.DatagramDnsResponse;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.handler.codec.dns.names.NameCodec;
import io.netty.handler.codec.dns.wire.DnsMessageDecoder;
import io.netty.handler.codec.dns.wire.DnsMessageEncoder;
import io.netty.handler.codec.dns.wire.InvalidDnsRecordException;
import io.netty.util.AsciiString;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.Test;

import static io.netty.buffer.ByteBufAllocator.DEFAULT;
import static io.netty.buffer.Unpooled.buffer;
import static io.netty.codec.dns.protocol.optrecords.OptSubrecordType.COOKIE;
import static io.netty.codec.dns.protocol.optrecords.OptSubrecordType.ECS;
import static io.netty.handler.codec.dns.DnsClass.*;
import static io.netty.handler.codec.dns.DnsOpCode.QUERY;
import static io.netty.handler.codec.dns.DnsRecordType.*;
import static io.netty.handler.codec.dns.DnsResponseCode.BADCOOKIE;
import static io.netty.handler.codec.dns.DnsResponseCode.NOERROR;
import static io.netty.handler.codec.dns.DnsSection.ANSWER;
import static io.netty.handler.codec.dns.DnsSection.AUTHORITY;
import static io.netty.handler.codec.dns.DnsSection.QUESTION;
import static io.netty.handler.codec.dns.names.NameCodec.compressingNameCodec;
import static io.netty.handler.codec.dns.names.NameCodecFeature.COMPRESSION;
import static io.netty.handler.codec.dns.names.NameCodecFeature.MDNS_UTF_8;
import static io.netty.handler.codec.dns.names.NameCodecFeature.WRITE_TRAILING_DOT;
import static io.netty.handler.codec.dns.wire.IllegalRecordPolicy.INCLUDE;
import static io.netty.handler.codec.dns.wire.IllegalRecordPolicy.THROW;
import static io.netty.util.internal.StringUtil.charSequencesEqual;
import static org.junit.Assert.*;

public class TypedDnsRecordCodecsTest {

    static final ClientSubnet<Ipv4Address> subnet = ClientSubnet.ipv4(new Ipv4Address("127.0.0.1"), 0, 0);
    static final OptSubrecord<ClientSubnet<Ipv4Address>> clientSubnetSubrecord = new OptSubrecord<>(ECS, subnet);
    static final OptRecordHeaderFields fields = OptRecordHeaderFields.newInstance().setDnsSecOK(true)
            .setEdnsVersion(42).setExtendedRCode(BADCOOKIE.intValue()).setUDPPayloadSize(768);

    static final Cookies cookies = new Cookies(new byte[]{1, 2, 3, 4, 5, 6, 7, 8},
            new byte[]{9, 10, 11, 12, 13, 14, 15, 16});
    static final OptSubrecord<Cookies> cookiesSubrecord = new OptSubrecord<>(COOKIE, cookies);

    static final DefaultTypedDnsRecord<OptRecords> optRecord = new DefaultTypedDnsRecord<OptRecords>(
            OptRecords.of(clientSubnetSubrecord, cookiesSubrecord), fields);
    private static final InetSocketAddress SENDER = new InetSocketAddress("127.0.0.1", 53);
    private static final InetSocketAddress RECIPIENT = new InetSocketAddress("127.0.0.1", 32353);

    @Test
    public void testRecordEncoders() throws Exception {
        assertTrue(fields.dnsSecOK());
        assertEquals(23, fields.extendedRCode());
        assertEquals(768, fields.udpPayloadSize());
        assertEquals(42, fields.ednsVersion());

        List<DnsRecord> records = new ArrayList<>();
        records.add(new DefaultTypedDnsRecord<>(new Ipv4Address("127.0.0.1"), "foo.example", A, HESIOD, 10));
        records.add(new DefaultTypedDnsRecord<>(new Ipv6Address("fe80::3602:86ff:fe27:5a56"), "foo6.example", AAAA,
                CHAOS, 11));
        records.add(new DefaultTypedDnsRecord<>(new MailExchanger(2, "mail.foo.example"), "foo.example", MX, IN, 12));
        records.add(new DefaultTypedDnsRecord<>(new Location((byte) 1, (byte) 0, (byte) 1, (byte) 2, 352, -80, 55),
                "foo.example", LOC, CSNET, 13));
        records.add(new DefaultTypedDnsRecord<>(
                new StartOfAuthority("ns1.foo.example", "root.foo.example", 1234, 20, 40, 360000, 86200), "foo.example",
                SOA, IN, 12));
        records.add(optRecord);

        TypedDnsRecordDecoder dec = new TypedDnsRecordDecoder();
        TypedDnsRecordEncoder enc = new TypedDnsRecordEncoder();

        ByteBuf buf = buffer();
        int ix = 0;
        try (NameCodec names = compressingNameCodec()) {
            for (DnsRecord rec : records) {
                enc.encodeRecord(names, rec, buf, Integer.MAX_VALUE);
            }
        }
        List<DnsRecord> got = new ArrayList<>();
        try (NameCodec names = compressingNameCodec()) {
            for (int i = 0; i < records.size(); i++) {
                DnsRecord decoded = dec.decodeRecord(buf, names);
                if (decoded.type() == OPT && decoded instanceof TypedDnsRecord<?>) {
                    OptRecordHeaderFields flds = ((TypedDnsRecord<?>) decoded).optRecordHeaderFields(BADCOOKIE);
                    assertEquals(fields.dnsSecOK(), flds.dnsSecOK());
                    assertEquals(fields.z(), flds.z());
                    assertEquals(fields.ednsVersion(), flds.ednsVersion());
                    assertEquals(fields.extendedRCode(), flds.extendedRCode());
                }
                got.add(decoded);
            }
        }
        assertEquals(records.size(), got.size());
        for (int i = 0; i < got.size(); i++) {
            DnsRecord expect = records.get(i);
            DnsRecord found = got.get(i);
            if (expect.timeToLive() != found.timeToLive()) {
                fail("Time to live mismatch - expected " + Long.toHexString(expect.timeToLive()) + " vs. " + Long
                        .toHexString(found.timeToLive())
                        + " aka " + Long.toBinaryString(expect.timeToLive()) + " vs. " + Long.toBinaryString(found
                        .timeToLive()));
            }
            if (!expect.equals(got)) {
                assertEquals(expect.type().intValue(), found.type().intValue());
                assertEquals(expect.timeToLive(), found.timeToLive());
                assertEquals(expect.dnsClassValue(), found.dnsClassValue());
                assertEquals(expect.dnsClass(), found.dnsClass());
                assertEquals(expect.name().toString(), found.name().toString());
            }

            assertEquals("Mismatch on record " + i, expect, found);
        }
        assertEquals(records, got);
    }

    @Test(expected = InvalidDnsRecordException.class)
    public void testNoSoaRecordsInMdns() throws Exception {
        DnsMessageEncoder encoder = DnsMessageEncoder.builder()
                .withRecordEncoder(new TypedDnsRecordEncoder(true))
                .mDNS()
                .withIllegalRecordPolicy(THROW)
                .withNameFeatures(MDNS_UTF_8, WRITE_TRAILING_DOT)
                .build();
        DnsMessageDecoder<? extends DnsResponse> decoder = DnsMessageDecoder.builder()
                .mDNS()
                .withIllegalRecordPolicy(THROW)
                .withRecordDecoder(new TypedDnsRecordDecoder(true))
                .withNameFeatures(MDNS_UTF_8, WRITE_TRAILING_DOT)
                .buildResponseDecoder();

        DatagramDnsResponse orig = createResponse();

        ChannelHandlerContext x = new MockContext();
        ByteBuf buf = encoder.encode(x, orig);
        DnsResponse<?> decoded = decoder.decode(buf, orig.sender(), orig.recipient());
    }

    @Test
    public void testUnicastSrvNamesNotCompressed() throws Exception {
        // mDNS spec specifies that compression may be used for SRV records
        // which are *not* unicast, but for compatibility with unicast DNS,
        // SRV records which have the unicast bit set must not use name
        // compression
        DnsMessageEncoder encoder = DnsMessageEncoder.builder()
                .withRecordEncoder(new TypedDnsRecordEncoder(true))
                .mDNS()
                .withIllegalRecordPolicy(INCLUDE)
                .withNameFeatures(MDNS_UTF_8, COMPRESSION, WRITE_TRAILING_DOT)
                .build();
        DnsMessageDecoder<? extends DnsResponse> decoder = DnsMessageDecoder.builder()
                .mDNS()
                .withIllegalRecordPolicy(INCLUDE)
                .withRecordDecoder(new TypedDnsRecordDecoder(true))
                .withNameFeatures(MDNS_UTF_8, COMPRESSION, WRITE_TRAILING_DOT)
                .buildResponseDecoder();
        DatagramDnsResponse resp = new DatagramDnsResponse(SENDER, RECIPIENT, 23, QUERY);

        resp.addRecord(QUESTION, new DefaultDnsQuestion("somelaptop.somewhere.com", SRV, CHAOS, true));
        resp.addRecord(ANSWER, new ServiceRecord(new ServiceLocation("_ssh._tcp.somelaptop.somewhere.com"),
                new ServiceDetails(10, 5, 22, "somelaptop.somewhere.com"), 82300L, CHAOS.intValue(), true));

        ChannelHandlerContext x = new MockContext();
        ByteBuf buf = encoder.encode(x, resp);
        DnsResponse<?> decoded = decoder.decode(buf.duplicate(), resp.sender(), resp.recipient());

        DnsRecord q = decoded.recordAt(QUESTION);
        DnsRecord ans = decoded.recordAt(ANSWER);
        assertNotNull(q);
        assertNotNull(ans);

        assertEquals(SRV, q.type());
        assertEquals(SRV, ans.type());

        assertEquals("somelaptop.somewhere.com", q.name().toString());
        assertTrue(ans instanceof ServiceRecord);
        ServiceRecord sr = (ServiceRecord) ans;

        assertEquals("ssh", sr.location().serviceName);
        assertEquals("tcp", sr.location().protocol);
        assertEquals("somelaptop.somewhere.com", sr.location().host);
        assertEquals(10, sr.content().priority);
        assertEquals(5, sr.content().weight);
        assertEquals(22, sr.content().port);

        DnsMessageEncoder noMdnsEncoder = DnsMessageEncoder.builder()
                .withRecordEncoder(new TypedDnsRecordEncoder(false))
                .mDNS()
                .withIllegalRecordPolicy(INCLUDE)
                .withNameFeatures(WRITE_TRAILING_DOT)
                .build();
        DnsMessageDecoder<? extends DnsResponse> noMdnsDecoder = DnsMessageDecoder.builder()
                .withIllegalRecordPolicy(INCLUDE)
                .withRecordDecoder(new TypedDnsRecordDecoder(false))
                .withNameFeatures(WRITE_TRAILING_DOT)
                .buildResponseDecoder();

        ByteBuf buf2 = noMdnsEncoder.encode(x, resp);
        DnsResponse<?> otherDecoded = noMdnsDecoder.decode(buf.duplicate(),
                resp.sender(), resp.recipient());

        // If compression was used in encoding the first one, the original
        // buffer will have a shorted length
        assertEquals(buf2.readableBytes(), buf.readableBytes());
        assertEquals(decoded, otherDecoded);
    }

    @Test
    public void testEncodeDecodeResponse() throws Exception {
        DnsMessageEncoder encoder = DnsMessageEncoder.builder()
                .withRecordEncoder(new TypedDnsRecordEncoder(true))
                .mDNS()
                .withIllegalRecordPolicy(INCLUDE)
                .withNameFeatures(MDNS_UTF_8, WRITE_TRAILING_DOT)
                .build();
        DnsMessageDecoder<? extends DnsResponse> decoder = DnsMessageDecoder.builder()
                .mDNS()
                .withIllegalRecordPolicy(INCLUDE)
                .withRecordDecoder(new TypedDnsRecordDecoder(true))
                .withNameFeatures(MDNS_UTF_8, WRITE_TRAILING_DOT)
                .buildResponseDecoder();

        DatagramDnsResponse orig = createResponse();

        ChannelHandlerContext x = new MockContext();
        ByteBuf buf = encoder.encode(x, orig);
        DnsResponse<?> decoded = decoder.decode(buf, orig.sender(), orig.recipient());

        assertEquals(orig.z(), decoded.z());
        assertEquals(orig.code(), decoded.code());
        assertEquals(orig.count(), decoded.count());
        assertEquals(orig.id(), decoded.id());
        assertEquals(orig.isAuthoritativeAnswer(), decoded.isAuthoritativeAnswer());
        assertEquals(orig.isRecursionAvailable(), decoded.isRecursionAvailable());
        assertEquals(orig.isRecursionDesired(), decoded.isRecursionDesired());
        assertEquals(orig.isTruncated(), decoded.isTruncated());
        assertEquals(orig.opCode(), decoded.opCode());

        for (DnsSection sect : DnsSection.values()) {
            assertEquals(sect + " mismatch", orig.count(sect), decoded.count(sect));
        }
        for (DnsSection sect : DnsSection.values()) {
            assertEquals(sect + " mismatch", orig.count(sect), decoded.count(sect));
            int max = orig.count(sect);
            int max2 = decoded.count(sect);
            assertEquals(max, max2);
            for (int i = 0; i < max; i++) {
                DnsRecord a = orig.recordAt(sect, i);
                DnsRecord b = decoded.recordAt(sect, i);
                assertNotNull(a);
                assertNotNull(b);
                assertNotNull(a + "", a.name());
                assertNotNull(b + "", b.name());
                assertTrue(a.name() + " <-> " + b.name(), charSequencesEqual(a.name(), b.name(), true));
                assertEquals(a.dnsClass(), b.dnsClass());
                assertEquals(a.timeToLive(), b.timeToLive());
                assertEquals(a.type(), b.type());
                assertEquals(a + " and " + b, a.hashCode(), b.hashCode());
                assertEquals(a.getClass(), b.getClass());
                assertEquals("Mismatch in " + sect, a, b);
            }
        }
    }

    private DatagramDnsResponse createResponse() {

        DatagramDnsResponse resp = new DatagramDnsResponse(SENDER, RECIPIENT, 12345);
        resp.setCode(NOERROR);
        resp.setOpCode(QUERY);

        DnsQuestion ques = new DefaultDnsQuestion("google.com", A);

        resp.addRecord(QUESTION, ques);

        DnsRecord rec = new DefaultTypedDnsRecord(new Ipv4Address("8.8.8.8"),
                new AsciiString("google.com"), A, IN, 1000);

        StartOfAuthority auth = new StartOfAuthority("monkey.google.com",
                "root.google.com", 1234, 3000, 1000, 2000, 2);
        DnsRecord raw = new DefaultTypedDnsRecord<>(auth, "google.com", SOA, IN, 1000);
        resp.addRecord(AUTHORITY, raw);
        resp.addRecord(ANSWER, rec);

        resp.addRecord(AUTHORITY, DefaultTypedDnsRecord.forString("ns1.google.com",
                "google.com", NS, IN, 1000));
        resp.addRecord(AUTHORITY, DefaultTypedDnsRecord.forString("ns2.google.com",
                "google.com", NS, IN, 1000));

        resp.addRecord(ANSWER, new ServiceRecord(new ServiceLocation("_ssh._tcp.somehost.example.com"),
                new ServiceDetails(10, 5, 22, "somelaptop.somewhere.com"), 82300L, CHAOS.intValue(), true));

        resp.addRecord(ANSWER, new DefaultTypedDnsRecord(new UriInfo(10, 1, "http://netty.io"), "netty.io", URI));
        return resp;
    }

    static class MockContext implements ChannelHandlerContext {

        @Override
        public Channel channel() {
            return null;
        }

        @Override
        public EventExecutor executor() {
            return null;
        }

        @Override
        public String name() {
            return "x";
        }

        @Override
        public ChannelHandler handler() {
            return null;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }

        @Override
        public ChannelHandlerContext fireChannelRegistered() {
            return this;
        }

        @Override
        public ChannelHandlerContext fireChannelUnregistered() {
            return this;
        }

        @Override
        public ChannelHandlerContext fireChannelActive() {
            return this;
        }

        @Override
        public ChannelHandlerContext fireChannelInactive() {
            return this;
        }

        @Override
        public ChannelHandlerContext fireExceptionCaught(Throwable cause) {
            return this;
        }

        @Override
        public ChannelHandlerContext fireUserEventTriggered(Object event) {
            return this;
        }

        @Override
        public ChannelHandlerContext fireChannelRead(Object msg) {
            return this;
        }

        @Override
        public ChannelHandlerContext fireChannelReadComplete() {
            return this;
        }

        @Override
        public ChannelHandlerContext fireChannelWritabilityChanged() {
            return this;
        }

        @Override
        public ChannelFuture bind(SocketAddress localAddress) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
            return null;
        }

        @Override
        public ChannelFuture disconnect() {
            return null;
        }

        @Override
        public ChannelFuture close() {
            return null;
        }

        @Override
        public ChannelFuture deregister() {
            return null;
        }

        @Override
        public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture disconnect(ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture close(ChannelPromise promise) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelFuture deregister(ChannelPromise promise) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelHandlerContext read() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelFuture write(Object msg) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelFuture write(Object msg, ChannelPromise promise) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelHandlerContext flush() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelPipeline pipeline() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ByteBufAllocator alloc() {
            return DEFAULT;
        }

        @Override
        public ChannelPromise newPromise() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelProgressivePromise newProgressivePromise() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelFuture newSucceededFuture() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelFuture newFailedFuture(Throwable cause) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ChannelPromise voidPromise() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        List<A<?>> attrs = new ArrayList<>();

        @Override
        @SuppressWarnings("deprecation")
        public <T> Attribute<T> attr(AttributeKey<T> key) {
            for (A<?> a : attrs) {
                if (key.equals(a.key)) {
                    return (Attribute<T>) a;
                }
            }
            A<T> nue = new A<>(key);
            attrs.add(nue);
            return nue;
        }

        @Override
        @SuppressWarnings("deprecation")
        public <T> boolean hasAttr(AttributeKey<T> key) {
            for (A<?> a : attrs) {
                if (key.equals(a.key)) {
                    return true;
                }
            }
            return false;
        }

        static final class A<T> implements Attribute<T> {

            private final AttributeKey<T> key;
            T obj;

            public A(AttributeKey<T> key) {
                this.key = key;
            }

            @Override
            public AttributeKey<T> key() {
                return key;
            }

            @Override
            public T get() {
                return obj;
            }

            @Override
            public void set(T value) {
                obj = value;
            }

            @Override
            public T getAndSet(T value) {
                T old = obj;
                obj = value;
                return old;
            }

            @Override
            public T setIfAbsent(T value) {
                if (obj == null) {
                    obj = value;
                }
                return obj;
            }

            @Override
            @SuppressWarnings("deprecation")
            public T getAndRemove() {
                T result = obj;
                obj = null;
                return result;
            }

            @Override
            public boolean compareAndSet(T oldValue, T newValue) {
                if (Objects.equals(obj, oldValue)) {
                    obj = newValue;
                    return true;
                }
                return false;
            }

            @Override
            @SuppressWarnings("deprecation")
            public void remove() {
                obj = null;
            }
        }
    }
}
