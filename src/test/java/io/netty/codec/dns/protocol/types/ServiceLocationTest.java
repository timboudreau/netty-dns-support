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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ServiceLocationTest {

    @Test
    public void testEncodeName() {
        ServiceLocation loc = new ServiceLocation("ssh", "tcp", "somehost.example.com");
        assertEquals("_ssh._tcp.somehost.example.com", loc.toString());
    }

    @Test
    public void testParse() {
        ServiceLocation loc = new ServiceLocation("_ssh._tcp.somehost.example.com");
        assertEquals("ssh", loc.serviceName);
        assertEquals("tcp", loc.protocol);
        assertEquals("somehost.example.com", loc.host);
    }

}
