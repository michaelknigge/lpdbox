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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Unit-Tests of class {@link ReceivePrinterJobCommandParser}.
 */
public final class ReceivePrinterJobCommandParserTest extends TestCase {

    private static final Charset ISO8859 = Charset.forName("ISO-8859-1");
    private static final char LINE_FEED = 0x0A;
    private static final char ZERO = 0x00;
    private static final char SPACE = ' ';

    private static class ReceivePrinterJobCommandHandlerStub implements ReceivePrinterJobCommandHandler {
        private PrintJob printJob;
        private byte[] dataFileContent;

        @Override
        public void handle(final PrintJob printJob, final InputStream dataFileStream) throws IOException {

            this.printJob = printJob;
            this.dataFileContent = new byte[(int) this.printJob.getDataFileLength()];

            assertEquals(this.dataFileContent.length, dataFileStream.read(this.dataFileContent));
        }
    };

    /**
     * Performs the test and returns a {@link ReceivePrinterJobCommandHandlerStub} for checking
     * the results.
     */
    private static ReceivePrinterJobCommandHandlerStub performTest(final int ackCount, final String first,
            final int subCode, final String second) throws Exception {

        final ReceivePrinterJobCommandHandlerStub handler = new ReceivePrinterJobCommandHandlerStub();
        final ReceivePrinterJobCommandParser parser = new ReceivePrinterJobCommandParser(handler);

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(first.getBytes(ISO8859));
        data.write((char) subCode);
        data.write(second.getBytes(ISO8859));

        final ByteArrayInputStream is = new ByteArrayInputStream(data.toByteArray());
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        parser.parse(is, os);

        final byte[] acks = os.toByteArray();
        assertEquals(ackCount, acks.length);
        for (int ix = 0; ix < ackCount; ++ix) {
            assertEquals(0x0, acks[ix]);
        }

        return handler;
    }

    /**
     * Performs the test and returns a {@link ReceivePrinterJobCommandHandlerStub} for checking
     * the results.
     */
    private static ReceivePrinterJobCommandHandlerStub performTest(final int ackCount, final byte[] data)
            throws Exception {

        final ReceivePrinterJobCommandHandlerStub handler = new ReceivePrinterJobCommandHandlerStub();
        final ReceivePrinterJobCommandParser parser = new ReceivePrinterJobCommandParser(handler);

        final ByteArrayInputStream is = new ByteArrayInputStream(data);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        parser.parse(is, os);

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
        final ReceivePrinterJobCommandHandlerStub handler = performTest(1, "my_queue\n",
                ReceivePrinterJobCommandParser.COMMAND_CODE_ABORT_JOB, "");

        assertNull(handler.printJob);
        assertNull(handler.dataFileContent);
    }

    /**
     * Receive the control file.
     */
    public void testReceiveControlFile() throws Exception {
        final ReceivePrinterJobCommandHandlerStub handler = performTest(3, "my_queue\n",
                ReceivePrinterJobCommandParser.COMMAND_CODE_RECEIVE_CONTROL_FILE, "4 my_name\nABCD\000");

        // They are all null because the handler gets called after the data file has been
        // received. So this unit test just checks if the code does not crash.
        assertNull(handler.printJob);
        assertNull(handler.dataFileContent);
    }

    /**
     * Receive the data file.
     */
    public void testReceiveDataFile() throws Exception {
        final ReceivePrinterJobCommandHandlerStub handler = performTest(3, "my_queue\n",
                ReceivePrinterJobCommandParser.COMMAND_CODE_RECEIVE_DATA_FILE, "4 my_name\nABCD\000");

        assertEquals("my_queue", handler.printJob.getQueueName());
        assertEquals("my_name", handler.printJob.getDataFileName());
        assertNull(handler.printJob.getControlFileName());
        assertNull(handler.printJob.getControlFileContent());
        assertTrue(Arrays.equals("ABCD".getBytes(ISO8859), handler.dataFileContent));
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

        final ReceivePrinterJobCommandHandlerStub handler = performTest(5, os.toByteArray());

        assertEquals("my_queue", handler.printJob.getQueueName());
        assertEquals("cfA001localhost", handler.printJob.getControlFileName());
        assertTrue(Arrays.equals(controlFileBytes, handler.printJob.getControlFileContent()));

        assertEquals("dfA001localhost", handler.printJob.getDataFileName());
        assertTrue(Arrays.equals(dataFileBytes, handler.dataFileContent));
    }
}
