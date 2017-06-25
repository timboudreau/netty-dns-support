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
package io.netty.codec.dns.protocol.recordcodecs;

import io.netty.buffer.ByteBuf;
import io.netty.codec.dns.protocol.DnsRecordCodec;
import io.netty.codec.dns.protocol.types.NextSecureRecord;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.names.NameCodec;
import io.netty.handler.codec.dns.names.NameCodecFeature;
import java.io.IOException;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.netty.handler.codec.dns.DnsResponseCode.FORMERR;
import static io.netty.handler.codec.dns.names.NameCodecFeature.MDNS_UTF_8;
import static io.netty.handler.codec.dns.names.NameCodecFeature.WRITE_TRAILING_DOT;

final class NSECRecordCodec extends DnsRecordCodec<NextSecureRecord> {

    NSECRecordCodec() {
        super(NextSecureRecord.class);
    }

    @Override
    public NextSecureRecord read(ByteBuf from, NameCodec forReadingNames, int length)
            throws DnsDecoderException, UnmappableCharacterException, IOException {
        int start = from.readerIndex();
        CharSequence name = forReadingNames.readName(from);
        List<DnsRecordType> types = new ArrayList<>();
        length -= from.readerIndex() - start;
        int end = from.readerIndex() + length - 1;
        while (from.readerIndex() < end) {
            // The number of the 256-bit window, corresponding to 256
            // possible record types
            int window = from.readUnsignedByte();
            // The length of this window - it will go up to the last
            // byte that needs to be non-zero
            int windowLength = from.readUnsignedByte();
            // Do some sanity checks
            if (windowLength > from.readableBytes()) {
                throw new DnsDecoderException(FORMERR, "NSEC bitmap window is "
                        + windowLength + " bytes long but only "
                        + from.readableBytes() + " bytes remain.");
            } else if (windowLength < 1) {
                throw new DnsDecoderException(FORMERR, "NSEC bitmap window is "
                        + windowLength
                        + " bytes long but must be 1 or greater.");
            } else if (windowLength > 32) {
                throw new DnsDecoderException(FORMERR, "NSEC bitmap window is "
                        + windowLength + " but may not be > 32.");
            }
            // Iterate the bytes
            for (int i = 0; i < windowLength; i++) {
                // Bit order is reversed in the bitmap
                final int curr = reverse(from.readUnsignedByte());
                // If nothing here, skip
                if (curr == 0) {
                    continue;
                }
                // There is no RR type 0, so bit 0 must always be false
                if (window == 0 && i == 0 && (curr & 1) != 0) {
                    throw new DnsDecoderException(FORMERR, "Zeroth bit in "
                            + "zeroth frame of NSEC bitmask MUST be false but "
                            + "first byte is " + toBinaryString(curr));
                }
                // Compute what bit we looking at
                int bitOffsetAtStartOfByte = (window * 256) + (i * 8);
                for (int bit = 0; bit < 8; bit++) {
                    // Find the bits that are set, and look up the
                    // corrsponding type
                    int test = 1 << bit;
                    if ((test & curr) != 0) {
                        int typeIndex = bitOffsetAtStartOfByte + bit;
                        DnsRecordType type = DnsRecordType.valueOf(typeIndex);
                        types.add(type);
                    }
                }
            }
        }
        return new NextSecureRecord(name, types);
    }

    private static CharSequence toBinaryString(int val) {
        StringBuilder sb = new StringBuilder(Integer.toBinaryString(val));
        if (sb.length() < 8) {
            char[] zeros = new char[8 - sb.length()];
            Arrays.fill(zeros, '0');
            sb.insert(0, zeros);
        }
        return sb;
    }

