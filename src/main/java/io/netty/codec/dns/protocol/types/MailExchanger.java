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

import static io.netty.util.internal.StringUtil.charSequenceHashCode;
import static io.netty.util.internal.StringUtil.charSequencesEqual;

/**
 * Represents the payload of a DNS MX record.
 */
public class MailExchanger implements Comparable<MailExchanger> {

    public final int pref;
    public final CharSequence mx;

    public MailExchanger(int pref, CharSequence mx) {
        this.pref = pref;
        this.mx = mx;
    }

    public int pref() {
        return pref;
    }

    public CharSequence mx() {
        return mx;
    }

    @Override
    public String toString() {
        return mx + " (" + pref + ")";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + this.pref;
        hash = 61 * hash + charSequenceHashCode(this.mx, true);
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
        final MailExchanger other = (MailExchanger) obj;
        return pref == other.pref && charSequencesEqual(mx, other.mx, true);
    }

    @Override
    public int compareTo(MailExchanger o) {
        return o.pref > pref?1:o.pref < pref?-1:0;
    }
}
