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

/**
 * OPT record types have their own uses for the TTL and udpPayloadSize fields; this gives codecs a way to write them.
 */
final class StandaloneOptRecordHeaderFields extends OptRecordHeaderFields {

    private long ttl;
    private int dnsClass;
    private final long origTtl;
    private final int origDnsClass;
    private byte rcodeBits;
    private final byte origRcodeBits;

    StandaloneOptRecordHeaderFields(long ttl, int dnsClass, int rcodeBits, boolean readOnly) {
        super(readOnly);
        this.ttl = origTtl = ttl;
        this.dnsClass = origDnsClass = dnsClass;
        this.rcodeBits = origRcodeBits = (byte) rcodeBits;
    }

    boolean modified() {
        return ttlTouched()
                || payloadSizeTouched()
                || rcodeBitsTouched();
    }

    boolean ttlTouched() {
        return getTtlValue() != origTtl;
    }

    boolean payloadSizeTouched() {
        return getDnsClassValue() != origDnsClass;
    }

    public boolean rcodeBitsTouched() {
        return origRcodeBits != rcodeBits;
    }

    /**
     * @return the ttl
     */
    @Override
    public long getTtlValue() {
        return ttl;
    }

    /**
     * @param ttl the ttl to set
     */
    @Override
    void setTtlValue(long ttl) {
        this.ttl = ttl;
    }

    /**
     * @return the dnsClass
     */
    @Override
    public int getDnsClassValue() {
        return dnsClass;
    }

    /**
     * @param dnsClass the dnsClass to set
     */
    @Override
    public void setDnsClassValue(int dnsClass) {
        this.dnsClass = dnsClass;
    }

    @Override
    void setResponseCodeBits(byte bits) {
        rcodeBits = bits;
    }

    @Override
    public byte getResponseCodeBits() {
        return rcodeBits;
    }
}
