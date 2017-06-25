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

import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsResponseCode;

/**
 * Manages reading and writing the dnsClass, responseCode and ttl fields of an OPT pseudo record, which uses various
 * sets of bits for other purposes than their usual ones. Can be used either to read these values as EDNS values (doing
 * the necessary bit-chewing), or to write them into a new record.
 */
public abstract class OptRecordHeaderFields {

    private static final long Z_MASK = 0x8FFF;
    private static final long INVERTED_Z_MASK = Z_MASK ^ 0xFFFF_FFFF_FFFF_FFFFL;
    private static final int DNS_SEC_OK = 1 << 15;
    private final boolean readOnly;

    protected OptRecordHeaderFields(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public static OptRecordHeaderFields newInstance() {
        return newInstance(0, 0, (byte) 0);
    }

    public static OptRecordHeaderFields newInstance(long ttl, int dnsClass, byte rcodeBits) {
        return new StandaloneOptRecordHeaderFields(ttl, dnsClass, rcodeBits, false);
    }

    public static OptRecordHeaderFields forDnsRecord(DnsRecord record, DnsResponseCode code) {
        return newInstance(record.timeToLive(), record.dnsClassValue(), (byte) (code.intValue() & 0x0F));
    }

    public int ednsVersion() {
        return (int) ((getTtlValue() & 0x00FF0000) >> 16);
    }

    public OptRecordHeaderFields setResponseCode(DnsResponseCode code) {
        if (!code.isEdnsResponseCode()) {
            throw new IllegalArgumentException(code + " is < 16 - not an"
                    + " EDNS response code - use the messages setResponseCode()"
                    + " method.");
        }
        return setExtendedRCode(code.intValue());
    }

    public OptRecordHeaderFields setEdnsVersion(int version) {
        if (readOnly) {
            throw new UnsupportedOperationException("Read-only");
        }
        setTtlValue(getTtlValue() & 0xFF00_FFFF | (version << 16));
        return this;
    }

    public OptRecordHeaderFields setExtendedRCode(int val) {
        if (readOnly) {
            throw new UnsupportedOperationException("Read-only");
        }
        if (val > 4095) {
            throw new IllegalArgumentException("Extended RCODE must be "
                    + "less than 4096");
        }
        int lowBits = val & 0x000F;
        setResponseCodeBits((byte) lowBits);
        int highBits = val >> 4;
        setTtlValue((getTtlValue() & 0x00FF_FFFF) | (highBits << 24));
        return this;
    }

    public long extendedRCode() {
        long highBits = (getTtlValue() & 0xFF00_0000L) >> 24;
        return highBits << 4 | getResponseCodeBits();
    }

    public OptRecordHeaderFields setDnsSecOK(boolean val) {
        if (readOnly) {
            throw new UnsupportedOperationException("Read-only");
        }
        if (val) {
            setTtlValue(getTtlValue() | DNS_SEC_OK);
        } else {
            setTtlValue(getTtlValue() ^ DNS_SEC_OK);
        }
        return this;
    }

    public OptRecordHeaderFields setZ(int value) {
        if (value > Z_MASK) {
            throw new IllegalArgumentException("Max value for z is " + Z_MASK
                    + " but passed " + value);
        }
        if (value < 0) {
            throw new IllegalArgumentException("z may not be negative: " + value);
        }
        long ttl = (getTtlValue() & INVERTED_Z_MASK) | value;
        setTtlValue(ttl);
        return this;
    }

    public int z() {
        return (int) (getTtlValue() & Z_MASK);
    }

    public boolean dnsSecOK() {
        return (getTtlValue() & DNS_SEC_OK) == DNS_SEC_OK;
    }

    public OptRecordHeaderFields setUDPPayloadSize(int dnsClass) {
        if (readOnly) {
            throw new UnsupportedOperationException("Read-only");
        }
        if (dnsClass != this.getDnsClassValue()) {
            this.setDnsClassValue(dnsClass);
        }
        return this;
    }

    public int udpPayloadSize() {
        return getDnsClassValue();
    }

    /**
     * Get the time to live value to store in the DNS record.
     */
    public abstract long getTtlValue();

    /**
     * Get the time to live value stored in the DNS record.
     */
    abstract void setTtlValue(long ttl);

    /**
     * Get the dns class value to store in the DNS record.
     */
    public abstract int getDnsClassValue();

    /**
     * Set the dns class value to store in the DNS record.
     */
    abstract void setDnsClassValue(int dnsClass);

    /**
     * Set the 4-bit value that is the DNS message's response code's portion of the 12-bit extended response code.
     */
    abstract void setResponseCodeBits(byte bits);

    /**
     * Get the 4-bit value that is the DNS message's response code's portion of the 12-bit extended response code.
     */
    public abstract byte getResponseCodeBits();
}
