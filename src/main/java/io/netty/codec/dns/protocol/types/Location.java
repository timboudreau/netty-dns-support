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

import io.netty.codec.dns.protocol.types.Location.Coordinate.Direction;
import io.netty.codec.dns.protocol.types.Location.Coordinate.Direction.Orientation;
import io.netty.util.internal.ObjectUtil;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.codec.dns.protocol.types.Location.Coordinate.Direction.Orientation.LATITUDINAL;
import static io.netty.codec.dns.protocol.types.Location.Coordinate.Direction.Orientation.LONGITUDINAL;
import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * DNS location record.
 */
public final class Location {

    // Numbers above Integer.MAX_VALUE are North / East
    static final long PRIME_MERIDIAN_AND_EQUATOR = 1L << 31;
    // Altitudes are above this base
    static final long BASE_ALTITUDE = 10_000_000;
    // The default value for vertical precision if not specified
    static int DEFAULT_VERTICAL_PRECISION_CM = 10 * 100;
    // The default value for horizontal precision if not specified
    static int DEFAULT_HORIZONTAL_PRECISION_CM = 10000 * 100;
    // The default value for size if not specified
    static int DEFAULT_SIZE_CM = 1 * 100;

    private static final NumberFormat TWO_PLACES = new DecimalFormat();

    private final byte size; // most significant 4 bits is base,
    // least significant is a power of 10
    // to multiply the base by
    private final byte version;
    private final byte horizontalPrecision; // Same as size
    private final byte verticalPrecision; // Same as size
    private final int latitude; // thousandths of a second of arc
    private final int longitude; // thousandths of a second of arc
    private final int altitude;

    static {
        TWO_PLACES.setMinimumIntegerDigits(2);
    }
    /*
        From the RFC:
            ( d1 [m1 [s1]] {"N"|"S"} d2 [m2 [s2]]
                           {"E"|"W"} alt["m"] [siz["m"] [hp["m"]
                           [vp["m"]]]] )
     */
    private static final Pattern PARSE_PATTERN = Pattern.compile("(\\d+)(?: (\\d+))?(?: (\\d+(?:\\.\\d+)?))? (N|S) "
            + "(\\d+)(?: (\\d+))?(?: (\\d+(?:\\.\\d+)?))? (E|W)"
            + "(?: (-?\\d+(?:\\.\\d+)?)m?)"
            + "(?: (-?\\d+(?:\\.\\d+)?)m?)?(?: (-?\\d+(?:\\.\\d+)?)m?)?(?: (-?\\d+(?:\\.\\d+)?)m?)?", CASE_INSENSITIVE);

    public Location(byte size, byte version, byte horizontalPrecision, byte verticalPrecision, long latitude,
            long longitude, long altitude) {
        this.size = size;
        this.version = version;
        this.horizontalPrecision = horizontalPrecision;
        this.verticalPrecision = verticalPrecision;
        this.latitude = (int) latitude;
        this.longitude = (int) longitude;
        this.altitude = (int) altitude;
        convert(size);
    }

    /**
     * Convenience constructor for passing in computed coordinates.
     */
    public Location(double sizeMeters, double horizontalPrecisionMeters, double verticalPrecisionMeters,
            Coordinate latitude, Coordinate longitude, double altitudeMeters) {
        this(unconvertMeters(sizeMeters), unconvertMeters(horizontalPrecisionMeters),
                unconvertMeters(verticalPrecisionMeters), latitude, longitude,
                (long) ((altitudeMeters * 100D) + BASE_ALTITUDE));
    }

    public Location(int size, int horizontalPrecision, int verticalPrecision, Coordinate latitude, Coordinate longitude,
            long altitude) {
        this.version = 0; // There is only a version 0 of LOC record format, and if there were another this
        // class would not be compatible
        this.size = (byte) size;
        this.horizontalPrecision = (byte) horizontalPrecision;
        this.verticalPrecision = (byte) verticalPrecision;
        this.latitude = (int) latitude.toArcSecondThousandths();
        this.longitude = (int) longitude.toArcSecondThousandths();
        this.altitude = (int) altitude;
    }

