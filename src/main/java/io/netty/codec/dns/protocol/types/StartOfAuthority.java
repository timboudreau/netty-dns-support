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

import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.StringUtil.charSequenceHashCode;
import static io.netty.util.internal.StringUtil.charSequencesEqual;

/**
 * Represents a SOA record's payload.
 */
public final class StartOfAuthority {

    public final CharSequence primaryNs;
    public final CharSequence adminMailbox;
    public final long serialNumber;
    public final long refreshInterval;
    public final long retryInterval;
    public final long expirationLimit;
    public final long minimumTtl;

    public StartOfAuthority(CharSequence primaryNs, CharSequence adminMailbox,
            long serialNumber, long refreshInterval, long retryInterval,
            long expirationLimit, long minimumTtl) {
        this.primaryNs = checkNotNull(primaryNs, "primaryNs");
        this.adminMailbox = checkNotNull(adminMailbox, "adminMailbox");
        this.serialNumber = serialNumber;
        this.refreshInterval = refreshInterval;
        this.retryInterval = retryInterval;
        this.expirationLimit = expirationLimit;
        this.minimumTtl = minimumTtl;
    }

    /*
    // JDK 8
    public StartOfAuthority(CharSequence primaryNs, CharSequence adminMailbox,
            long serialNumber, Duration refreshInterval, Duration retryInterval,
            Duration expirationLimit, Duration minimumTtl) {
        this.primaryNs = ObjectUtil.checkNotNull(primaryNs, "primaryNs");
        this.adminMailbox = ObjectUtil.checkNotNull(adminMailbox, "adminMailbox");
        this.serialNumber = serialNumber;
        this.refreshInterval = ObjectUtil.checkNotNull(refreshInterval, "refreshInterval").getSeconds();
        this.retryInterval = ObjectUtil.checkNotNull(retryInterval, "retryInterval").getSeconds();
        this.expirationLimit = ObjectUtil.checkNotNull(expirationLimit, "expirationLimit").getSeconds();
        this.minimumTtl = ObjectUtil.checkNotNull(minimumTtl, "minimumTtl").getSeconds();
    }

    public Duration refreshInterval() {
        return Duration.ofSeconds(refreshInterval);
    }

    public Duration retryInterval() {
        return Duration.ofSeconds(retryInterval);
    }

    public Duration expirationLimit() {
        return Duration.ofSeconds(expirationLimit);
    }

    public Duration minimumTimeToLive() {
        return Duration.ofSeconds(minimumTtl);
    }

    private String durationString(long seconds) {
        Duration dur = Duration.ofSeconds(seconds);
        return (dur.toHours() / 24) + "d "
                + (dur.toHours() % 24) + "h "
                + (dur.toMinutes() % 60) + "m "
                + (dur.getSeconds() % 60) + "s";
    }
     */
    private static String durationString(long totalSeconds) {
        long days = (totalSeconds / (60 * 60)) / 24;
        long hours = (totalSeconds / (60 * 60)) % 24;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return (days == 0?"":days + "d ")
                + (days == 0 && hours == 0?"":hours + "h ")
                + (days == 0 && hours == 0 && minutes == 0?"":minutes + "m ")
                + seconds + "s";
    }

    private StringBuilder durationLine(long seconds, String name, StringBuilder into) {
        return into.append(seconds).append("\t;\t").append(name).append(' ')
                .append(durationString(seconds)).append("\n\t\t\t");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(primaryNs).append(". ")
                .append(adminMailbox).append(".\t(\n\t\t\t")
                .append(serialNumber).append("\t; Serial\n\t\t\t");
        durationLine(refreshInterval, "Refresh", sb);
        durationLine(retryInterval, "Retry", sb);
        durationLine(expirationLimit, "Expire", sb);
        durationLine(minimumTtl, "Minimum TTL", sb);
        sb.append(")\n");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + charSequenceHashCode(primaryNs, true);
        hash = 13 * hash + charSequenceHashCode(adminMailbox, true);
        hash = 13 * hash + (int) (this.serialNumber ^ (this.serialNumber >>> 32));
        hash = 13 * hash + (int) (this.refreshInterval ^ (this.refreshInterval >>> 32));
        hash = 13 * hash + (int) (this.retryInterval ^ (this.retryInterval >>> 32));
        hash = 13 * hash + (int) (this.expirationLimit ^ (this.expirationLimit >>> 32));
        hash = 13 * hash + (int) (this.minimumTtl ^ (this.minimumTtl >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof StartOfAuthority)) {
            return false;
        }
        final StartOfAuthority other = (StartOfAuthority) obj;
        if (this.serialNumber != other.serialNumber) {
            return false;
        }
        if (this.refreshInterval != other.refreshInterval) {
            return false;
        }
        if (this.retryInterval != other.retryInterval) {
            return false;
        }
        if (this.expirationLimit != other.expirationLimit) {
            return false;
        }
        if (this.minimumTtl != other.minimumTtl) {
            return false;
        }
        if (!charSequencesEqual(this.primaryNs, other.primaryNs, true)) {
            return false;
        }
        return charSequencesEqual(this.adminMailbox, other.adminMailbox, true);
    }
}
