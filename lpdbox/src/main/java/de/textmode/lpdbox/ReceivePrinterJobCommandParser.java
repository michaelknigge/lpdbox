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

/**
 * The {@link ReceivePrinterJobCommandParser} parses the daemon command "Receive printer job"
 * and sends the response back to the client.
 */
class ReceivePrinterJobCommandParser implements CommandParser {

    final static int COMMAND_CODE_ABORT_JOB = 0x01;
    final static int COMMAND_CODE_RECEIVE_CONTROL_FILE = 0x02;
    final static int COMMAND_CODE_RECEIVE_DATA_FILE = 0x03;

    private final ReceivePrinterJobCommandHandler handler;

    /**
     * Constructor that initializes the {@link ReceivePrinterJobCommandParser} with a
     * non-op {@link ReceivePrinterJobCommandHandler}.
     */
    ReceivePrinterJobCommandParser() {
        this.handler = new ReceivePrinterJobCommandHandler() {
        };
    }

    /**
     * Constructor that initializes the {@link ReceivePrinterJobCommandParser} with the
     * given {@link ReceivePrinterJobCommandHandler}.
     */
    ReceivePrinterJobCommandParser(final ReceivePrinterJobCommandHandler handler) {
        this.handler = handler;
    }

    @Override
    public void parse(final InputStream is, final OutputStream os) throws IOException {
        final String queueName = Util.readLine(is);
        if (queueName.isEmpty()) {
            throw new IOException("No queue name was provided.");
        }

        sendPositiveAcknowledgement(os);

        final PrintJob printJob = new PrintJob(queueName);

        while (true) {
            final int commandCode = is.read();
            if (commandCode == -1) {
                return;
            }

            if (commandCode < COMMAND_CODE_ABORT_JOB || commandCode > COMMAND_CODE_RECEIVE_DATA_FILE) {
                throw new IOException("Peer passed an unknwon second level command code " + Integer.toHexString(
                        commandCode) + " for the command receive printer job");
            }

            if (commandCode == COMMAND_CODE_ABORT_JOB) {
                this.abortJob(printJob);
            } else {
                final String parameterString = Util.readLine(is);
                final String parameters[] = parameterString.split("\\s+");

                if (parameters.length != 2) {
                    throw new IOException("Peer sent inavlid data: " + parameterString);
                }

                final long fileLength = Long.parseLong(parameters[0]);
                final String fileName = parameters[1];

                if (commandCode == COMMAND_CODE_RECEIVE_CONTROL_FILE) {
                    sendPositiveAcknowledgement(os);
                    this.receiveControlFile(is, (int) fileLength, fileName, printJob);
                    sendPositiveAcknowledgement(os);
                } else {
                    sendPositiveAcknowledgement(os);
                    this.receiveDataFile(is, fileLength, fileName, printJob);
                    sendPositiveAcknowledgement(os);
                }
            }
        }
    }

    private void receiveControlFile(final InputStream is, final int fileLength, final String fileName,
            final PrintJob printJob) throws IOException {

        final byte[] buffer = new byte[fileLength];
        final int read = is.read(buffer);

        if (read != fileLength) {
            throw new IOException("Short read on control file (expected " + fileLength + " bytes, but only " + read + " available)");
        }

        if (is.read() != 0x00) {
            throw new IOException("The control file is not followed by a 0x00 termination byte.");
        }

        printJob.setControlFileName(fileName);
        printJob.setControlFileContent(buffer);
    }

    private void receiveDataFile(final InputStream is, final long fileLength, final String fileName,
            final PrintJob printJob) throws IOException {

        printJob.setDataFileName(fileName);
        printJob.setDataFileLength(fileLength);

        this.handler.handle(printJob, is);

        if (is.read() != 0x00) {
            throw new IOException("The data file is not followed by a 0x00 termination byte.");
        }
    }

    private void abortJob(final PrintJob printJob) {
        // TODO: Tell the Handler to delete the stuff....
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
