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
 * A DNS record which has a payload of a specific Java type that models the data in that record. These are decoded by
 * DnsRecordCodec - provide a registry of codecs mapped to DnsRecordTypes to TypedDnsRecordDecoder/Encoder.
 *
 * @param <T> The content type
 * @see DefaultTypedDnsRecord
 */
public interface TypedDnsRecord<T> extends DnsRecord {

    /**
     * Get the Java object which represents the content of this record.
     *
     * @return The content
     */
    T content();

    /**
     * Get the header fields of this record as interpreted by an OPT record, which uses the TTL field differently, and
     * combines the original response code with its own value to form a 12-bit extended response code.
     *
     * @param code The raw response code from the header
     * @return An object that lets you access/manipulate EDNS header fields
     */
    OptRecordHeaderFields optRecordHeaderFields(DnsResponseCode code);
}