    @Override
    public void write(NextSecureRecord value, NameCodec names, ByteBuf into) throws IOException {
        // NSEC records MUST not use name compression, so ...
        NameCodec actualNames = NameCodecFeature.MDNS_UTF_8.isImplementedBy(names)
                ? NameCodec.get(MDNS_UTF_8, WRITE_TRAILING_DOT)
                : names.supportsUnicode() ? NameCodec.nonCompressingNameCodec().toPunycodeNameCodec() 
                : NameCodec.nonCompressingNameCodec();
        actualNames.writeName(value.nextRecord, into);
        int lastBlock = -1;
        int lengthOffset = -1;
        List<DnsRecordType> types = value.types;
        int size = types.size();
        for (int i = 0; i < size; i++) {
            // Get the current type - it's intValue() is the offset of the
            // bit we need to set
            DnsRecordType type = types.get(i);
            // See https://tools.ietf.org/html/rfc2929#section-3.1
            // No bits for Meta-TYPEs and QTYPEs
            if (type.isMetaTypeOrQType()) {
                continue;
            }
            int currentBit;
            int bitToSet = type.intValue();
            int currentWindow = bitToSet / 256;
            // If the window has changed (bit to set is > 256 bits away
            // from the start of the current window, we need to start a new
            // window
            if (currentWindow != lastBlock && lastBlock != -1) {
                // If we are not on the first block, close the previous one
                // by going back and filling in its length now that we know it
                if (lengthOffset != -1) {
                    // Store the spot to move the cursor back to
                    int newBlockStart = into.writerIndex();
                    into.writerIndex(lengthOffset);
                    int prevBlockLength = newBlockStart - (lengthOffset + 1);
                    // Write the old length
                    into.writeByte(prevBlockLength);
                    into.writerIndex(newBlockStart);
                }
                // Update the block start, the pointer to where to write the
                // length of this block, and the value of lastBlock to test
                // for block changes on subsequent iterations
                lastBlock = currentWindow;
                // Write the integer number of the current window (the packet
                // will not contain all possible windows - empty ones are skipped)
                into.writeByte(currentWindow);
                lengthOffset = into.writerIndex();
                into.writeByte(0); // placeholder we will overwrite later
            } else if (lastBlock == -1) {
                // First time through, do some initialization
                lastBlock = 0;
                into.writeByte(currentWindow);
                lengthOffset = into.writerIndex();
                into.writeByte(0); // placeholder
            }
            // Get the bit offset of the current byte we're writing
            currentBit = (currentWindow * 256) + (((into.writerIndex() - (lengthOffset + 1)) * 8));
            // If we're more than 8 bits away from what we need to set,
            // fill with zeros until we are on the byte we need to write
            // a bit into
            while (bitToSet - currentBit > 8) {
                into.writeByte(0);
                currentBit = (currentWindow * 256) + (((into.writerIndex() - (lengthOffset + 1)) * 8));
            }
            // Compute the byte we'll write
            int bitOffset = bitToSet - currentBit;
            int byteToWrite = 1 << bitOffset;
            for (int j = i + 1; j < size; j++) {
                // Scan ahead in the types list and see if subsequent ones
                // also need to be written into this byte, and if so, OR
                // those values in
                type = types.get(j);
                if (type.intValue() - bitToSet < 8 - bitOffset) {
                    bitOffset = type.intValue() - currentBit;
                    byteToWrite |= 1 << bitOffset;
                    i++;
                } else {
                    break;
                }
            }
            // Though the spec doesn't spell this out, the bitmap is written
            // in reverse bit-order - bit 8 of the first byte is bit 0 of the
            // bitmask, bit 1 is bit 7, and so forth.  That does result in a pretty, linear
            // bitmask if you print all the bits out, which nobody is likely
            // to do unless they're trying to ... figure out why their NSEC
            // implementation *almost* works.
            into.writeByte(reverse(byteToWrite));
        }
        // Close the last entry
        if (lengthOffset != -1) {
            int end = into.writerIndex();
            into.writerIndex(lengthOffset);
            int prevBlockLength = end - (lengthOffset + 1);
            into.writeByte(prevBlockLength);
            into.writerIndex(end);
        }
    }

    private static byte reverse(int val) {
        // Reverse the bit-order of a byte
        int value = Integer.reverse(val);
        return (byte) (value >> 24);
    }
}
