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

import java.net.InetAddress;

import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * Used in OPT EDNS Client subnet records.
 *
 * @param <T> The address type
 */
public final class ClientSubnet<T> {

    private final int sourcePrefixLength;
    private final int scopePrefixLength;
    private final T address;

    private ClientSubnet(int sourcePrefixLength, int scopePrefixLength, T address) {
        this.sourcePrefixLength = checkPositiveOrZero(sourcePrefixLength, "sourcePrefixLength");
        this.scopePrefixLength = checkPositiveOrZero(scopePrefixLength, "scopePrefixLength");
        this.address = checkNotNull(address, "address");
    }

    public static ClientSubnet<Ipv4Address> ipv4(Ipv4Address addr, int sourcePrefixLength, int scopePrefixLength) {
        return new ClientSubnet<>(sourcePrefixLength, scopePrefixLength, addr);
    }

    public static ClientSubnet<Ipv6Address> ipv6(Ipv6Address addr, int sourcePrefixLength, int scopePrefixLength) {
        return new ClientSubnet<>(sourcePrefixLength, scopePrefixLength, addr);
    }

    public static ClientSubnet<Ipv4Address> ipv4forQuery(Ipv4Address addr, int sourcePrefixLength) {
        return new ClientSubnet<>(sourcePrefixLength, 0, addr);
    }

    public static ClientSubnet<Ipv6Address> ipv6forQuery(Ipv6Address addr, int sourcePrefixLength) {
        return new ClientSubnet<>(sourcePrefixLength, 0, addr);
    }

    public int sourcePrefixLength() {
        return sourcePrefixLength;
    }

    public boolean isIpv4() {
        return address instanceof Ipv4Address;
    }

    /**
     * Returns the leftmost number of significant bits of ADDRESS that the response covers. In queries, it MUST be 0.
     */
    public int scopePrefixLength() {
        return scopePrefixLength;
    }

    /**
     * Returns the bytes of the {@link InetAddress} to use.
     */
    public T address() {
        return address;
    }

    public byte[] addressAsBytes() {
        if (address instanceof Ipv4Address) {
            Ipv4Address addr = (Ipv4Address) address();
            return addr.toByteArray();
        } else if (address instanceof Ipv6Address) {
            Ipv6Address addr = (Ipv6Address) address();
            return addr.toByteArray();
        } else {
            throw new IllegalStateException("Address is neither an ipv4 "
                    + "nor ipv6 address");
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + sourcePrefixLength;
        hash = 13 * hash + scopePrefixLength;
        hash = 13 * hash + address.hashCode();
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
        final ClientSubnet<?> other = (ClientSubnet<?>) obj;
        if (this.sourcePrefixLength != other.sourcePrefixLength) {
            return false;
        }
        if (this.scopePrefixLength != other.scopePrefixLength) {
            return false;
        }
        return this.address.equals(other.address);
    }

    @Override
    public String toString() {
        return address + "\t" + sourcePrefixLength + "\t" + scopePrefixLength;
    }
}
