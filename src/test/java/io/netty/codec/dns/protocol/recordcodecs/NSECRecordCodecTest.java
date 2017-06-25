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
import io.netty.codec.dns.protocol.types.NextSecureRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Test;

import static io.netty.handler.codec.dns.DnsRecordType.*;
import static io.netty.handler.codec.dns.names.NameCodecFeature.READ_TRAILING_DOT;
import static io.netty.handler.codec.dns.names.NameCodecFeature.WRITE_TRAILING_DOT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NSECRecordCodecTest {

    private static final byte[] DATA = new byte[]{
        0x04, 'h', 'o', 's', 't',
        0x07, 'e', 'x', 'a', 'm', 'p', 'l', 'e',
        0x03, 'c', 'o', 'm', 0x00, 0x00,
        0x06, 0x40, 0x01, 0x00, 0x00, 0x00, 0x03, 0x04, 0x1b,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x20
    };

    private static final String STRING_DATA = "host.example.com A MX RRSIG NSEC TYPE1234";

    @Test
    public void testDecode() throws IOException {
        NSECRecordCodec codec = new NSECRecordCodec();
        // CAA, DLV are high
        NextSecureRecord rec = new NextSecureRecord("foo.bar.example", AAAA, NS, SOA, MX);

        ByteBuf buf = Unpooled.buffer();
        codec.write(rec, NameCodec.nonCompressingNameCodec(), buf);

        NextSecureRecord decoded = codec.read(buf, NameCodec.nonCompressingNameCodec(), buf.writerIndex());

        assertEquals(rec.nextRecord.toString(), decoded.nextRecord.toString());
        assertTrue(decoded.types.contains(NS));
        assertTrue(decoded.types.contains(AAAA));
        assertTrue(decoded.types.contains(SOA));
        assertTrue(decoded.types.contains(MX));
    }

    private static String binaryArrayString(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            byte by = b[i];
            String hx = Integer.toHexString(by & 0xFF);
            sb.append(" ").append(i).append(":");
            sb.append("0x");
            if (hx.length() == 1) {
                sb.append('0');
            }
            sb.append(hx);
        }
        return sb.toString();
    }

    private static String hexArrayString(byte[] b) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            byte curr = b[i];
            CharSequence hx = toCharOrString(curr);
            result.append(" ").append(i).append(":");
            result.append(hx);
        }
        return result.toString();
    }

    private static String toCharOrString(byte b) {
        int val = b & 0xFF;
        if ((val >= '0' && val <= '9')
                || (val >= 'A' && val <= 'Z')
                || (val >= 'a' && val <= 'z')
                || val == '=' || val == '\'' || val == '.') {
            return "'" + new String(new char[]{(char) val}) + "'";
        }
        return Integer.toString(val);
    }

    private static String toDecoratedBinaryArrayString(byte[] b) {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            byte by = b[i];
            CharSequence hx = bs(by & 0xFF);
            int pos = sb.length();
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(hx);
            String hv = i + ":" + toCharOrString(by);
            sb2.append(hv);
            char[] c = new char[8 - hv.length()];
            Arrays.fill(c, ' ');
            sb2.append(c).append(' ');
        }
        sb.append('\n').append(sb2);
        return sb.toString();
    }

    @Test
    public void testRFCExample() throws IOException {
        NSECRecordCodec codec = new NSECRecordCodec();
        // host.example.com. A MX RRSIG NSEC TYPE1234

        // should have set bits 1, 15, 46, 47, 1234
        NextSecureRecord rec = new NextSecureRecord("host.example.com", A, MX, RRSIG, NSEC,
                DnsRecordType.valueOf(1234));
        ByteBuf test = Unpooled.buffer();
        codec.write(rec, NameCodec.get(READ_TRAILING_DOT, WRITE_TRAILING_DOT), test);
        byte[] got = new byte[test.readableBytes()];
        test.readBytes(got);

        boolean failure = false;
        for (int i = 0; i < Math.min(got.length, DATA.length); i++) {
            byte a = DATA[i];
            byte b = got[i];
            if (a != b) {
                System.out.println(i + ": mismatch " + a + " vs " + b);
            }
        }
        if (failure) {
            System.out.println(toDecoratedBinaryArrayString(DATA));
            System.out.println(toDecoratedBinaryArrayString(got));
        }
        assertArrayEquals(DATA, got);

        ByteBuf buf = Unpooled.wrappedBuffer(DATA);
        NextSecureRecord decoded = codec.read(buf, NameCodec.nonCompressingNameCodec(), buf.writerIndex());

        assertEquals("host.example.com", decoded.nextRecord.toString());

        assertTrue(decoded.types.contains(A));
        assertTrue(decoded.types.contains(MX));
        assertTrue(decoded.types.contains(RRSIG));
        assertTrue(decoded.types.contains(NSEC));
        assertTrue(decoded.types.contains(DnsRecordType.valueOf(1234)));

        assertEquals(STRING_DATA, decoded.toString());
    }

    private static CharSequence bs(int val) {
        StringBuilder sb = new StringBuilder(Integer.toBinaryString(val));
        char[] zeros = new char[8 - sb.length()];
        Arrays.fill(zeros, '0');
        sb.insert(0, zeros);
        return sb;
    }
}
