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

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.netty.util.collection.IntCollections.unmodifiableMap;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * Generic registry of DNS codecs mapped to some type - used for both DNS records and the internal contents of OPT
 * sub-records.
 */
public final class CodecRegistry<T> {

    private final IntObjectMap<DnsRecordCodec<?>> codecs;

    private final RegistryInternal<T> internal;

    CodecRegistry(IntObjectHashMap<DnsRecordCodec<?>> codecsMap, RegistryInternal<T> internal) {
        if (codecsMap == null) {
            throw new NullPointerException("codecsMap");
        }
        if (internal == null) {
            throw new NullPointerException("internal");
        }
        IntObjectHashMap<DnsRecordCodec<?>> temp = new IntObjectHashMap<>(codecsMap.size());
        temp.putAll(codecsMap);
        this.codecs = unmodifiableMap(temp);
        this.internal = internal;
    }

    /**
     * Internal plumbing for codec registry to work with different types.
     */
    public interface RegistryInternal<T> {

        /**
         * Get the integer value of a key.
         */
        int intValueFor(T type);

        /**
         * Get the fallback codec to use if none is available - for example, just reading raw bytes.
         */
        DnsRecordCodec<?> fallback();

        /**
         * Get a key from its name.
         */
        T fromString(String s);

        T valueOf(int val);
    }

    protected int intValueFor(T type) {
        return internal.intValueFor(type);
    }

    protected DnsRecordCodec<?> fallback() {
        return internal.fallback();
    }

    public List<T> supportedTypes() {
        List<T> result = new ArrayList<>();
        for (IntObjectMap.PrimitiveEntry<DnsRecordCodec<?>> e : codecs.entries()) {
            int key = e.key();
            result.add(internal.valueOf(key));
        }
        return result;
    }

    /**
     * Get the codec for a given record type.
     *
     * @param recordType The record type
     * @return The registered codec, or the fallback ByteBuf codec if none is available
     */
    public DnsRecordCodec<?> get(T recordType) {
        checkNotNull(recordType, "recordType");
        DnsRecordCodec<?> codec = codecs.get(intValueFor(recordType));
        if (codec == null) {
            codec = fallback();
        }
        return codec;
    }

    /**
     * Get the codec for a given record type which must match the passed payload's type, or an exception is thrown. This
     * method exists for the case that the type is known, to eliminate the need for an unchecked cast.
     *
     * @param <E> The payload type
     * @param recordType The record type
     * @param payload The payload
     * @return The matching codec
     */
    @SuppressWarnings("unchecked")
    public <E> DnsRecordCodec<E> get(T recordType, E payload) {
        checkNotNull(recordType, "recordType");
        checkNotNull(payload, "payload");
        DnsRecordCodec<?> codec = get(recordType);
        if (!codec.type().isInstance(payload)) {
            throw new IllegalArgumentException("The codec registered for "
                    + recordType + " uses "
                    + codec.type().getName() + " not " + payload.getClass().getName());
        }
        return (DnsRecordCodec<E>) codec;
    }

    /**
     * Get the codec for a given record type which must match the passed type, or an exception is thrown. This method
     * exists for the case that the type is known, to eliminate the need for an unchecked cast.
     *
     * @param <E> The payload type
     * @param recordType The record type
     * @param type The type
     * @return The matching codec
     * @throws IllegalArgumentException if the types do not match
     */
    @SuppressWarnings("unchecked")
    public <E> DnsRecordCodec<E> get(T recordType, Class<E> type) {
        DnsRecordCodec<?> codec = get(recordType);
        if (!type.isAssignableFrom(codec.type())) {
            throw new IllegalArgumentException("The codec registered for "
                    + recordType + " uses "
                    + codec.type().getName() + " not " + type.getName());
        }
        return (DnsRecordCodec<E>) codec;
    }

