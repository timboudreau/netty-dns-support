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

import io.netty.util.internal.StringUtil;

import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;
import static io.netty.util.internal.StringUtil.charSequencesEqual;

/**
 * The payload portion of an SRV record.
 */
public class ServiceDetails {

    public final int priority;
    public final int weight;
    public final int port;
    public final CharSequence name;

    public ServiceDetails(int priority, int weight, int port, CharSequence name) {
        this.priority = checkPositiveOrZero(priority, "priority");
        this.weight = checkPositiveOrZero(weight, "weight");
        this.port = checkPositiveOrZero(port, "port"); // mDNS has zero ports
        this.name = checkNotNull(name, "name");
    }

    @Override
    public String toString() {
        return priority + "\t" + weight + "\t" + port + "\t" + name;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + this.priority;
        hash = 41 * hash + this.weight;
        hash = 41 * hash + this.port;
        hash = 41 * hash + this.name.hashCode();
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
        final ServiceDetails other = (ServiceDetails) obj;
        if (this.priority != other.priority) {
            return false;
        }
        if (this.weight != other.weight) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        return charSequencesEqual(this.name, other.name, true);
    }

}
