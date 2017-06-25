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

import io.netty.codec.dns.protocol.optrecords.OptSubrecordType;

/**
 * Represents a single sub-record in an OPT record.
 */
public class OptSubrecord<T> {

    private final OptSubrecordType type;
    private final T content;

    public OptSubrecord(OptSubrecordType type, T content) {
        this.type = type;
        this.content = content;
    }

    public OptSubrecordType type() {
        return type;
    }

    public T content() {
        return content;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + type.hashCode();
        hash = 67 * hash + content.hashCode();
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
        final OptSubrecord<?> other = (OptSubrecord<?>) obj;
        if (!type.equals(other.type)) {
            return false;
        }
        return this.content.equals(other.content);
    }

    @Override
    public String toString() {
        return "OptRecord{" + "type=" + type + ", content=" + content + '}';
    }
}
