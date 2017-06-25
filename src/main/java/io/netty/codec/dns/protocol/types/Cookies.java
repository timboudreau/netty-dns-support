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

import io.netty.util.internal.ObjectUtil;
import java.util.Arrays;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * Represents the payload of an OPT COOKIE subrecord.
 */
public final class Cookies {

    private final byte[] clientCookie;

    private final byte[] serverCookie;

    public Cookies(byte[] clientCookie) {
        this(clientCookie, null);
    }

    public Cookies(byte[] clientCookie, byte[] serverCookie) {
        checkNotNull(clientCookie, "clientCookie");
        if (clientCookie.length != 8) {
            throw new IllegalArgumentException("Client cookie length must be 8");
        }
        if (serverCookie != null) {
            if (serverCookie.length < 8) {
                throw new IllegalArgumentException("Server cookie length must "
                        + "be at least 8");
            }
            if (serverCookie.length > 32) {
                throw new IllegalArgumentException("Server cookie length must "
                        + "be no larger than 32");
            }
        }
        this.clientCookie = clientCookie;
        this.serverCookie = serverCookie;
    }

    public Cookies withServerCookie(byte[] serverCookie) {
        return new Cookies(copy(this.clientCookie), serverCookie);
    }

    public Cookies copy() {
        return new Cookies(clientCookie(), serverCookie());
    }

    public boolean hasServerCookie() {
        return serverCookie != null;
    }

    public byte[] clientCookie() {
        return copy(clientCookie);
    }

    public byte[] serverCookie() {
        return copy(serverCookie);
    }

    private byte[] copy(byte[] b) {
        if (b == null) {
            return null;
        }
        byte[] nue = new byte[b.length];
        System.arraycopy(b, 0, nue, 0, b.length);
        return nue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Cookies)) {
            return false;
        }
        final Cookies other = (Cookies) obj;
        if ((this.clientCookie == null) != (other.clientCookie == null)) {
            return false;
        }
        if (!Arrays.equals(this.clientCookie, other.clientCookie)) {
            return false;
        }
        return Arrays.equals(this.serverCookie, other.serverCookie);
    }

    @Override
    public int hashCode() {
        int mult = serverCookie == null?571:937;
        int result = 0;
        for (byte b : clientCookie) {
            result += mult * b;
        }
        if (serverCookie != null) {
            for (byte b : serverCookie) {
                result += mult * b;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : clientCookie) {
            String s = Integer.toHexString(b & 0xFF);
            if (s.length() == 1) {
                sb.append('0');
            }
            System.out.println((b & 0xFF) + " -> " + s);
            sb.append(s);
        }
        if (serverCookie != null) {
            for (byte b : serverCookie) {
                String s = Integer.toHexString(b & 0xFF);
                if (s.length() == 1) {
                    sb.append('0');
                }
                sb.append(s);
            }
        }
        return sb.toString();
    }
}