    /**
     * Create a new empty registry builder for assembling a registry. Standard codecs are available from static methods
     * on DnsRecordCodec.
     *
     * @return A registry builder
     */
    static <T> CodecRegistryBuilder<T> builder(RegistryInternal<T> internal) {
        return new CodecRegistryBuilder<>(internal);
    }

    /**
     * Get the registered codec for a given type, regardless of the associated DNS record type. If more than one codec
     * is registered for a given type (unlikely) then it is not defined which one is returned.
     *
     * @param <E> The type
     * @param type The type
     * @return A codec
     * @throws IllegalArgumentException if no codec is registered for this type
     */
    public <E> DnsRecordCodec<E> forType(Class<E> type) {
        Set<DnsRecordCodec<?>> all = new HashSet<>(codecs.values());
        for (DnsRecordCodec<?> codec : all) {
            if (type.equals(codec.type())) {
                return (DnsRecordCodec<E>) codec;
            }
        }
        throw new IllegalArgumentException("No codec registered for "
                + type.getSimpleName());
    }

    /**
     * Builder for a DnsRecordCodecRegistry.
     */
    public static final class CodecRegistryBuilder<T> {

        private final List<Entry<T, ?>> entries = new ArrayList<>(11);
        private final RegistryInternal<T> internal;

        private CodecRegistryBuilder(RegistryInternal<T> internal) {
            this.internal = internal;
        }

        /**
         * Add a codec to use for the passed set of types.
         */
        public <E> CodecRegistryBuilder<T> add(DnsRecordCodec<E> codec, T... types) {
            Set<T> actualTypes = new HashSet<>(Arrays.asList(types));
            return add(codec, actualTypes);
        }

        /**
         * Convenience method to register a DnsRecordCodec the names of one or more types.
         *
         * @param <T> The type the codec processes
         * @param codec The codec
         * @param types DnsRecordType names, such as "A", "AAAA", etc.
         * @return This builder
         * @throws IllegalArgumentException if DnsRecordType.valueOf(String) does not match any known record type, or if
         * the array of names is empty
         */
        public <E> CodecRegistryBuilder<T> add(DnsRecordCodec<E> codec, String... types) {
            Set<T> actualTypes = new HashSet<>();
            for (String type : types) {
                actualTypes.add(internal.fromString(type));
            }
            return add(codec, actualTypes);
        }

        /**
         * Register a codec to be used for specific DnsRecordTypes.
         *
         * @param <T> The type the codec processes
         * @param codec The codec
         * @param types The record types the codec should be used for
         * @return This builder
         */
        public <E> CodecRegistryBuilder<T> add(DnsRecordCodec<E> codec, Set<T> types) {
            checkNotNull(codec, "codec");
            if (types.isEmpty()) {
                throw new IllegalArgumentException("No DNS record types specified");
            }
            List<Entry<T, ?>> toRemove = new ArrayList<>();
            for (Entry<T, ?> entry : entries) {
                if (entry.codec.equals(codec)) {
                    entry.types.addAll(types);
                    return this;
                }
                for (T type : types) {
                    if (entry.types.contains(type)) {
                        entry.types.remove(type);
                    }
                }
                if (entry.types.isEmpty()) {
                    toRemove.add(entry);
                }
            }
            entries.removeAll(toRemove);
            entries.add(new Entry<>(codec, types));
            return this;
        }

        /**
         * Build a DnsCodecRegistry using the contents of this builder.
         *
         * @return A DnsCodecRegistry
         */
        public CodecRegistry<T> build() {
            IntObjectHashMap<DnsRecordCodec<?>> map = new IntObjectHashMap<>(20);
            for (Entry<T, ?> e : entries) {
                for (T type : e.types) {
                    map.put(internal.intValueFor(type), e.codec);
                }
            }
            return new CodecRegistry<>(map, internal);
        }
    }

    private static final class Entry<T, E> {

        private final Set<T> types = new HashSet<>(4);
        private final DnsRecordCodec<E> codec;

        Entry(DnsRecordCodec<E> codec, Set<T> types) {
            this.codec = codec;
            this.types.addAll(types);
        }
    }
}
