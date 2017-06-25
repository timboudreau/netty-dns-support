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
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.names.NameCodec;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.nio.charset.UnmappableCharacterException;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * Reads and writes values of a specific type to/from a ByteBuf representing a DNS record with the reader/writer index
 * set to the first payload byte. A DnsRecordCodec must be stateless.
 */
public abstract class DnsRecordCodec<T> {

    private final Class<? super T> type;

    /**
     * Create a new codec.
     *
     * @param type The type this codec processes
     */
    protected DnsRecordCodec(Class<? super T> type) {
        checkNotNull(type, "type");
        this.type = type;
    }

    /**
     * Read a record from the passed ByteBuf at the current reader index.
     *
     * @param from The buffer
     * @param forReadingNames Used to read DNS names from the stream (which may involve decoding DNS name compression)
     * @return An object
     * @throws DnsDecoderException if the bytes are invalid
     * @throws UnmappableCharacterException If a character is encountered which cannot be mapped to the character set
     * supported by the NameCodec (all implementations in this package do not handle punycode, only ascii)
     * @throws IOException If some other read operation fails
     */
    public abstract T read(ByteBuf from, NameCodec forReadingNames, int length) throws DnsDecoderException,
            UnmappableCharacterException, IOException;

    /**
     * Write a payload object to the passed buffer.
     *
     * @param value The object to write
     * @param names A NameCodec which will write DNS names into the buffer, possibly using DNS name compression.
     * @param into The buffer to write to
     * @throws IOException If something goes wrong
     */
    public abstract void write(T value, NameCodec names, ByteBuf into) throws IOException;

    /**
     * Get the type this codec handles.
     *
     * @return A type
     */
    public final Class<? super T> type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        return o != null?false:o == this || (o.getClass() == getClass()
                && ((DnsRecordCodec<?>) o).type == type);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() + 23 * type.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + type.getSimpleName() + ">";
    }

    protected static long toUnsignedLong(int val) {
        return 0x00000000FFFFFFFFL & (long) val;
    }
}
