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

import org.junit.Test;
import static org.junit.Assert.*;

public class OptRecordHeaderFieldsTest {

    @Test
    public void testSetExtendedRCode() {
        StandaloneOptRecordHeaderFields r = new StandaloneOptRecordHeaderFields(0, 0, 0, false);
        assertEquals(0, r.extendedRCode());
        for (int i = 1; i < 256 + 15; i++) {
            r.setExtendedRCode(i);
            assertFalse("Negative rcode for " + i + " - "
                    + r.extendedRCode(), r.extendedRCode() < 0);
            assertEquals("Wrong rcode for " + i, i, r.extendedRCode());
            assertEquals(0, r.ednsVersion());
            assertTrue(r.modified());
            assertEquals(0, r.udpPayloadSize());
            assertFalse(r.payloadSizeTouched());
        }
        r.setExtendedRCode(16);
        assertEquals(16, r.extendedRCode());
        assertEquals(0, r.ednsVersion());

        r.setExtendedRCode(15);
        r.setEdnsVersion(0);
        assertFalse(r.ttlTouched());
    }

    @Test
    public void testSetDnsSecOK() {
        StandaloneOptRecordHeaderFields r = new StandaloneOptRecordHeaderFields(0, 0, 0, false);
        assertFalse(r.dnsSecOK());
        r.setDnsSecOK(true);
        assertTrue(r.dnsSecOK());
        assertEquals(0, r.extendedRCode());
        assertEquals(0, r.ednsVersion());
        r.setDnsSecOK(false);
        assertFalse(r.dnsSecOK());

        r.setEdnsVersion(223);
        r.setExtendedRCode(269);
        assertFalse(r.dnsSecOK());
        r.setDnsSecOK(true);
        assertTrue(r.dnsSecOK());
        assertEquals(269, r.extendedRCode());
        assertEquals(223, r.ednsVersion());
    }

    @Test
    public void testSetVersion() {
        StandaloneOptRecordHeaderFields r = new StandaloneOptRecordHeaderFields(0, 0, 0, false);
        assertEquals(0, r.ednsVersion());
        for (int i = 1; i < 256; i++) {
            r.setEdnsVersion((short) i);
            assertEquals(i, r.ednsVersion());
            assertEquals("Wrong rcode for version " + i, 0, r.extendedRCode());
            assertEquals(0, r.udpPayloadSize());
            assertTrue(r.ttlTouched());
            assertFalse(r.payloadSizeTouched());
        }
        r.setEdnsVersion(0);
        assertEquals(0, r.ednsVersion());
        assertEquals(0, r.extendedRCode());
        assertFalse(r.ttlTouched());
    }

    @Test
    public void testSetZ() {
        StandaloneOptRecordHeaderFields r = new StandaloneOptRecordHeaderFields(0, 0, 0, false);
        r.setExtendedRCode(269);
        r.setEdnsVersion(132);
        r.setDnsSecOK(true);
        for (int i = 0; i < 4095; i++) {
            r.setZ(i);
            assertEquals(i, r.z());
            assertEquals(269, r.extendedRCode());
            assertEquals(132, r.ednsVersion());
        }
    }
}
