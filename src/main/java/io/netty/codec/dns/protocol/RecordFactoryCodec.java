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
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.names.NameCodec;

/**
 * Extension to DnsRecordCodec which takes responsibility for decoding the entire DNS record, not just the payload -
 * needed for types such as SRV records, where name field is used atypically, and should be treated as part of the
 * <i>value</i> of the record, not its header.
 */
public abstract class RecordFactoryCodec<T> extends DnsRecordCodec<T> {

    protected RecordFactoryCodec(Class<? super T> type) {
        super(type);
    }

    public abstract TypedDnsRecord<T> decodeRecord(CharSequence name,
            DnsRecordType type, int dnsClass, long timeToLive, ByteBuf in,
            int length, NameCodec names, boolean mdns) throws Exception;
}
