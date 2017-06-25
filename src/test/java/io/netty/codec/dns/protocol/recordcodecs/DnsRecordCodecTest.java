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
import io.netty.buffer.Unpooled;
import io.netty.codec.dns.protocol.DnsRecordCodec;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.dns.names.NameCodec;
import java.io.IOException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DnsRecordCodecTest {

    @Test
    public void testTxtRecordEncoding() throws Exception {
        testOne("This is some text");
        testOne("my foo=bar", "baz=hoo");
        testOne("foo=\"Oh\"", "whee=whee");
        testOne("foo=\"Oh My\"", "boo=baz", "my word=whee");
        testOne("foo=\"Oh My\"", "boo=baz \"thing\"", "my word=whee");
        testOne("foo=bar", "woo=baz", "this=thing");
        testOne("\"\"\"\"\"\"=\"\"\"\"", "thisIsStuff", "This is some more \"text\" here", "blah");
        testOne("    blah blah");
        testOne("Some more stuff", "More more stuff", "This is stuff too");
        testOne("Don't get confused by trailing\" quotes", "okay\"");
    }

    @Test(expected = TooLongFrameException.class)
    public void testTooLong() throws Exception {
        String[] tooLongStrings = {
            "This will be a long thing, made longer with escaping \" quotes \"",
            "It will be long enough that it is > 255 bytes, which is a problem for DNS packets",
            "Thus we shall make sure that we do not read something crazy and make a mess.",
            "A TXT record stores its length in a single byte;  so a length greater than 255 "
            + "would wrap around zero, and that would be a very bad thing."
        };

        TextRecordCodec codec = new TextRecordCodec();
        ByteBuf buf = Unpooled.buffer();
        codec.write(tooLongStrings, NameCodec.nonCompressingNameCodec(), buf);
    }

    @Test(expected = IOException.class)
    public void testNonAscii() throws Exception {
        String[] notSoGood = {
            "This one is ok",
            "Snowboardová sekce @skimbracing hlásí čtvrté místo v obřáku"
        };
        TextRecordCodec codec = new TextRecordCodec();
        ByteBuf buf = Unpooled.buffer();
        codec.write(notSoGood, NameCodec.nonCompressingNameCodec(), buf);
    }

    private void testOne(CharSequence... in) throws Exception {
        DnsRecordCodec<CharSequence[]> codec = DnsRecordCodecs.text();
        ByteBuf buf = Unpooled.buffer();
        codec.write(in, NameCodec.nonCompressingNameCodec(), buf);

        CharSequence[] out = codec.read(buf, NameCodec.nonCompressingNameCodec(), 500);
        assertEquals("Got different lengths - in: " + a2s(in) + " out: " + a2s(out), in.length, out.length);
        for (int i = 0; i < out.length; i++) {
            assertStringsEqual(in[i], out[i]);
        }
    }

    private void assertStringsEqual(CharSequence a, CharSequence b) {
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a.toString(), b.toString());
    }

    private static String a2s(CharSequence[] seqs) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence seq : seqs) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(seq);
        }
        return sb.toString();
    }
}
