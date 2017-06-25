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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * Provides a way to query a list of OPT records by subtype or payload type.
 */
public final class OptRecords implements Iterable<OptSubrecord<?>> {

    private final List<OptSubrecord<?>> records;

    public OptRecords(List<OptSubrecord<?>> records) {
        checkNotNull(records, "records");
        this.records = new ArrayList<>(records);
    }

    public OptRecords() {
        this.records = new ArrayList<>(1);
    }

    public static OptRecords of(OptSubrecord<?> single, OptSubrecord<?>... more) {
        checkNotNull(single, "single");
        checkNotNull(more, "more");
        if (more.length == 0) {
            return new OptRecords(Arrays.asList(single));
        } else {
            List<OptSubrecord<?>> l = new ArrayList<>();
            l.add(single);
            l.addAll(Arrays.asList(more));
            return new OptRecords(l);
        }
    }

    public OptRecords add(OptSubrecord<?> record) {
        records.add(record);
        return this;
    }

    public List<OptSubrecord<?>> asList() { // for json serialization
        return Collections.unmodifiableList(records);
    }

    @SuppressWarnings("unchecked")
    public <T> OptSubrecord<T> find(Class<T> payloadType) {
        for (OptSubrecord<?> r : this) {
            if (payloadType.isInstance(r.content())) {
                return (OptSubrecord<T>) r;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> List<OptSubrecord<T>> findAll(Class<T> payloadType) {
        List<OptSubrecord<T>> result = new ArrayList<>();
        for (OptSubrecord<?> r : this) {
            if (payloadType.isInstance(r.content())) {
                result.add((OptSubrecord<T>) r);
            }
        }
        return result;
    }

    public OptSubrecord<?> find(OptSubrecordType type) {
        for (OptSubrecord<?> r : this) {
            if (type.equals(r.type())) {
                return r;
            }
        }
        return null;
    }

    public List<OptSubrecord<?>> findAll(OptSubrecordType type) {
        List<OptSubrecord<?>> result = new ArrayList<>();
        for (OptSubrecord<?> r : this) {
            if (type.equals(r.type())) {
                result.add(r);
            }
        }
        return result;
    }

    public int size() {
        return records.size();
    }

    @Override
    public Iterator<OptSubrecord<?>> iterator() {
        return records.iterator();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + records.hashCode();
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
        final OptRecords other = (OptRecords) obj;
        return this.records.equals(other.records);
    }

    @Override
    public String toString() {
        return "OptRecords{" + "records=" + records + '}';
    }
}