    public static Location parse(String rec) {
        Matcher m = PARSE_PATTERN.matcher(rec);
        if (m.find()) {
            int latitudeDegrees = nullInt(m.group(1));
            int latitudeMinutes = nullInt(m.group(2));
            double latitudeSeconds = nullDouble(m.group(3));
            Direction latitudeDirection = Direction.match(m.group(4).charAt(0));

            Coordinate latitude = new Coordinate(latitudeDegrees, latitudeMinutes, latitudeSeconds, latitudeDirection);

            int longitudeDegrees = nullInt(m.group(5));
            int longitudeMinutes = nullInt(m.group(6));
            double longitudeSeconds = nullDouble(m.group(7));
            Direction longitudeDirection = Direction.match(m.group(8).charAt(0));

            Coordinate longitude = new Coordinate(longitudeDegrees, longitudeMinutes, longitudeSeconds,
                    longitudeDirection);

            double altitudeMeters = nullDouble(m.group(9));

            double sizeMeters = defaultDouble(m.group(10), DEFAULT_SIZE_CM / 100);
            double horizontalPrecisionMeters = defaultDouble(m.group(11), DEFAULT_HORIZONTAL_PRECISION_CM / 100);
            double verticalPrecisionMeters = defaultDouble(m.group(12), DEFAULT_VERTICAL_PRECISION_CM / 100);

            return new Location(sizeMeters, horizontalPrecisionMeters, verticalPrecisionMeters, latitude, longitude,
                    altitudeMeters);
        }
        return null;
    }

    private static int nullInt(String s) {
        return s == null?0:Integer.parseInt(s);
    }

    private static double nullDouble(String s) {
        return s == null?0D:Double.parseDouble(s);
    }

    private static double defaultDouble(String s, double def) {
        return s == null?def:Double.parseDouble(s);
    }

    public byte rawSize() {
        return size;
    }

    public byte rawHorizontalPrecision() {
        return horizontalPrecision;
    }

    public byte rawVerticalPrecision() {
        return verticalPrecision;
    }

    public long size() {
        return convert(size);
    }

    public byte version() {
        return version;
    }

    public int horizontalPrecision() {
        return (int) convert(horizontalPrecision);
    }

    public int verticalPrecision() {
        return (int) convert(verticalPrecision);
    }

    static byte unconvertMeters(double meters) {
        double centimeters = meters * 100;
        return unconvert((long) centimeters);
    }

    static byte unconvert(long f) {
        long val = f;
        byte powerOfTen = 0;
        while (val >= 10) {
            powerOfTen += 1;
            val /= 10;
        }
        byte result = (byte) ((val & 0xF) << 4 | powerOfTen);
        return result;
    }

    static long convert(byte b) {
        int val = b & 0xff;
        int powerOfTen = val & 0xF;
        int base = (val >> 4) & 0xF;
        long result = (long) (base * Math.pow(10, powerOfTen));
        return result;
    }

    public long rawLatitude() {
        return latitude & 0xFFFF_FFFFL;
    }

    public long rawLongitude() {
        return longitude & 0xFFFF_FFFFL;
    }

    public long altitude() {
        return altitude & 0xFFFF_FFFFL;
    }

    public Coordinate longitude() {
        return new Coordinate(rawLongitude(), LONGITUDINAL);
    }

    public Coordinate latitude() {
        return new Coordinate(rawLatitude(), LATITUDINAL);
    }

    public double sizeMeters() {
        return size() / (double) CENTIMETERS_MULTIPLIER;
    }

    public double horizontalPrecisionMeters() {
        return horizontalPrecision() / (double) CENTIMETERS_MULTIPLIER;
    }

    public double verticalPrecisionMeters() {
        return verticalPrecision() / (double) CENTIMETERS_MULTIPLIER;
    }

    public double altitudeMeters() {
        double alt = altitude;
        return (alt - BASE_ALTITUDE) / CENTIMETERS_MULTIPLIER;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(latitude());
        sb.append(' ');
        sb.append(longitude());
        sb.append(' ');
        fixedPoint(sb, TWO_PLACES, altitude - BASE_ALTITUDE);
        sb.append("m");

        if (size() != DEFAULT_SIZE_CM
                || horizontalPrecision() != DEFAULT_HORIZONTAL_PRECISION_CM
                || verticalPrecision()
                != DEFAULT_VERTICAL_PRECISION_CM) {
            sb.append(' ');
            fixedPoint(sb, TWO_PLACES, size());
            sb.append("m");
        }
        if (horizontalPrecision() != DEFAULT_HORIZONTAL_PRECISION_CM || verticalPrecision()
                != DEFAULT_VERTICAL_PRECISION_CM) {
            sb.append(' ');
            fixedPoint(sb, TWO_PLACES, horizontalPrecision());
            sb.append("m");
        }
        if (verticalPrecision() != DEFAULT_VERTICAL_PRECISION_CM) {
            sb.append(' ');
            fixedPoint(sb, TWO_PLACES, verticalPrecision());
            sb.append("m");
        }
        return sb.toString();
    }

    private static final int CENTIMETERS_MULTIPLIER = 100;

