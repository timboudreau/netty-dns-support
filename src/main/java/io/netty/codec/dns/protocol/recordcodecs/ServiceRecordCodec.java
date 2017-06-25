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
import io.netty.codec.dns.protocol.RecordFactoryCodec;
import io.netty.codec.dns.protocol.ServiceRecord;
import io.netty.codec.dns.protocol.TypedDnsRecord;
import io.netty.codec.dns.protocol.types.ServiceDetails;
import io.netty.codec.dns.protocol.types.ServiceLocation;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.DnsRecordDecoder;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;
import java.nio.charset.UnmappableCharacterException;

import static io.netty.handler.codec.dns.DnsRecordDecoder.MDNS_DNS_CLASS_MASK;
import static io.netty.handler.codec.dns.DnsRecordDecoder.MDNS_UNICAST_RESPONSE_BIT;

final class ServiceRecordCodec extends RecordFactoryCodec<ServiceDetails> {

    ServiceRecordCodec() {
        super(ServiceDetails.class);
    }

    @Override
    public ServiceDetails read(ByteBuf from, NameCodec forReadingNames, int length) throws DnsDecoderException,
            UnmappableCharacterException, IOException {
        int priority = from.readUnsignedShort();
        int weight = from.readUnsignedShort();
        int port = from.readUnsignedShort();
        CharSequence name = forReadingNames.readName(from);
        return new ServiceDetails(priority, weight, port, name);
    }

    @Override
    public void write(ServiceDetails value, NameCodec names, ByteBuf into) throws IOException {
        into.writeShort(value.priority);
        into.writeShort(value.weight);
        into.writeShort(value.port);
        names.writeName(value.name, into);
    }

    @Override
    public TypedDnsRecord<ServiceDetails> decodeRecord(CharSequence name, DnsRecordType type, int dnsClass,
            long timeToLive, ByteBuf in, int length, NameCodec names, boolean mdns) throws UnmappableCharacterException,
            IOException {
        boolean isUnicastResponse = false;
        if (mdns) {
            isUnicastResponse = (dnsClass & MDNS_UNICAST_RESPONSE_BIT) != 0;
            dnsClass &= MDNS_DNS_CLASS_MASK;
        }
        ServiceDetails details = read(in, names, length);
        ServiceLocation loc = new ServiceLocation(name);
        return new ServiceRecord(loc, details, timeToLive, dnsClass, isUnicastResponse);
    }
}
