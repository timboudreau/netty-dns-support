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
package io.netty.codec.dns.protocol.optrecords;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.codec.dns.protocol.DnsRecordCodec;
import io.netty.codec.dns.protocol.types.ClientSubnet;
import io.netty.codec.dns.protocol.types.Ipv4Address;
import io.netty.codec.dns.protocol.types.Ipv6Address;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;
import java.nio.charset.UnmappableCharacterException;

import static io.netty.handler.codec.dns.DnsResponseCode.FORMERR;

final class ClientSubnetCodec extends DnsRecordCodec<ClientSubnet<?>> {

    private static final int PREFIX_MASK = Byte.SIZE - 1;

    ClientSubnetCodec() {
        super(ClientSubnet.class);
    }

    @Override
    public ClientSubnet<?> read(ByteBuf from, NameCodec forReadingNames, int length) throws DnsDecoderException,
            UnmappableCharacterException, IOException {
        int addressKind = from.readShort();
        int sourcePrefixLength = from.readUnsignedByte();
        int scopePrefixLength = from.readUnsignedByte();

        int lowOrderBitsToPreserve = sourcePrefixLength % Byte.SIZE;
        // http://www.iana.org/assignments/address-family-numbers/address-family-numbers.xhtml
        switch (addressKind) {
            case 1: // ipv4
                final byte[] ip4bytes = new byte[4];
                from.readBytes(ip4bytes);
                applyPrefixMask(ip4bytes, lowOrderBitsToPreserve);
                return ClientSubnet.ipv4(new Ipv4Address(ip4bytes), sourcePrefixLength, scopePrefixLength);
            case 2: // ipv6
                byte[] ip6bytes = new byte[16];
                from.readBytes(ip6bytes);
                applyPrefixMask(ip6bytes, lowOrderBitsToPreserve);
                return ClientSubnet.ipv6(new Ipv6Address(ip6bytes), sourcePrefixLength, scopePrefixLength);
            default:
                throw new DnsDecoderException(FORMERR, "Unknown address type "
                        + Integer.toHexString(addressKind));
        }
    }

    private void applyPrefixMask(byte[] addressBytes, int preserveBits) {
        if (preserveBits > 0) {
            addressBytes[addressBytes.length - 1] = padWithZeros(
                    addressBytes[addressBytes.length - 1], preserveBits);
        }
    }

    @Override
    public void write(ClientSubnet<?> value, NameCodec names, ByteBuf into) throws IOException {
        //        encodeRecord0(nameCodec, record, out);
        int sourcePrefixLength = value.sourcePrefixLength();
        int scopePrefixLength = value.scopePrefixLength();
        int lowOrderBitsToPreserve = sourcePrefixLength & PREFIX_MASK;
        byte[] bytes = value.addressAsBytes();
        int addressBits = bytes.length << 3;
        if (addressBits < sourcePrefixLength || sourcePrefixLength < 0) {
            throw new IllegalArgumentException(sourcePrefixLength + ": "
                    + sourcePrefixLength + " (expected: 0 >= " + addressBits + ')');
        }
        // See http://www.iana.org/assignments/address-family-numbers/address-family-numbers.xhtml
        final short addressNumber = (short) (bytes.length == 4
                ?InternetProtocolFamily.IPv4.addressNumber():InternetProtocolFamily.IPv6.addressNumber());
        int payloadLength = sourcePrefixLength == 0?bytes.length:calculateEcsAddressLength(sourcePrefixLength,
                lowOrderBitsToPreserve);
        into.writeShort(addressNumber);
        into.writeByte(sourcePrefixLength);
        into.writeByte(scopePrefixLength); // Must be 0 in queries.

        if (lowOrderBitsToPreserve > 0) {
            int bytesLength = payloadLength - 1;
            into.writeBytes(bytes, 0, bytesLength);
            // Pad the leftover of the last byte with zeros.
            byte padded = padWithZeros(bytes[bytesLength], lowOrderBitsToPreserve);
            into.writeByte(padded);
        } else {
            // The sourcePrefixLength align with Byte so just copy in the bytes directly.
            into.writeBytes(bytes, 0, payloadLength);
        }
    }

    static int calculateEcsAddressLength(int sourcePrefixLength, int lowOrderBitsToPreserve) {
        return (sourcePrefixLength >>> 3) + (lowOrderBitsToPreserve != 0?1:0);
    }

    static byte padWithZeros(byte b, int lowOrderBitsToPreserve) {
        switch (lowOrderBitsToPreserve) {
            case 0:
                return 0;
            case 1:
                return (byte) (0x01 & b);
            case 2:
                return (byte) (0x03 & b);
            case 3:
                return (byte) (0x07 & b);
            case 4:
                return (byte) (0x0F & b);
            case 5:
                return (byte) (0x1F & b);
            case 6:
                return (byte) (0x3F & b);
            case 7:
                return (byte) (0x7F & b);
            case 8:
                return b;
            default:
                throw new IllegalArgumentException("lowOrderBitsToPreserve: " + lowOrderBitsToPreserve);
        }
    }
}
