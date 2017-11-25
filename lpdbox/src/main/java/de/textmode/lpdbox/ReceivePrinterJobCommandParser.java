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

import org.slf4j.Logger;

/**
 * The {@link ReceivePrinterJobCommandParser} parses the daemon command "Receive printer job"
 * and sends the response back to the client.
 */
final class ReceivePrinterJobCommandParser extends CommandParser {

    /**
     * Sub-Command Code "Abort Job".
     */
    static final int COMMAND_CODE_ABORT_JOB = 0x01;

    /**
     * Sub-Command Code "Receive control file".
     */
    static final int COMMAND_CODE_RECEIVE_CONTROL_FILE = 0x02;

    /**
     * Sub-Command Code "Receive data file".
     */
    static final int COMMAND_CODE_RECEIVE_DATA_FILE = 0x03;


    /**
     * Constructor.
     */
    ReceivePrinterJobCommandParser(final Logger logger, final DaemonCommandHandler handler) {
        super(logger, handler);
    }

    /**
     * Parses the daemon command "Receive printer job" and delegates the work to
     * the {@link DaemonCommandHandler}.
     */
    @Override
    void parse(final InputStream is, final OutputStream os) throws IOException {

        final String queueName = this.getQueueName(is);

        if (this.getDaemonCommandHandler().startPrinterJob(queueName)) {
            sendPositiveAcknowledgement(os);
        } else {
            // TODO X: Test in real world! sendNegativeAcknowledgement() or close the socket?
            sendNegativeAcknowledgement(os);
            return;
        }

        // TODO X: Check what happens if the connection closes.... SocketException? invoke handler.Abort?!?
        while (true) {
            final int commandCode = is.read();
            if (commandCode == -1) {
                this.getDaemonCommandHandler().endPrinterJob();
                return;
            }

            if (commandCode < COMMAND_CODE_ABORT_JOB || commandCode > COMMAND_CODE_RECEIVE_DATA_FILE) {
                throw new IOException("Client passed an unknwon second level command code 0x"
                        + Integer.toHexString(commandCode)
                        + " for the command receive printer job");
            }

            if (commandCode == COMMAND_CODE_ABORT_JOB) {
                // TODO X: Write an unit test!
                this.getDaemonCommandHandler().abortPrinterJob();
            } else {
                final String parameterString = Util.readLine(is);
                final String[] parameters = parameterString.split("\\s+");

                if (parameters.length != 2) {
                    throw new IOException("Client sent inavlid data: " + parameterString);
                }

                final long fileLength = Long.parseLong(parameters[0]);
                final String fileName = parameters[1];

                sendPositiveAcknowledgement(os);

                final boolean result = commandCode == COMMAND_CODE_RECEIVE_CONTROL_FILE
                    ? this.getDaemonCommandHandler().receiveControlFile(is, (int) fileLength, fileName)
                    : this.getDaemonCommandHandler().receiveDataFile(is, fileLength, fileName);

                // After the file has been sent completely, the client sends an 0x00 as an indication that
                // the file being sent is complete.... We read that here...
                // TODO X: Check existing lpr tools what happens if *NOT* an 0x00 has been sent!
                final boolean fileComplete = is.read() == 0x00;

                if (result && fileComplete) {
                    sendPositiveAcknowledgement(os);
                } else {
                    // TODO X: Write an unit test!
                    // TODO X: Test in real world! sendNegativeAcknowledgement() or close the socket?
                    sendNegativeAcknowledgement(os);
                }
            }
        }
    }

    /**
     * Sends a positive acknowledgement to the client.
     */
    private static void sendPositiveAcknowledgement(final OutputStream os) throws IOException {
        os.write((char) 0x00);
    }

    /**
     * Sends a negative acknowledgement to the client.
     */
    private static void sendNegativeAcknowledgement(final OutputStream os) throws IOException {
        os.write((char) 0x01);
    }
}