    private static void fixedPoint(StringBuilder sb, NumberFormat formatter, long value) {
        sb.append(value / CENTIMETERS_MULTIPLIER);
        value %= CENTIMETERS_MULTIPLIER;
        if (value != 0) {
            sb.append(".");
            sb.append(formatter.format(value));
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.size;
        hash = 67 * hash + this.version;
        hash = 67 * hash + this.horizontalPrecision;
        hash = 67 * hash + this.verticalPrecision;
        hash = 67 * hash + (this.latitude ^ (this.latitude >>> 31));
        hash = 67 * hash + (this.longitude ^ (this.longitude >>> 31));
        hash = 67 * hash + (this.altitude ^ (this.altitude >>> 31));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Location)) {
            return false;
        }
        final Location other = (Location) obj;
        return this.altitude == other.altitude && this.longitude == other.longitude
                && this.verticalPrecision == other.verticalPrecision
                && this.horizontalPrecision == other.horizontalPrecision
                && this.version == other.version
                && this.size == other.size;
    }

    /**
     * Represents a latitude or longitude in a Location record.
     */
    public static final class Coordinate {

        private final int degrees;
        private final int minutes;
        private final double seconds;
        private final Direction direction;

        public Coordinate(long value, Orientation orientation) {
            if (value > PRIME_MERIDIAN_AND_EQUATOR) {
                direction = orientation.greater();
            } else {
                direction = orientation.lesser();
            }
            long currValue = value - PRIME_MERIDIAN_AND_EQUATOR;
            if (currValue < 0) {
                currValue = -currValue;
            }
            degrees = ((int) currValue / (3600 * 1000)) % 360;
            currValue = currValue % 3600000;
            minutes = (int) currValue / 60000;
            currValue = currValue % 60000;
            seconds = (double) currValue / 1000D;
        }

        public Coordinate(int degrees, int minutes, double seconds, char direction) {
            this(degrees, minutes, seconds, Direction.match(direction));
        }

        public Coordinate(int degrees, int minutes, double seconds, Direction direction) {
            if (minutes < 0) {
                throw new IllegalArgumentException("Minutes must be > 0: " + minutes);
            }
            if (seconds < 0) {
                throw new IllegalArgumentException("Seconds must be > 0: " + seconds);
            }
            // Normalize to > 0
            if (degrees < 0) {
                degrees = 360 - -degrees;
                minutes = 60 - minutes;
                seconds = 60 - seconds;
                direction = checkNotNull(direction, "direction")
                        .opposite();
            }
            this.direction = checkNotNull(direction, "direction");
            this.degrees = degrees;
            this.minutes = minutes;
            this.seconds = seconds;
        }

        public long toArcSecondThousandths() {
            double result = (3600D * 1000D) * degrees;
            result += (60D * 1000D) * minutes;
            result += 1000D * seconds;
            if (direction.isLesser()) {
                result = -result;
            }
            return (long) (result + PRIME_MERIDIAN_AND_EQUATOR);
        }

        public int degrees() {
            return degrees;
        }

        public int minutes() {
            return minutes;
        }

        public double seconds() {
            return seconds;
        }

        public Direction direction() {
            return direction;
        }

        static final NumberFormat FP_3 = new DecimalFormat("##0.###");

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder().append(degrees());
            if (minutes != 0) {
                sb.append(' ').append(minutes);
            }
            if (seconds != 0) {
                sb.append(' ').append(FP_3.format(seconds));
            }
            sb.append(' ').append(direction);
            return sb.toString();
        }

        @Override
        public int hashCode() {
            return (int) toArcSecondThousandths();
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
            final Coordinate other = (Coordinate) obj;
            if (this.degrees != other.degrees) {
                return false;
            }
            if (this.minutes != other.minutes) {
                return false;
            }
            if (Double.doubleToLongBits(this.seconds) != Double.doubleToLongBits(other.seconds)) {
                return false;
            }
            return this.direction == other.direction;
        }

        public enum Direction {
            N,
            S,
            E,
            W;

            public enum Orientation {
                LATITUDINAL,
                LONGITUDINAL;

                public Direction greater() {
                    switch (this) {
                        case LONGITUDINAL:
                            return E;
                        case LATITUDINAL:
                            return N;
                        default:
                            throw new AssertionError(this);
                    }
                }

                public Direction lesser() {
                    return greater().opposite();
                }
            }

            public boolean isLesser() {
                return !isGreater();
            }

            public boolean isGreater() {
                return orientation().greater() == this;
            }

            public Orientation orientation() {
                return this == N || this == S
                        ?Orientation.LATITUDINAL:Orientation.LONGITUDINAL;
            }

            public static Direction match(char c) {
                switch (c) {
                    case 'n':
                    case 'N':
                        return N;
                    case 's':
                    case 'S':
                        return S;
                    case 'e':
                    case 'E':
                        return E;
                    case 'w':
                    case 'W':
                        return W;
                    default:
                        throw new IllegalArgumentException("Invalid direction " + c);
                }
            }

            public Direction opposite() {
                switch (this) {
                    case N:
                        return S;
                    case S:
                        return N;
                    case E:
                        return W;
                    case W:
                        return E;
                    default:
                        throw new AssertionError(this);
                }
            }
        }
    }
}
