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

import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.util.internal.StringUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static io.netty.util.internal.StringUtil.charSequencesEqual;

/**
 * A DNS NSEC record - the DNS name of the next alphabetic server, and a list of types of records available for it. Not
 * used heavily in unicast DNS due to the fact that it leaks the names of all hosts on the network (so does NSEC3, for a
 * little more work), but used heavily in mDNS where the network is trusted.
 */
public class NextSecureRecord implements Iterable<DnsRecordType> {

    public final CharSequence nextRecord;
    public final List<DnsRecordType> types;

    public NextSecureRecord(CharSequence nextRecord, DnsRecordType... types) {
        this(nextRecord, Arrays.asList(types));
    }

    public NextSecureRecord(CharSequence nextRecord, List<DnsRecordType> types) {
        this.nextRecord = nextRecord;
        List<DnsRecordType> realTypes = new ArrayList<>(types);
        Collections.sort(realTypes);
        this.types = Collections.unmodifiableList(realTypes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(nextRecord);
        for (DnsRecordType type : types) {
            sb.append(' ');
            if (type.isStandardType()) {
                sb.append(type.name());
            } else {
                sb.append("TYPE").append(type.intValue());
            }
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + nextRecord.hashCode();
        hash = 59 * hash + types.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NextSecureRecord other = (NextSecureRecord) obj;
        if (!charSequencesEqual(this.nextRecord, other.nextRecord, true)) {
            return false;
        }
        return this.types.equals(other.types);
    }

    @Override
    public Iterator<DnsRecordType> iterator() {
        return types.iterator();
    }
}
