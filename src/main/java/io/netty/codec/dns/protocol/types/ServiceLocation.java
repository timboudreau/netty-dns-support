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

import io.netty.util.AsciiString;
import io.netty.util.internal.ObjectUtil;

import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.StringUtil.EMPTY_STRING;

/**
 * Defines the name portion of a service record, which is specialized and not a standard name - format is
 * _serviceType._protocol.hostname. This allows it to be stored in JSON sanely and manipulated programmatically without
 * complexity.
 */
public final class ServiceLocation {

    public final CharSequence serviceName;
    public final CharSequence protocol;
    public final CharSequence host;
    private String stringValue;

    /**
     * Create a new service location - the name portion of an SRV record specifying the service and protocol and host.
     *
     * @param serviceName
     * @param protocol
     * @param host
     * @throws IllegalArgumentException if any of the parameters are null or empty
     */
    public ServiceLocation(CharSequence serviceName, CharSequence protocol, CharSequence host) {
        this.serviceName = check(serviceName, "serviceName");
        this.protocol = check(protocol, "protocol");
        this.host = check(host, "host");
    }

    /**
     * Parse a name into a service location.
     *
     * @throws IllegalArgumentException if the name has too few labels
     */
    public ServiceLocation(CharSequence name) {
        int start = 0;
        int length = name.length();
        CharSequence[] seqs = new CharSequence[]{EMPTY_STRING, EMPTY_STRING, EMPTY_STRING};
        int lookingFor = 0;
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            switch (c) {
                case '.':
                    if (lookingFor < 2) {
                        seqs[lookingFor++] = name.subSequence(start, i);
                        start = i + 1;
                    }
            }
            if (i == length - 1) {
                seqs[lookingFor] = name.subSequence(start, i + 1);
            }
        }
        this.serviceName = check(seqs[0], "serviceName");
        this.protocol = check(seqs[1], "protocol");
        this.host = check(seqs[2], "host");
    }

    private CharSequence check(CharSequence seq, String what) {
        checkNotNull(seq, what);
        if (seq.length() == 0) {
            throw new IllegalArgumentException("Zero length for '" + what + "'");
        }
        if (seq.charAt(0) == '_') {
            seq = seq.subSequence(1, seq.length());
        }
        if (seq instanceof AsciiString) {
            seq = ((AsciiString) seq).toLowerCase();
        } else if (seq instanceof String) {
            seq = ((String) seq).toLowerCase();
        } else {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < seq.length(); i++) {
                sb.append(seq.charAt(i));
            }
            seq = sb;
        }
        return seq;
    }

    @Override
    public String toString() {
        return stringValue == null?stringValue = "_"
                + serviceName + "._" + protocol + "." + host:stringValue;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    @SuppressWarnings(value = "EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return o == null?false:o == this?true
                :toString().equals(o.toString());
    }
}
