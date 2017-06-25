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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.codec.dns.protocol.types.OptRecords;
import io.netty.codec.dns.protocol.types.OptSubrecord;
import io.netty.handler.codec.dns.AbstractDnsRecord;
import io.netty.handler.codec.dns.DnsClass;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.util.internal.ObjectUtil;
import java.lang.reflect.Array;

import static io.netty.handler.codec.dns.DnsClass.IN;
import static io.netty.handler.codec.dns.DnsRecordType.OPT;
import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.StringUtil.charSequenceHashCode;
import static io.netty.util.internal.StringUtil.charSequencesEqual;

/**
 * DNS record implementation in which the payload has been parsed into a record-type specific Java object.
 */
public final class DefaultTypedDnsRecord<T> extends AbstractDnsRecord implements TypedDnsRecord<T> {

    private final T content;

    public DefaultTypedDnsRecord(T payload, CharSequence name, DnsRecordType type) {
        this(payload, name, type, DnsClass.valueOf(0), 0);
    }

    public DefaultTypedDnsRecord(T payload, CharSequence name, DnsRecordType type, long ttl) {
        this(payload, name, type, IN, ttl);
    }

    public DefaultTypedDnsRecord(T payload, CharSequence name, DnsRecordType type, DnsClass dnsClass, long ttl) {
        super(name, type, dnsClass, ttl);
        this.content = checkNotNull(payload, "payload");
    }

    public DefaultTypedDnsRecord(T payload, CharSequence name, DnsRecordType type, int dnsClass, long ttl,
            boolean unicast) {
        super(name, type, dnsClass, ttl, unicast);
        this.content = checkNotNull(payload, "payload");
    }

    public DefaultTypedDnsRecord(T payload, OptRecordHeaderFields fields) {
        this(payload, ".", OPT, checkNotNull(fields, "fields").getDnsClassValue(), fields
                .getTtlValue(), false);
    }

    public static TypedDnsRecord<?> forString(CharSequence seq, CharSequence name, DnsRecordType type,
            DnsClass dnsClass, long ttl) {
        return new DefaultTypedDnsRecord<>(seq, name, type, dnsClass, ttl);
    }

    @Override
    public T content() {
        return content;
    }

    @Override
    public String toString() {
        if (OPT.equals(type())) {
            StringBuilder sb = new StringBuilder(";;OPT PSEUDOSECTION\n; EDNS: version: ");
            OptRecordHeaderFields flds = this.optRecordHeaderFields(DnsResponseCode.valueOf(0));
            sb.append(flds.ednsVersion());
            sb.append("; flags:");
            if (flds.z() != 0) {
                sb.append(flds.z());
            }
            sb.append("; udp: ");
            sb.append(flds.udpPayloadSize());
            if (flds.dnsSecOK()) {
                sb.append("; DNSSEC_OK");
            }
            if (flds.extendedRCode() > 15) {
                sb.append("; rcode: ").append(flds.extendedRCode());
            }
            if (content instanceof OptRecords) {
                for (OptSubrecord<?> rec : (OptRecords) content) {
                    sb.append("\n;").append(rec.type().name()).append(": ").append(toString(rec.content()));
                }
            } else {
                sb.append(" ").append(content);
            }
            return sb.toString();
        } else {
            return name() + "\t" + dnsClass().name() + '\t' + timeToLive() + '\t' + type().name() + '\t' + toString(
                    content());
        }
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    private String toString(Object content) {
        if (content.getClass().isArray()) {
            StringBuilder sb = new StringBuilder();
            int sz = Array.getLength(content);
            for (int i = 0; i < sz; i++) {
                sb.append('"').append("" + Array.get(content, i)).append('"');
                if (i != sz - 1) {
                    sb.append(" ");
                }
            }
            return sb.toString();
        } else {
            return content.toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            if (o instanceof TypedDnsRecord) {
                TypedDnsRecord<?> other = (TypedDnsRecord<?>) o;
                if (other.content() == content()) {
                    return true;
                }
                if (content instanceof CharSequence && other.content() instanceof CharSequence) {
                    return charSequencesEqual((CharSequence) content, (CharSequence) other.content(), true);
                } else {
                    return content.equals(other.content());
                }
            } else if (o instanceof DnsRawRecord && content instanceof ByteBuf) {
                return content.equals(((ByteBufHolder) o).content());
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 3 * contentHashCode();
    }

    private int contentHashCode() {
        if (content instanceof CharSequence) {
            return charSequenceHashCode((CharSequence) content, true);
        } else {
            return content.hashCode();
        }
    }

    @Override
    public OptRecordHeaderFields optRecordHeaderFields(final DnsResponseCode code) {
        return new OptRecordHeaderFields(true) {
            @Override
            public long getTtlValue() {
                return DefaultTypedDnsRecord.this.timeToLive();
            }

            @Override
            void setTtlValue(long ttl) {
                throw new UnsupportedOperationException("Read only");
            }

            @Override
            public int getDnsClassValue() {
                return DefaultTypedDnsRecord.this.dnsClassValue();
            }

            @Override
            void setDnsClassValue(int dnsClass) {
                throw new UnsupportedOperationException("Read only");
            }

            @Override
            void setResponseCodeBits(byte bits) {
                throw new UnsupportedOperationException("Read only");
            }

            @Override
            public byte getResponseCodeBits() {
                return (byte) (code.intValue() & 0xF);
            }
        };
    }
}
