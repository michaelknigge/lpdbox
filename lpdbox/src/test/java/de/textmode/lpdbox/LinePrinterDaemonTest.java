package de.textmode.lpdbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import junit.framework.TestCase;

/**
 * Unit-Tests of class {@link LinePrinterDaemon}.
 */
public final class LinePrinterDaemonTest extends TestCase {

    private static class ReceivePrinterJobCommandHandlerStub implements ReceivePrinterJobCommandHandler {
        private PrintJob printJob;
        private byte[] dataFileContent;

        @Override
        public void handle(final PrintJob printJob, final InputStream dataFileStream) throws IOException {

            this.printJob = printJob;
            this.dataFileContent = new byte[(int) this.printJob.getDataFileLength()];

            dataFileStream.read(this.dataFileContent);
        }
    };

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
    }

    private void readPositiveAcknowledgement(final InputStream is) throws IOException {
        final int ack = is.read();
        assertEquals(0, ack);
    }

    /**
     * "Replays" a real-world examlple.
     */
    public void testRealWorldExample() throws IOException {
        final ReceivePrinterJobCommandHandlerStub handler = new ReceivePrinterJobCommandHandlerStub();

        final LinePrinterDaemon daemon = new LinePrinterDaemonBuilder().portNumber(1515)
                                                                       .receiveJobCommandHandler(handler)
                                                                       .build();

        final Thread thread = new Thread(daemon);
        thread.setDaemon(true);

        daemon.startup();
        thread.start();

        final Socket s = new Socket("localhost", 1515);

        try {
            final InputStream is = s.getInputStream();
            final OutputStream os = s.getOutputStream();

            // Command    :  0x02  (Receive Print File)
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

            // Now write the control file. This file here has been captured in a real world
            // from a z/OS system talking to a print server.
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
                    "2d6f655f74693d45333835413241333430433439394134383339323430433238353939413338383936393338343430443744390a");

            // Now write the control file and the 0x00 "finalizer".
            writeTo(os, ctrl.toString() + "00");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Sub-Command : 0x03 (Receive data file)
            // Length      : 0000000060
            // Name        : dfA963SYSB
            writeTo(os, "033030303030303030363020646641393633535953420a");

            // LPD tells us "yeah, continue...."
            this.readPositiveAcknowledgement(is);

            // Now super simple "linemode" data follows. Just foer data records, each 15 bytes long (including
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
            // the ReceivePrinterJobCommandHandler has been called correctly...
            assertEquals("NW_BETA93_EINZELBLATT", handler.printJob.getQueueName());

            assertEquals("cfA963SYSB", handler.printJob.getControlFileName());
            assertTrue(Arrays.equals(hexToByteArray(ctrl.toString()), handler.printJob.getControlFileContent()));

            final String[] entries = handler.printJob.getControlFileContentAsArray();
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

            assertEquals(60, handler.printJob.getDataFileLength());
            assertEquals("dfA963SYSB", handler.printJob.getDataFileName());
            assertTrue(Arrays.equals(hexToByteArray(records.toString()), handler.dataFileContent));

        } finally {
            s.close();
            daemon.stop();
        }
    }
}
