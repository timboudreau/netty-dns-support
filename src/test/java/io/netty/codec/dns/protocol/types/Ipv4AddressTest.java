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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test of ipv4address.
 */
public class Ipv4AddressTest {

    /**
     * Test of address method, of class Ipv4Address.
     */
    @Test
    public void testAddress() {
        Ipv4Address addr = new Ipv4Address("192.168.2.1");
        int[] ints = addr.toIntArray();
        for (int i = 0; i < ints.length; i++) {
            switch (i) {
                case 0:
                    assertEquals(192, ints[i]);
                    break;
                case 1:
                    assertEquals(168, ints[i]);
                    break;
                case 2:
                    assertEquals(2, ints[i]);
                    break;
                case 3:
                    assertEquals(1, ints[i]);
                    break;
            }
        }
        assertEquals("192.168.2.1", addr.toString());
        assertEquals((int) 3232236033L, addr.intValue());
        assertEquals(3232236033L, addr.longValue());
    }

    /**
     * Test of addressParts method, of class Ipv4Address.
     */
    @Test
    public void testAddressParts() {
        Ipv4Address addr = new Ipv4Address("192.168.2.1");
        Ipv4Address addr2 = new Ipv4Address(addr.intValue());
        assertEquals(addr, addr2);
        Ipv4Address addr3 = new Ipv4Address(addr.toString());
        assertEquals(addr, addr3);
    }
}
