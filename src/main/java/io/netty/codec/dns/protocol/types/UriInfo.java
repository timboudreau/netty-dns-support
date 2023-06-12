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

/**
 * Payload for a URI type DNS record.
 */
public final class UriInfo {

    public final int weight;
    public final int priority;
    public final CharSequence uri;

    public UriInfo(int weight, int priority, CharSequence uri) {
        this.weight = checkPositiveOrZero(weight, "weight");
        this.priority = checkPositiveOrZero(priority, "priority");
        this.uri = checkNotNull(uri, "uri");
        if (uri.length() == 0) {
            throw new IllegalArgumentException("URI may not be empty");
        }
        if (uri.length() > 255) {
            throw new IllegalArgumentException("URI must be <= 255 characters");
        }
        if (weight > 65535) {
            throw new IllegalArgumentException("Weight must be a positive integer 0-65535 but got " + weight);
        }
        if (priority > 65535) {
            throw new IllegalArgumentException("Priority must be a positive integer 0-65535 but got " + priority);
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash += 73 * weight;
        hash += 73 * priority;
        hash += 73 * uri.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UriInfo
                && ((UriInfo) o).weight == weight
                && ((UriInfo) o).priority == priority
                && StringUtil.charSequencesEqual(((UriInfo) o).uri, uri, false);
    }

    @Override
    public String toString() {
        return priority + "\t" + weight + "\t" + uri;
    }
}
