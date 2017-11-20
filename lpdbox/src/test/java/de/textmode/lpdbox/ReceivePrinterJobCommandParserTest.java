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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

/**
 * Unit-Tests of class {@link ReceivePrinterJobCommandParser}.
 */
public final class ReceivePrinterJobCommandParserTest extends TestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceivePrinterJobCommandParserTest.class);

    private static final Charset ISO8859 = Charset.forName("ISO-8859-1");
    private static final char LINE_FEED = 0x0A;
    private static final char ZERO = 0x00;
    private static final char SPACE = ' ';


    /**
     * Performs the test and returns a {@link DaemonCommandHandlerStub} for checking
     * the results.
     */
    private static DaemonCommandHandlerStub performTest(final int ackCount, final String first,
            final int subCode, final String second) throws Exception {

        final DaemonCommandHandlerStub handler = new DaemonCommandHandlerStub();

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(first.getBytes(ISO8859));
        data.write((char) subCode);
        data.write(second.getBytes(ISO8859));

        final ByteArrayInputStream is = new ByteArrayInputStream(data.toByteArray());
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        ReceivePrinterJobCommandParser.parse(LOGGER, handler, is, os);

        final byte[] acks = os.toByteArray();
        assertEquals(ackCount, acks.length);
        for (int ix = 0; ix < ackCount; ++ix) {
            assertEquals(0x0, acks[ix]);
        }

        return handler;
    }

    /**
     * Performs the test and returns a {@link DaemonCommandHandlerStub} for checking
     * the results.
     */
    private static DaemonCommandHandlerStub performTest(final int ackCount, final byte[] data)
            throws Exception {

        final DaemonCommandHandlerStub handler = new DaemonCommandHandlerStub();

        final ByteArrayInputStream is = new ByteArrayInputStream(data);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        ReceivePrinterJobCommandParser.parse(LOGGER, handler, is, os);

        final byte[] acks = os.toByteArray();
        assertEquals(ackCount, acks.length);
        for (int ix = 0; ix < ackCount; ++ix) {
            assertEquals(0x0, acks[ix]);
        }

        return handler;
    }

    /**
     * Abort the current "Receive job" command.
     */
    public void testAbort() throws Exception {
        final DaemonCommandHandlerStub handler = performTest(
                1,
                "my_queue\n",
                ReceivePrinterJobCommandParser.COMMAND_CODE_ABORT_JOB,
                "");

        assertEquals("my_queue", handler.getPrinterQueueName());

        assertNull(handler.getControlFileName());
        assertNull(handler.getControlFileContent());

        assertNull(handler.getDataFileName());
        assertNull(handler.getDataFileContent());
    }

    /**
     * Receive the control file.
     */
    public void testReceiveControlFile() throws Exception {
        final DaemonCommandHandlerStub handler = performTest(
                3,
                "my_queue\n",
                ReceivePrinterJobCommandParser.COMMAND_CODE_RECEIVE_CONTROL_FILE,
                "4 my_name\nABCD\000");

        assertEquals("my_queue", handler.getPrinterQueueName());

        assertEquals(4, handler.getControlFileLength());
        assertEquals("my_name", handler.getControlFileName());
        assertTrue(Arrays.equals("ABCD".getBytes(ISO8859), handler.getControlFileContent()));

        assertNull(handler.getDataFileName());
        assertNull(handler.getDataFileContent());
    }

    /**
     * Receive the data file.
     */
    public void testReceiveDataFile() throws Exception {
        final DaemonCommandHandlerStub handler = performTest(
                3,
                "my_queue\n",
                ReceivePrinterJobCommandParser.COMMAND_CODE_RECEIVE_DATA_FILE,
                "4 my_name\nABCD\000");

        assertEquals("my_queue", handler.getPrinterQueueName());

        assertEquals(4, handler.getDataFileLength());
        assertEquals("my_name", handler.getDataFileName());
        assertTrue(Arrays.equals("ABCD".getBytes(ISO8859), handler.getDataFileContent()));

        assertNull(handler.getControlFileName());
        assertNull(handler.getControlFileContent());
    }

    /**
     * Receive the control and data file.
     */
    public void testReceiveAllFiles() throws Exception {

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write("my_queue".getBytes(ISO8859));
        os.write(LINE_FEED);

        final String controlFileString = "ABC\nDEF\nFoo\n\000";
        final byte[] controlFileBytes = controlFileString.getBytes(ISO8859);

        os.write((char) ReceivePrinterJobCommandParser.COMMAND_CODE_RECEIVE_CONTROL_FILE);
        os.write(Integer.toString(controlFileBytes.length).getBytes(ISO8859));
        os.write(SPACE);
        os.write("cfA001localhost".getBytes(ISO8859));
        os.write(LINE_FEED);
        os.write(controlFileBytes);
        os.write(ZERO);

        final String dataFileString = "This is the data to print";
        final byte[] dataFileBytes = dataFileString.getBytes(ISO8859);

        os.write((char) ReceivePrinterJobCommandParser.COMMAND_CODE_RECEIVE_DATA_FILE);
        os.write(Integer.toString(dataFileBytes.length).getBytes(ISO8859));
        os.write(SPACE);
        os.write("dfA001localhost".getBytes(ISO8859));
        os.write(LINE_FEED);
        os.write(dataFileBytes);
        os.write(ZERO);

        final DaemonCommandHandlerStub handler = performTest(5, os.toByteArray());

        assertEquals("my_queue", handler.getPrinterQueueName());

        assertEquals(controlFileBytes.length, handler.getControlFileLength());
        assertEquals("cfA001localhost", handler.getControlFileName());
        assertTrue(Arrays.equals(controlFileBytes, handler.getControlFileContent()));

        assertEquals(dataFileBytes.length, handler.getDataFileLength());
        assertEquals("dfA001localhost", handler.getDataFileName());
        assertTrue(Arrays.equals(dataFileBytes, handler.getDataFileContent()));
    }

    /**
     * Without an queue name.
     */
    public void testWithoutQueue() throws Exception {
        try {
            performTest(
                    3,
                    "\n",
                    ReceivePrinterJobCommandParser.COMMAND_CODE_RECEIVE_CONTROL_FILE,
                    "");

            fail();
        } catch (final IOException e) {
            assertEquals("No queue name was provided by the client", e.getMessage());
        }
    }

    /**
     * Without an queue name (just spaces).
     */
    public void testInvalidQueueName() throws Exception {
        try {
            performTest(
                    3,
                    "   \n",
                    ReceivePrinterJobCommandParser.COMMAND_CODE_RECEIVE_CONTROL_FILE,
                    "");

            fail();
        } catch (final IOException e) {
            assertEquals("No queue name was provided by the client", e.getMessage());
        }
    }

    /**
     * An invalid second level command code.
     */
    public void testInvalidSecondLevelCommandCode() throws Exception {
        try {
            performTest(
                    3,
                    "my_queue\n",
                    123,
                    "4 my_name\nABCD\000");

            fail();
        } catch (final IOException e) {
            assertEquals(
                    "Client passed an unknwon second level command code 0x7b for the command receive printer job",
                    e.getMessage());
        }
    }
}
