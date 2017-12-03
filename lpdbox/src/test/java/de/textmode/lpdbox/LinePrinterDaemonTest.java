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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import junit.framework.TestCase;

/**
 * Unit-Tests of class {@link LinePrinterDaemon}.
 */
public final class LinePrinterDaemonTest extends TestCase {

    private static final int PORT_NUMBER = 1515;

    /**
     * Converts a hex string to a byte array.
     */
    private static byte[] hexToByteArray(final String s) {
        return DatatypeConverter.parseHexBinary(s);
    }

    /**
     * Takes a hex string, converts it to a byte array and writes this array
     * to the given OutputStream.
     */
    private static void writeTo(final OutputStream os, final String hexString) throws IOException {
        os.write(hexToByteArray(hexString));
        os.flush();
    }

    private void readPositiveAcknowledgement(final InputStream is) throws IOException {
        final int ack = is.read();
        assertEquals(0, ack);
    }

    private void readNegativeAcknowledgement(final InputStream is) throws IOException {
        final int ack = is.read();
        assertEquals(1, ack);
    }

    /**
     * Returns a 383 bytes long control file. This file here has been captured in a real world
     * from a z/OS system talking to a print server.
     */
    private String getControlFileContent() {
        final StringBuilder ctrl = new StringBuilder();

        ctrl.append("48535953420a");
        ctrl.append("5056505345585439300a");
        ctrl.append("6c646641393633535953420a");
        ctrl.append("55646641393633535953420a");
        ctrl.append("4e4a4f4249443d4a303030393932302c4c495354453d50413030354830312e5359535554320a");
        ctrl.append("2d6f63633d6e6f0a");
        ctrl.append("2d6f636f703d310a");
        ctrl.append("2d6f64617461743d6c0a");
        ctrl.append("2d6f6a6f626e3d42393936303330550a");
        ctrl.append("2d6f6e6f3d504e50584a4553320a");
        ctrl.append("2d6f70613d666f726d733d303031302c636c6173733d4b2c64657374696e6174696f6e3d4c4f43414c0a");
        ctrl.append("2d6f70723d572e42455254484f4c442e3a323836390a");
        ctrl.append("2d6f75733d423939363033300a");
        ctrl.append("2d6f63686172733d475431300a");
        ctrl.append("2d6f706167656465663d5031303031300a");
        ctrl.append("2d6f74693d546573741c447275636b1c42657274686f6c641c50520a");
        ctrl.append("2d6f7472633d6e6f0a");
        ctrl.append("2d6f66696c65666f726d61743d7265636f72640a");
        ctrl.append("2d6f663d4631535444310a");
        ctrl.append("2d6f6e613d572e42455254484f4c442e3a323836390a");
        ctrl.append(
                "2d6f655f74693d45333835413241333430433439"
                        + "394134383339323430433238353939413338383936393338343430443744390a");

        return ctrl.toString();
    }

