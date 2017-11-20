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

import org.slf4j.Logger;

/**
 * The {@link LinePrinterDaemonConnectionHandler} handles the connection from a client.
 * It implements the {@link Runnable}-Interface so it can be ran within a thread.
 */
final class LinePrinterDaemonConnectionHandler implements Runnable {

    private static final int COMMAND_CODE_PRINT_JOBS = 0x01;
    private static final int COMMAND_CODE_RECEIVE_PRINTER_JOB = 0x02;
    private static final int COMMAND_CODE_REPORT_QUEUE_STATE_SHORT = 0x03;
    private static final int COMMAND_CODE_REPORT_QUEUE_STATE_LONG = 0x04;
    private static final int COMMAND_CODE_REMOVE_PRINT_JOBS = 0x05;

    private final Logger logger;
    private final Socket connection;
    private final DaemonCommandHandlerFactory factory;

    /**
     * Constructor.
     */
    LinePrinterDaemonConnectionHandler(
            final Logger logger,
            final Socket connection,
            final DaemonCommandHandlerFactory factory) {

        this.logger = logger;
        this.connection = connection;
        this.factory = factory;
    }

    /**
     * Handles the connection from the client.
     */
    @Override
    public void run() {
        final String client = Util.getClientString(this.connection);
        try {
            this.logger.debug("Handling connection from " + client);
            this.handleConnection();
            this.logger.debug("Handled connection from " + client + " successfully");
        } catch (final Throwable e) {
            this.logger.error("An error occurred while handling connection from " + client + ": " + e.getMessage());
        } finally {
            Util.closeQuietly(this.connection);
        }
    }

    private void handleConnection() throws IOException {

        final String client = Util.getClientString(this.connection);

        // Read the first byte - it's value is used to determine the CommandParser that is
        // responsible to parse the incoming data.
        final InputStream is = this.connection.getInputStream();
        final OutputStream os = this.connection.getOutputStream();

        final int commandCode = is.read();
        if (commandCode == -1) {
            this.logger.error("Connection from " + client + " is down");
            return;
        }

        final DaemonCommandHandler handler = this.factory.create();
        if (handler == null) {
            this.logger.error("A daemon command handler could not be created");
            return;
        }

        this.logger.debug("Client " + client + " sent command code " + Integer.toHexString(commandCode));

        switch (commandCode) {
        case COMMAND_CODE_PRINT_JOBS:
            PrintJobsCommandParser.parse(this.logger, handler, is, os);
            break;

        case COMMAND_CODE_RECEIVE_PRINTER_JOB:
            ReceivePrinterJobCommandParser.parse(this.logger, handler, is, os);
            break;

        case COMMAND_CODE_REPORT_QUEUE_STATE_SHORT:
            ReportQueueStateShortCommandParser.parse(this.logger, handler, is, os);
            break;

        case COMMAND_CODE_REPORT_QUEUE_STATE_LONG:
            ReportQueueStateLongCommandParser.parse(this.logger, handler, is, os);
            break;

        case COMMAND_CODE_REMOVE_PRINT_JOBS:
            RemovePrintJobsCommandParser.parse(this.logger, handler, is, os);
            break;

        default:
            this.logger.error(
                    "Client " + client + " passed an unknwon command code " + Integer.toHexString(commandCode));
            break;
        }
    }
}
