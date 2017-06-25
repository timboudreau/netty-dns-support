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

import io.netty.util.AsciiString;
import io.netty.util.internal.ObjectUtil;

import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.ObjectUtil.checkPositive;

/**
 * Opt subrecord types.  OPT Pseudo records can embed a collection of different sub-records, each of which has their
 * own payload type;  this allows us to register decoders for those.
 */
public final class OptSubrecordType {

    public static final OptSubrecordType ECS = new OptSubrecordType(new AsciiString("ECS"), 8);
    public static final OptSubrecordType COOKIE = new OptSubrecordType(new AsciiString("COOKIE"), 10);

    private final AsciiString name;

    private final int intValue;

    public OptSubrecordType(CharSequence name, int intValue) {
        this.name = AsciiString.of(checkNotNull(name, "name"));
        this.intValue = checkPositive(intValue, "intValue");
    }

    public static OptSubrecordType valueOf(int value) {
        switch (value) {
            case 8:
                return ECS;
            case 10:
                return COOKIE;
            default:
                return new OptSubrecordType(Integer.toString(value), value);
        }
    }

    public static OptSubrecordType[] values() {
        return new OptSubrecordType[]{ECS};
    }

    public static OptSubrecordType valueOf(CharSequence name) {
        for (OptSubrecordType t : values()) {
            if (t.name.contentEquals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown opt subrecord type '"
                + name + "'");
    }

    public AsciiString name() {
        return name;
    }

    public int intValue() {
        return intValue;
    }

    public boolean isUnknown() {
        return name.contentEquals(Integer.toString(intValue));
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