    public void fooXXX() throws Exception {
        final Socket s = new Socket("localhost", PORT_NUMBER);

        try {
            final InputStream is = s.getInputStream();
            final OutputStream os = s.getOutputStream();

            // Command    : 0x02  (Receive Print File)
            // Queue Name : NW_BETA93_EINZELBLATT
            writeTo(os, "024e575f4245544139335f45494e5a454c424c4154540a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Sub-Command : 0x02 (Receive control file)
            // Length      : 0000000383
            // Name        : cfA963SYSB
            writeTo(os, "023030303030303033383320636641393633535953420a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Now write the control file (plus 0x00 finalizer). This file here has been
            // captured in a real world from a z/OS system talking to a print server.
            writeTo(os, this.getControlFileContent() + "00");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Sub-Command : 0x03 (Receive data file)
            // Length      : 0000000060
            // Name        : dfA963SYSB
            writeTo(os, "033030303030303030363020646641393633535953420a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Now super simple "linemode" data follows. Just four data records, each 15 bytes long (including
            // an prefixed two byte record length field). Here too, after the data an 0x00 is sent as an
            // indication that the file being sent is complete.
            final StringBuilder records = new StringBuilder();

            records.append("000d4040e3c5e2e3e9c5c9d3c540f1");
            records.append("000d4040e3c5e2e3e9c5c9d3c540f2");
            records.append("000d4040e3c5e2e3e9c5c9d3c540f3");
            records.append("000d4040e3c5e2e3e9c5c9d3c540f4");

            // Now write the data file and the 0x00 "finalizer".
            writeTo(os, records.toString() + "00");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

        } finally {
            s.close();
        }
    }

    /**
     * "Replays" a real-world example.
     */
    public void testRealWorldExample() throws Exception {
        final DaemonCommandHandlerStubFactory stubFactory = new DaemonCommandHandlerStubFactory();
        final DaemonCommandHandlerStub handler = stubFactory.getStubHandler();

        final LinePrinterDaemon daemon = new LinePrinterDaemonBuilder(stubFactory)
                .portNumber(PORT_NUMBER)
                .build();

        assertFalse(daemon.isRunning());

        final Thread thread = new Thread(daemon);
        thread.setDaemon(true);

        daemon.startup();
        assertFalse(daemon.isRunning());

        thread.start();

        final Socket s = new Socket("localhost", PORT_NUMBER);

        try {
            final InputStream is = s.getInputStream();
            final OutputStream os = s.getOutputStream();

            // Command    : 0x02  (Receive Print File)
            // Queue Name : NW_BETA93_EINZELBLATT
            writeTo(os, "024e575f4245544139335f45494e5a454c424c4154540a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            assertTrue(daemon.isRunning());

            // Sub-Command : 0x02 (Receive control file)
            // Length      : 0000000383
            // Name        : cfA963SYSB
            writeTo(os, "023030303030303033383320636641393633535953420a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Now write the control file (plus 0x00 finalizer). This file here has been
            // captured in a real world from a z/OS system talking to a print server.
            writeTo(os, this.getControlFileContent() + "00");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Sub-Command : 0x03 (Receive data file)
            // Length      : 0000000060
            // Name        : dfA963SYSB
            writeTo(os, "033030303030303030363020646641393633535953420a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Now super simple "linemode" data follows. Just four data records, each 15 bytes long (including
            // an prefixed two byte record length field). Here too, after the data an 0x00 is sent as an
            // indication that the file being sent is complete.
            final StringBuilder records = new StringBuilder();

            records.append("000d4040e3c5e2e3e9c5c9d3c540f1");
            records.append("000d4040e3c5e2e3e9c5c9d3c540f2");
            records.append("000d4040e3c5e2e3e9c5c9d3c540f3");
            records.append("000d4040e3c5e2e3e9c5c9d3c540f4");

            // Now write the data file and the 0x00 "finalizer".
            writeTo(os, records.toString() + "00");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // ok, now as we are here, we have successfully created an print job! Now check if
            // the DaemonCommandHandler has been called correctly...
            assertEquals("NW_BETA93_EINZELBLATT", handler.getPrinterQueueName());

            assertEquals("cfA963SYSB", handler.getControlFileName());
            assertTrue(Arrays.equals(hexToByteArray(this.getControlFileContent()), handler.getControlFileContent()));

            final String[] entries = handler.getControlFileContentAsArray();
            assertEquals("HSYSB", entries[0]);
            assertEquals("PVPSEXT90", entries[1]);
            assertEquals("ldfA963SYSB", entries[2]);
            assertEquals("UdfA963SYSB", entries[3]);
            assertEquals("NJOBID=J0009920,LISTE=PA005H01.SYSUT2", entries[4]);
            assertEquals("-occ=no", entries[5]);
            assertEquals("-ocop=1", entries[6]);
            assertEquals("-odatat=l", entries[7]);
            assertEquals("-ojobn=B996030U", entries[8]);
            assertEquals("-ono=PNPXJES2", entries[9]);
            assertEquals("-opa=forms=0010,class=K,destination=LOCAL", entries[10]);
            assertEquals("-opr=W.BERTHOLD.:2869", entries[11]);
            assertEquals("-ous=B996030", entries[12]);
            assertEquals("-ochars=GT10", entries[13]);
            assertEquals("-opagedef=P10010", entries[14]);
            assertEquals("-oti=TestDruckBertholdPR", entries[15]);
            assertEquals("-otrc=no", entries[16]);
            assertEquals("-ofileformat=record", entries[17]);
            assertEquals("-of=F1STD1", entries[18]);
            assertEquals("-ona=W.BERTHOLD.:2869", entries[19]);
            assertEquals("-oe_ti=E385A2A340C499A4839240C28599A38896938440D7D9", entries[20]);

            assertEquals(60, handler.getDataFileLength());
            assertEquals("dfA963SYSB", handler.getDataFileName());
            assertTrue(Arrays.equals(hexToByteArray(records.toString()), handler.getDataFileContent()));

        } finally {
            s.close();
            daemon.stop(5000);
        }
    }

    /**
     * Sends an invalid control code.
     */
    public void testInvalidControlCode() throws Exception {

        final DaemonCommandHandlerStubFactory stubFactory = new DaemonCommandHandlerStubFactory();
        final DaemonCommandHandlerStub handler = stubFactory.getStubHandler();

        final LinePrinterDaemon daemon = new LinePrinterDaemonBuilder(stubFactory)
                .portNumber(PORT_NUMBER)
                .maxThreads(1)
                .build();

        final Thread thread = new Thread(daemon);
        thread.setDaemon(true);
        daemon.startup();
        thread.start();

        // First we send an invalid control code - the server will close the connection...
        final Socket s = new Socket("localhost", PORT_NUMBER);
        try {
            writeTo(s.getOutputStream(), "000000000a");

            // On my Windows box I get a -1 when doing a read on the socket....
            assertEquals(-1, s.getInputStream().read());
        } catch (final SocketException e) {
            // .... and on Linux a SocketException is thrown on the read()...
            assertFalse(e.getMessage().isEmpty());
        } finally {
            s.close();
        }

        // The server is still running and should accept and handle connections - we check this now
        final Socket s2 = new Socket("localhost", PORT_NUMBER);
        try {
            writeTo(s2.getOutputStream(), "016C700A");
        } finally {
            s2.close();
        }

        // If we stop the daemon now, the thread that handles the connection might not have
        // been scheduled... So we check the received printer queue name in a loop....
        for (int ix = 0; ix < 50; ++ix) {
            if ("lp".equals(handler.getPrinterQueueName())) {
                break;
            }
            Thread.sleep(50);
        }

        daemon.stop(5000);
        assertEquals("lp", handler.getPrinterQueueName());
    }

    /**
     * Closes the connection in the middle of the transfer of the control file.
     */
    public void testCloseConnectionWhileTransferringControlFile() throws Exception {
        final DaemonCommandHandlerStubFactory stubFactory = new DaemonCommandHandlerStubFactory();
        final DaemonCommandHandlerStub handler = stubFactory.getStubHandler();

        final LinePrinterDaemon daemon = new LinePrinterDaemonBuilder(stubFactory)
                .portNumber(PORT_NUMBER)
                .build();

        final Thread thread = new Thread(daemon);
        thread.setDaemon(true);
        daemon.startup();
        thread.start();

        final Socket s = new Socket("localhost", PORT_NUMBER);

        try {
            final InputStream is = s.getInputStream();
            final OutputStream os = s.getOutputStream();

            // Command    : 0x02  (Receive Print File)
            // Queue Name : lp
            writeTo(os, "026c700a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Sub-Command : 0x02 (Receive control file)
            // Length      : 0000000383
            // Name        : cfA963SYSB
            writeTo(os, "023030303030303033383320636641393633535953420a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Now write the control file. Write just a few bytes, then close the connection.
            // The server should handle this...
            writeTo(os, "deadbeef");
            os.flush();

            s.close();

            assertTrue(daemon.isRunning());
            daemon.stop(5000);
            assertFalse(daemon.isRunning());

            assertFalse(handler.isAborted());
            assertFalse(handler.isControlFileComplete());
            assertTrue(handler.isEnded());
        } finally {
            s.close();
            daemon.stop(5000);
        }
    }

    /**
     * Closes the connection in the middle of the transfer of the data file.
     */
    public void testCloseConnectionWhileTransferringDataFile() throws Exception {
        final DaemonCommandHandlerStubFactory stubFactory = new DaemonCommandHandlerStubFactory();
        final DaemonCommandHandlerStub handler = stubFactory.getStubHandler();

        final LinePrinterDaemon daemon = new LinePrinterDaemonBuilder(stubFactory)
                .portNumber(PORT_NUMBER)
                .build();

        final Thread thread = new Thread(daemon);
        thread.setDaemon(true);
        daemon.startup();
        thread.start();

        final Socket s = new Socket("localhost", PORT_NUMBER);

        try {
            final InputStream is = s.getInputStream();
            final OutputStream os = s.getOutputStream();

            // Command    : 0x02  (Receive Print File)
            // Queue Name : lp
            writeTo(os, "026c700a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            assertTrue(daemon.isRunning());

            // Sub-Command : 0x02 (Receive control file)
            // Length      : 0000000383
            // Name        : cfA963SYSB
            writeTo(os, "023030303030303033383320636641393633535953420a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Now write the control file (plus 0x00 finalizer). This file here has been
            // captured in a real world from a z/OS system talking to a print server.
            writeTo(os, this.getControlFileContent() + "00");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Sub-Command : 0x03 (Receive data file)
            // Length      : 0000000060
            // Name        : dfA963SYSB
            writeTo(os, "033030303030303030363020646641393633535953420a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Now write the control file. Write just a few bytes, then close the connection.
            // The server should handle this...
            writeTo(os, "deadbeef");
            os.flush();

            s.close();

            assertTrue(daemon.isRunning());
            daemon.stop(5000);
            assertFalse(daemon.isRunning());

            assertFalse(handler.isAborted());
            assertTrue(handler.isControlFileComplete());
            assertFalse(handler.isDataFileComplete());
            assertTrue(handler.isEnded());
        } finally {
            s.close();
            daemon.stop(5000);
        }
    }

    /**
     * Closes the connection between the control file and the data file.
     */
    public void testCloseConnectionBetweenControlAndDataFile() throws Exception {
        final DaemonCommandHandlerStubFactory stubFactory = new DaemonCommandHandlerStubFactory();
        final DaemonCommandHandlerStub handler = stubFactory.getStubHandler();

        final LinePrinterDaemon daemon = new LinePrinterDaemonBuilder(stubFactory)
                .portNumber(PORT_NUMBER)
                .build();

        final Thread thread = new Thread(daemon);
        thread.setDaemon(true);
        daemon.startup();
        thread.start();

        final Socket s = new Socket("localhost", PORT_NUMBER);

        try {
            final InputStream is = s.getInputStream();
            final OutputStream os = s.getOutputStream();

            // Command    : 0x02  (Receive Print File)
            // Queue Name : lp
            writeTo(os, "026c700a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            assertTrue(daemon.isRunning());

            // Sub-Command : 0x02 (Receive control file)
            // Length      : 0000000383
            // Name        : cfA963SYSB
            writeTo(os, "023030303030303033383320636641393633535953420a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Now write the control file (plus 0x00 finalizer). This file here has been
            // captured in a real world from a z/OS system talking to a print server.
            writeTo(os, this.getControlFileContent() + "00");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            s.close();

            assertTrue(daemon.isRunning());
            daemon.stop(5000);
            assertFalse(daemon.isRunning());

            assertFalse(handler.isAborted());
            assertTrue(handler.isControlFileComplete());
            assertFalse(handler.isDataFileComplete());
            assertTrue(handler.isEnded());
        } finally {
            s.close();
            daemon.stop(5000);
        }
    }

    /**
     * Printing to a locked queue.
     */
    public void testPrintingToLockedQueue() throws Exception {
        final DaemonCommandHandlerStubFactory stubFactory = new DaemonCommandHandlerStubFactory();
        final DaemonCommandHandlerStub handler = stubFactory.getStubHandler();

        final LinePrinterDaemon daemon = new LinePrinterDaemonBuilder(stubFactory)
                .portNumber(PORT_NUMBER)
                .build();

        final Thread thread = new Thread(daemon);
        thread.setDaemon(true);
        daemon.startup();
        thread.start();

        final Socket s = new Socket("localhost", PORT_NUMBER);

        try {
            handler.lockQueue();

            final InputStream is = s.getInputStream();
            final OutputStream os = s.getOutputStream();

            // Command    : 0x02  (Receive Print File)
            // Queue Name : lp
            writeTo(os, "026c700a");

            // LPD tells us "sorry, it's closed...."
            this.readNegativeAcknowledgement(is);

            assertTrue(daemon.isRunning());
        } finally {
            s.close();
            daemon.stop(5000);
        }
    }

    /**
     * Check if the Server handles {@link DaemonCommandHandler} == null correctly.
     */
    public void testDaemonCommandHandlerIsNull() throws Exception {
        final DaemonCommandHandlerFactory factory = new DaemonCommandHandlerFactory() {
            @Override
            public DaemonCommandHandler create() {
                return null;
            }
        };

        final LinePrinterDaemon daemon = new LinePrinterDaemonBuilder(factory)
                .portNumber(PORT_NUMBER)
                .build();

        final Thread thread = new Thread(daemon);
        thread.setDaemon(true);
        daemon.startup();
        thread.start();

        final Socket s = new Socket("localhost", PORT_NUMBER);

        try {
            final InputStream is = s.getInputStream();
            final OutputStream os = s.getOutputStream();

            // Command    : 0x01  (Print jobs)
            // Queue Name : lp
            writeTo(os, "016c700a");
            os.flush();

            assertEquals(-1, is.read());
        } catch (final SocketException e) {
            // This ok, too.... On my windows box the is.read() above
            // returns -1... Linux throws an SocketException...
            return;
        } finally {
            s.close();
            assertTrue(daemon.isRunning());
            daemon.stop(5000);
        }
    }

    /**
     * Check if the Server handles if {@link DaemonCommandHandler} throws an error.
     */
    public void testDaemonCommandHandlerThrowsAnError() throws Exception {
        final DaemonCommandHandlerFactory factory = new DaemonCommandHandlerFactory() {
            @Override
            public DaemonCommandHandler create() {
                throw new Error("foo");
            }
        };

        final LinePrinterDaemon daemon = new LinePrinterDaemonBuilder(factory)
                .portNumber(PORT_NUMBER)
                .build();

        final Thread thread = new Thread(daemon);
        thread.setDaemon(true);
        daemon.startup();
        thread.start();

        final Socket s = new Socket("localhost", PORT_NUMBER);

        try {
            final InputStream is = s.getInputStream();
            final OutputStream os = s.getOutputStream();

            // Command    : 0x01  (Print jobs)
            // Queue Name : lp
            writeTo(os, "016c700a");
            os.flush();

            assertEquals(-1, is.read());
        } catch (final SocketException e) {
            // This ok, too.... On my windows box the is.read() above
            // returns -1... Linux throws an SocketException...
            return;
        } finally {
            s.close();
            assertTrue(daemon.isRunning());
            daemon.stop(5000);
        }
    }

    /**
     * Send the Report Queue State Short to the server.
     */
    public void testReportQueueStateShort() throws Exception {
        // Command    : 0x03  (ReportQueueStateShort)
        // Queue Name : lp
        this.testReportQueueState("036c700a", "this is a short list");
    }

    /**
     * Send the Report Queue State Long to the server.
     */
    public void testReportQueueStateLong() throws Exception {
        // Command    : 0x04  (ReportQueueStateShort)
        // Queue Name : lp
        this.testReportQueueState("046c700a", "this is a long list");
    }

    /**
     * Send the Report Queue State Long/Short to the server.
     */
    private void testReportQueueState(final String hexString, final String expected) throws Exception {
        final DaemonCommandHandlerStubFactory stubFactory = new DaemonCommandHandlerStubFactory();
        final DaemonCommandHandlerStub handler = stubFactory.getStubHandler();

        final LinePrinterDaemon daemon = new LinePrinterDaemonBuilder(stubFactory)
                .portNumber(PORT_NUMBER)
                .build();

        final Thread thread = new Thread(daemon);
        thread.setDaemon(true);
        daemon.startup();
        thread.start();

        final Socket s = new Socket("localhost", PORT_NUMBER);

        try {
            handler.lockQueue();

            final InputStream is = s.getInputStream();
            final OutputStream os = s.getOutputStream();

            writeTo(os, hexString);

            final byte[] buffer = new byte[256];
            final int len = is.read(buffer);

            assertTrue(new String(buffer, Util.ISO_8859_1).substring(0, len).equals(expected));

        } finally {
            s.close();
            daemon.stop(5000);
        }
    }

    /**
     * Remove a print job.
     */
    public void testRemovePrintJob() throws Exception {
        final DaemonCommandHandlerStubFactory stubFactory = new DaemonCommandHandlerStubFactory();
        final DaemonCommandHandlerStub handler = stubFactory.getStubHandler();

        final LinePrinterDaemon daemon = new LinePrinterDaemonBuilder(stubFactory)
                .portNumber(PORT_NUMBER)
                .build();

        final Thread thread = new Thread(daemon);
        thread.setDaemon(true);
        daemon.startup();
        thread.start();

        final Socket s = new Socket("localhost", PORT_NUMBER);

        try {
            handler.lockQueue();

            final InputStream is = s.getInputStream();
            final OutputStream os = s.getOutputStream();

            // Command    : 0x05  (Remove Print Job)
            // Queue Name : lp
            // Agent      : ABC
            writeTo(os, "056c70204142430a");

            os.flush();

            assertEquals(-1, is.read());
            assertTrue(daemon.isRunning());
            assertEquals("ABC", handler.getUserName());
            assertEquals("lp", handler.getPrinterQueueName());
        } finally {
            s.close();
            daemon.stop(5000);
        }
    }
}
