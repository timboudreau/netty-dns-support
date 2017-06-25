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
import io.netty.codec.dns.protocol.types.Location;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.dns.DnsDecoderException;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;
import java.nio.charset.UnmappableCharacterException;

import static io.netty.handler.codec.dns.DnsResponseCode.BADNAME;
import static io.netty.handler.codec.dns.DnsResponseCode.BADVERS_OR_BADSIG;

final class LocationRecordCodec extends DnsRecordCodec<Location> {

    LocationRecordCodec() {
        super(Location.class);
    }

    @Override
    public Location read(ByteBuf from, NameCodec forReadingNames, int length) throws DnsDecoderException,
            UnmappableCharacterException, IOException {
        byte version = from.readByte();
        if (version != 0) {
            throw new DnsDecoderException(BADVERS_OR_BADSIG, "Unknown version for" + " LOC record "
                    + version);
        }
        byte size = from.readByte();
        byte horizontalPrecision = from.readByte();
        byte versionPrecision = from.readByte();
        long latitude = from.readUnsignedInt();
        long longitude = from.readUnsignedInt();
        long altitude = from.readUnsignedInt();
        check(size, true, "size");
        check(horizontalPrecision, true, "horizontalPrecision");
        check(versionPrecision, true, "verticalPrecision");
        Location result = new Location(size, version, horizontalPrecision, versionPrecision, latitude, longitude,
                altitude);
        return result;
    }

    private void check(byte b, boolean encoding, String name) throws DnsDecoderException {
        long out = b >> 4;
        int exp = b & 0xF;
        if (out > 9 || exp > 9) {
            String msg = "Invalid value for " + name + " - high and low 4 bits must be < 9 but were " + exp + " and "
                    + out + " from value " + b;
            if (encoding) {
                throw new DnsDecoderException(BADNAME, msg);
            } else {
                throw new CorruptedFrameException(msg);
            }
        }
    }

    @Override
    public void write(Location value, NameCodec names, ByteBuf into) throws IOException {
        check(value.rawSize(), true, "size");
        check(value.rawHorizontalPrecision(), true, "horizontalPrecision");
        check(value.rawVerticalPrecision(), true, "verticalPrecision");
        into.writeByte(value.version());
        into.writeByte(value.rawSize());
        into.writeByte(value.rawHorizontalPrecision());
        into.writeByte(value.rawVerticalPrecision());
        into.writeInt((int) value.rawLatitude());
        into.writeInt((int) value.rawLongitude());
        into.writeInt((int) value.altitude());
    }

}
