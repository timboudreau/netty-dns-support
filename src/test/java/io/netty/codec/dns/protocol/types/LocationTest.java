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

import io.netty.codec.dns.protocol.types.Location.Coordinate;
import org.junit.Test;

import static io.netty.codec.dns.protocol.types.Location.Coordinate.Direction.E;
import static io.netty.codec.dns.protocol.types.Location.Coordinate.Direction.N;
import static io.netty.codec.dns.protocol.types.Location.Coordinate.Direction.Orientation.LATITUDINAL;
import static io.netty.codec.dns.protocol.types.Location.Coordinate.Direction.Orientation.LONGITUDINAL;
import static io.netty.codec.dns.protocol.types.Location.Coordinate.Direction.W;
import static io.netty.codec.dns.protocol.types.Location.DEFAULT_HORIZONTAL_PRECISION_CM;
import static io.netty.codec.dns.protocol.types.Location.DEFAULT_SIZE_CM;
import static io.netty.codec.dns.protocol.types.Location.DEFAULT_VERTICAL_PRECISION_CM;
import static io.netty.codec.dns.protocol.types.Location.convert;
import static io.netty.codec.dns.protocol.types.Location.unconvert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class LocationTest {

    @Test
    public void coordinateTest() {
        Coordinate a = new Coordinate(42, 28, 38.928, N);

        Coordinate b = new Coordinate(a.degrees(), a.minutes(), a.seconds(), a.direction());

        long ast = a.toArcSecondThousandths();
        Coordinate c = new Coordinate(ast, LATITUDINAL);

        assertEquals(a, b);
        assertEquals(a, c);
        assertEquals(b, c);

        assertEquals(42, a.degrees());
        assertEquals(28, a.minutes());
        assertEquals(38.928D, a.seconds(), 0.1D);

        assertEquals(42, c.degrees());
        assertEquals(28, c.minutes());
        assertEquals(38.928D, c.seconds(), 0.1D);

        // 72 36 24.465
        a = new Coordinate(72, 36, 24.465, W);

        Coordinate a1 = new Coordinate(72, 36, 24.465, E);
        assertNotEquals("East and west should not be same value", a.toArcSecondThousandths(),
                a1.toArcSecondThousandths());

        b = new Coordinate(a.degrees(), a.minutes(), a.seconds(), a.direction());

        ast = a.toArcSecondThousandths();
        c = new Coordinate(ast, LONGITUDINAL);

        assertEquals(a.direction(), c.direction());

        assertEquals(a, b);
        assertEquals(a, c);
        assertEquals(b, c);

        assertEquals(72, c.degrees());
        assertEquals(36, c.minutes());
        assertEquals(24.465D, c.seconds(), 0.1D);

        a = new Coordinate(72, 36, 24.465, E);

        b = new Coordinate(a.degrees(), a.minutes(), a.seconds(), a.direction());

        ast = a.toArcSecondThousandths();
        c = new Coordinate(ast, LONGITUDINAL);

        assertEquals(a, b);
        assertEquals(b, c);
        assertEquals(a, c);

        assertEquals(72, c.degrees());
        assertEquals(36, c.minutes());
        assertEquals(24.465D, c.seconds(), 0.1D);

        a = new Coordinate(-72, 36, 24.465, W);
        assertEquals(72, c.degrees());
        assertEquals(36, c.minutes());
        assertEquals(24.465D, c.seconds(), 0.1D);
        assertEquals(E, c.direction());

        b = new Coordinate(a.degrees(), a.minutes(), a.seconds(), a.direction());

        ast = a.toArcSecondThousandths();
        c = new Coordinate(ast, LONGITUDINAL);

        assertEquals(a, c);
        assertEquals(a, b);
        assertEquals(b, c);

        assertEquals(288, a.degrees());
        assertEquals(a.degrees(), b.degrees());

        assertEquals(288, c.degrees());
        assertEquals(24, c.minutes());
        assertEquals(35.535D, c.seconds(), 0.1D);
        assertEquals(E, a.direction());
    }

    @Test
    public void testConvertWithoutLoss() {
        Coordinate latitude = new Coordinate(42, 28, 38.928, N);
        Coordinate longitude = new Coordinate(72, 36, 24.465, W);
        Location loc = new Location(61, 10, 10, latitude, longitude, 100);

        assertEquals(latitude, loc.latitude());
        assertEquals(longitude, loc.longitude());
    }

    @Test
    @SuppressWarnings("UnusedAssignment")
    public void testParse() {
        Coordinate latitude = new Coordinate(42, 28, 38.928, N);
        Coordinate longitude = new Coordinate(72, 36, 24.465, W);
        Location loc = new Location(10, 100, 10, latitude, longitude, 61D);

        String s = "42 28 38.928 N 72 36 24.465 W 61.00m 10m 100m 10m";
        Location got = Location.parse(s);

        assertEquals(loc.toString(), got.toString());
        assertEquals(loc, got);

        s = "42 28 N 72 36 24.465 W 61.00m 10m 100m 10m";
        got = Location.parse(s);
        assertEquals(42, got.latitude().degrees());
        assertEquals(28, got.latitude().minutes());
        assertEquals(0, got.latitude().seconds(), 0.01);
        assertEquals(61, got.altitudeMeters(), 0.1D);
        assertEquals(10, got.verticalPrecisionMeters(), 0.1D);
        s = "42 N 72 36 24.465 W 61.00m 10m 100m 10m";
        got = Location.parse(s);

        s = "42 N 72 36 24.465 W 61.00m 10m 100m";
        got = Location.parse(s);

        s = "42 N 72 W 61m";
        got = Location.parse(s);
        assertEquals(42, got.latitude().degrees());
        assertEquals(72, got.longitude().degrees());
        assertEquals(DEFAULT_HORIZONTAL_PRECISION_CM, got.horizontalPrecision());
        assertEquals(DEFAULT_VERTICAL_PRECISION_CM, got.verticalPrecision());
        assertEquals(DEFAULT_SIZE_CM, got.size());
        assertEquals(61, got.altitudeMeters(), 0.1);
        assertEquals(got, Location.parse(got.toString()));
        assertEquals(s, got.toString());
    }

    @Test
    public void testParse2() {
        //   42 21 43.952 N 71 5 6.344 W -24m 1m 200m
        String s = "42 21 43.952 N 71 5 6.344 W -24m 1m 200m";
        Location loc = Location.parse(s);
        assertEquals(s, loc.toString());

        s = "42 21 43.952 N 71 5 6.344 W -24m 1m 200m 100m";
        loc = Location.parse(s);
        assertEquals(s, loc.toString());
    }

    @Test
    public void testConvert() {
        assertEquals(1000000, convert((byte) 165));
        assertEquals(1000, convert((byte) 162));
        assertEquals(100, convert((byte) 18));

        for (int base = 9; base > 0; base--) {
            for (int exp = 9; exp >= 0; exp--) {
                long val = (long) (exp == 0?base:base * Math.pow(10, exp));
                byte uc = unconvert(val);
                long cv = convert(uc);
                assertEquals(uc + " -> " + val + " <- " + cv, val, cv);
            }
        }
    }
}
