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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.codec.dns.protocol.DnsRecordCodec;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.names.NameCodec;
import io.netty.util.AsciiString;
import java.io.IOException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.List;

import static io.netty.util.CharsetUtil.US_ASCII;

final class TextRecordCodec extends DnsRecordCodec<CharSequence[]> {

    private static final CharsetEncoder ASCII_ENCODER = US_ASCII.newEncoder();

    TextRecordCodec() {
        super(CharSequence[].class);
    }

    private byte[] bytes(ByteBuf buf) {
        byte[] b = new byte[buf.readableBytes()];
        int oldPos = buf.readerIndex();
        buf.readBytes(b);
        buf.readerIndex(oldPos);
        return b;
    }

    @Override
    public CharSequence[] read(ByteBuf from, NameCodec forReadingNames, int dataLength)
            throws DnsDecoderException, UnmappableCharacterException, IOException {
        // Read the length of the txt record body
        short bytes = from.readUnsignedByte();
        // If we are going to underflow, get out now
        if (from.readableBytes() < bytes) {
            throw new CorruptedFrameException("Requested to read " + bytes
                    + " bytes from a buffer with only " + from.readableBytes() + " available.");
        }
        List<AsciiString> results = new ArrayList<>(2);
        // State variables
        boolean inQuotes = false;
        boolean precedingBackslash = false;
        boolean inElement = false;

        ByteBuf currString = from.alloc().buffer(bytes, 255);
        boolean lastWasQuoteOrWhitespace = true;
        for (int i = 0; i < bytes; i++) {
            // Read one character as ASCII
            byte currChar = from.readByte();
            boolean isQuote = currChar == '"' && !precedingBackslash;
            boolean isWhitespace = !isQuote && Character.isWhitespace(currChar);
            if (i == 0 && !isWhitespace && !isQuote) {
                //First character is a letter - open element now
                inElement = true;
            }
            if (precedingBackslash && currChar == '"') {
                // Back up to remove the \ from the string
                currString.writerIndex(currString.writerIndex() - 1);
            }
            // Are we at a boundary between string elements?
            boolean boundary = isQuote || (!inQuotes && isWhitespace) || (lastWasQuoteOrWhitespace && !inElement
                    && !isQuote && !isWhitespace);
            boolean enteredElement = false;
            if (boundary && inElement) {
                // End of element - store it and create a new buffer
                int length = currString.readableBytes();
                results.add(new AsciiString(bytes(currString), 0, length, false));
                currString = Unpooled.buffer(bytes - length, 255);
                inQuotes = false;
                inElement = false;
            } else if (boundary && !inElement) {
                // Start of an element
                if (!isQuote && !isWhitespace) {
                    // We are on the first letter of it
                    currString.writeByte(currChar);
                }
                enteredElement = true;
                inElement = true;
            } else if (!inElement && isWhitespace) {
                // More than one whitespace char between non-quoted items
                //do nothing
            } else if (inElement && !boundary) {
                // Not on a boundary - just add the character
                currString.writeByte(currChar);
            }
            // Update state if we've just started an element
            if (enteredElement) {
                inQuotes = isQuote;
            }
            // Update state if we may need to delete a quote-escape
            precedingBackslash = currChar == '\\';
            if (i == bytes - 1 && currString.readableBytes() > 0) {
                results.add(new AsciiString(bytes(currString), 0, currString.readableBytes(), false));
            }
            // Track whether this character was one included in the results or not
            lastWasQuoteOrWhitespace = isWhitespace || isQuote;
        }
        return results.toArray(new AsciiString[results.size()]);
    }

    @Override
    public void write(CharSequence[] value, NameCodec names, ByteBuf into) throws TooLongFrameException, IOException {
        // Store the position of where we'll writePseudoRecord the byte count
        int lengthOffset = into.writerIndex();
        // Write a dummy 0 value for now - we'll come back to it once we have a total
        into.writeByte(0);
        int cumulativeLength = 0;
        // Iterate over the strings
        for (int j = 0; j < value.length; j++) {
            CharSequence seq = value[j];
            int len = seq.length();
            boolean canEncodeRaw = true;
            // Scan for whether we can copy the data as-is or whether it needs quoting or
            // escaping, and check for illegal characters
            for (int i = 0; i < len; i++) {
                char c = seq.charAt(i);
                if (!ASCII_ENCODER.canEncode(c)) {
                    throw new IOException("Character '" + c + " at " + i + " in '" + seq
                            + "' cannot be encoded in ASCII");
                }
                if (c == '"' || Character.isWhitespace(c)) {
                    canEncodeRaw = false;
                }
            }
            if (canEncodeRaw) {
                // If the number of bytes will exceed 255, get out - we can't record a length
                // field longer than 255 in one byte
                if (cumulativeLength + seq.length() > 255) {
                    throw new TooLongFrameException("Maximum TXT record length reached - appending '" + seq
                            + "' + would result in " + (cumulativeLength + seq.length()
                            + " bytes, which would make the length field wrap around zero. Max is 255, "
                            + "including any escapes of quotes and delimiters"));
                }
                // Raw copy the data
                cumulativeLength = ByteBufUtil.writeAscii(into, seq);
                // Add a delimiter
                if (j != value.length - 1) {
                    into.writeByte(' ');
                    cumulativeLength++;
                }
            } else {
                // Add quotes and escape any internal quotes
                String s = '"' + seq.toString().replace("\"", "\\\"") + '"';
                if (s.length() + cumulativeLength > 255) {
                    throw new TooLongFrameException("Maximum TXT record length reached - appending '" + s
                            + "' + would result in " + (cumulativeLength + s.length()
                            + " bytes, which would make the length field wrap around zero. Max is 255, "
                            + "including any escapes of quotes and delimiters"));
                }
                cumulativeLength += ByteBufUtil.writeAscii(into, s);
            }
        }
        int endOffset = into.writerIndex();
        int bytesWritten = endOffset - (lengthOffset + 1);
        into.writerIndex(lengthOffset);
        into.writeByte(bytesWritten);
        into.writerIndex(endOffset);
    }

}
