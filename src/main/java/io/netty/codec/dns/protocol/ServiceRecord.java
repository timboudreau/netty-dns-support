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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.codec.dns.protocol.recordcodecs.DnsRecordCodecs;
import io.netty.codec.dns.protocol.types.ServiceDetails;
import io.netty.codec.dns.protocol.types.ServiceLocation;
import io.netty.handler.codec.dns.DnsClass;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.handler.codec.dns.names.NameCodec;
import io.netty.util.internal.StringUtil;
import java.io.IOException;

import static io.netty.handler.codec.dns.DnsRecordType.SRV;
import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.StringUtil.charSequenceHashCode;
import static io.netty.util.internal.StringUtil.charSequencesEqual;

/**
 * Special record type for ServiceRecord.
 */
public class ServiceRecord implements TypedDnsRecord<ServiceDetails> {

    private final ServiceDetails details;
    private final ServiceLocation location;
    private final long timeToLive;
    private final int dnsClassValue;
    private final boolean unicast;

    public ServiceRecord(ServiceLocation location, ServiceDetails details, long timeToLive, int dnsClassValue,
            boolean unicast) {
        this.details = checkNotNull(details, "details");
        this.location = checkNotNull(location, "location");
        this.timeToLive = timeToLive;
        this.dnsClassValue = dnsClassValue;
        this.unicast = unicast;
    }

    public ServiceLocation location() {
        return location;
    }

    @Override
    public ServiceDetails content() {
        return details;
    }

    @Override
    public CharSequence name() {
        return location.toString();
    }

    @Override
    public DnsRecordType type() {
        return SRV;
    }

    @Override
    public DnsClass dnsClass() {
        return DnsClass.valueOf(dnsClassValue);
    }

    @Override
    public long timeToLive() {
        return timeToLive;
    }

    @Override
    public int dnsClassValue() {
        return dnsClassValue;
    }

    @Override
    public ServiceRecord withTimeToLiveAndDnsClass(long timeToLive, int dnsClass) {
        return new ServiceRecord(location, details, timeToLive, dnsClass, unicast);
    }

    @Override
    public boolean isUnicastOrCacheFlushRequested() {
        return unicast;
    }

    @Override
    public OptRecordHeaderFields optRecordHeaderFields(DnsResponseCode code) {
        throw new UnsupportedOperationException("Not an OPT record.");
    }

    @Override
    public String toString() {
        return name() + "\t" + dnsClass() + "\t" + type().name()
                + "\t" + timeToLive + "\t" + (unicast?"unicast\t":"")
                + details;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof TypedDnsRecord<?>) {
            TypedDnsRecord<?> tdn = (TypedDnsRecord<?>) o;
            if (tdn.timeToLive() == timeToLive
                    && tdn.type().equals(type())
                    && tdn.dnsClassValue() == dnsClassValue
                    && charSequencesEqual(name(), tdn.name(), true)) {
                return tdn.content().equals(details);
            }
        } else if (o instanceof DnsRawRecord) {
            DnsRecord tdn = (DnsRecord) o;
            if (tdn.timeToLive() == timeToLive
                    && tdn.type().equals(type())
                    && tdn.dnsClassValue() == dnsClassValue
                    && charSequencesEqual(name(), tdn.name(), true)) {
                ByteBuf a = Unpooled.buffer();
                try {
                    DnsRecordCodecs.service().write(details, NameCodec.nonCompressingNameCodec(), a);
                    ByteBuf b = ((ByteBufHolder) o).content();
                    return ByteBufUtil.equals(a, b);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + charSequenceHashCode(name(), true);
        hash = 67 * hash + this.type().hashCode();
        hash = 67 * hash + this.dnsClassValue;
        hash = 67 * hash + (int) (this.timeToLive ^ (this.timeToLive >>> 32));
        hash = 67 * hash + details.hashCode();
        return hash;
    }
}
