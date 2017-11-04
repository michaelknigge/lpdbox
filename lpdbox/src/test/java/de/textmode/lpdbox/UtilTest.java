package de.textmode.lpdbox;

/*
 * Copyright 2017 Michael Knigge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import junit.framework.TestCase;

/**
 * Unit-Tests of class {@link Util}.
 */
public final class UtilTest extends TestCase {

    private static final Charset ISO8859 = Charset.forName("ISO-8859-1");
    private static final String LINE_FEED = "\n";

    /**
     * Performs the test and expects success.
     */
    private static void performSuccessfulReadLineTest(final String in, final String ex) throws IOException {
        final ByteArrayInputStream is = new ByteArrayInputStream(in.getBytes(ISO8859));
        assertEquals(ex, Util.readLine(is));
    }

    /**
     * Performs the test and expects an thrown exception.
     */
    private static void performUnsuccessfulReadLineTest(final String in) throws IOException {
        final ByteArrayInputStream is = new ByteArrayInputStream(in.getBytes(ISO8859));
        try {
            Util.readLine(is);
            fail();
        } catch (final IOException e) {
            assertEquals("Unexpectedly reached the end of the data stream. The connection might be lost.",
                    e.getMessage());
        }
    }

    /**
     * Reading an empty {@link String}.
     */
    public void testEmptyString() throws Exception {
        performSuccessfulReadLineTest(LINE_FEED, "");
    }

    /**
     * Reading a simple {@link String}.
     */
    public void testSimpleString() throws Exception {
        performSuccessfulReadLineTest("foo" + LINE_FEED, "foo");
    }

    /**
     * Reading a string with a white space {@link String}.
     */
    public void testStringWithSpaces() throws Exception {
        performSuccessfulReadLineTest("foo ba" + LINE_FEED, "foo ba");
    }

    /**
     * Reading an empty stream.
     */
    public void testEmptyStream() throws Exception {
        performUnsuccessfulReadLineTest("");
    }

    /**
     * Reading a truncated stream.
     */
    public void testTruncatedStream() throws Exception {
        performUnsuccessfulReadLineTest("foo");
    }
}
