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
package io.netty.codec.dns.protocol.types;

import java.math.BigInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class Ipv6AddressTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid1() {
        assertNull(new Ipv6Address(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid2() {
        new Ipv6Address("0000:0000:0000:0000:0000:0000:0000:0000:0001");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid3() {
        new Ipv6Address("Hello");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid4() {
        new Ipv6Address("0000:0000:0000:0000:0000:0000:0000:0000:0001:0002:0003:0004");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid5() {
        new Ipv6Address("0000:30000:0000:0000:0000:0000:0000:0000:0001:0002:0003:0004");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid0() {
        new Ipv6Address("0000:3000:0000:0000:0000:p00p:0000:0000:0001:0002:0003:0004");
    }

    @Test
    public void testToString() {
        Ipv6Address addr = new Ipv6Address(0, 1);
        assertEquals(1, addr.low());
        assertEquals(0, addr.high());
        assertEquals("0000:0000:0000:0000:0000:0000:0000:0001", addr.toString());
    }

    @Test
    public void testStringConversion() {
        Ipv6Address addr = new Ipv6Address("2008:FEED:3c20:0128:0004:0003:0002:0001");
        assertEquals("2008:FEED:3c20:0128:0004:0003:0002:0001".toLowerCase(), addr.toString());
    }

    @Test
    public void testToIntArray() {
        Ipv6Address addr = new Ipv6Address("2008:FEED:3c20:0128:0004:0003:0002:0001");
        int[] ints = addr.toIntArray();
        assertEquals(8, ints.length);
    }

    @Test
    public void testToBigInteger() {
        Ipv6Address addr = new Ipv6Address("2008:FEED:3c20:0128:0004:0003:0002:0001");
        BigInteger bi = addr.toBigInteger();
        assertEquals(addr, new Ipv6Address(bi));
    }

    @Test
    public void testConversion() {
        Ipv6Address addr = new Ipv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        Ipv6Address addr2 = new Ipv6Address(addr.high(), addr.low());
        assertEquals(addr, addr2);
        Ipv6Address addr4 = new Ipv6Address(addr.toByteArray());
        assertEquals(addr, addr4);
        Ipv6Address addr3 = new Ipv6Address(addr.toIntArray());
        assertEquals(addr.low(), addr3.low());
        assertEquals(addr.high(), addr3.high());
        assertEquals(addr, addr3);
        assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", addr.toString());
    }

    @Test
    public void testShorthand() {
        Ipv6Address addr = new Ipv6Address("0001:0002:0003:0000:0000:0006:0007:0008");
        assertEquals("1:2:3::6:7:8", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0002:0003:0004:0005:0006:0007:0008");
        assertEquals("1:2:3:4:5:6:7:8", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0001:0002:0003:0004:0005:0006:0007");
        assertEquals("::1:2:3:4:5:6:7", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0000:0000:0004:0000:0000:0000:0008");
        assertEquals("1:0:0:4::8", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0002:0003:0004:0005:0006:0000:0000");
        assertEquals("1:2:3:4:5:6::", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0002:0003:0004:0005:0006:0007:0000");
        assertEquals("1:2:3:4:5:6:7::", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0002:0003:0004:0005:0006:0000:0000");
        assertEquals("0:2:3:4:5:6::", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        assertEquals("2001:db8:85a3::8a2e:370:7334", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0db8:85a3:0000:0000:8a2e:0370:7334");
        assertEquals("0:db8:85a3::8a2e:370:7334", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0000:85a3:0000:0000:0000:0370:7334");
        assertEquals("0:0:85a3::370:7334", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0000:0000:0000:0000:0000:0000:0001");
        assertEquals("::1", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0000:0000:0000:0000:0000:0000:0000");
        assertEquals("::", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0000:0000:0000:0000:0000:0000:0000");
        assertEquals("1::", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0000:0000:0000:0000:0000:0000:0001");
        assertEquals("1::1", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));
    }

    @Test
    public void testToString2() {
        String base = "fe80:0000:0000:0000:184d:1cbc:f0dd:e656";
        Ipv6Address addr = new Ipv6Address(base);
        assertEquals(base, addr.toString());
        assertFalse(addr.toStringShorthand().contains("%"));
        assertEquals(addr, new Ipv6Address(addr.toString()));
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));
        assertEquals("fe80::184d:1cbc:f0dd:e656", addr.toStringShorthand());
    }
}
