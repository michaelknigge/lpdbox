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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;

/**
 * The {@link LinePrinterDaemon} implements a RFC1179 compliant line printer daemon.
 * It implements the {@link Runnable} so it can be run without a wrapper class as
 * within a thread.
 */
public final class LinePrinterDaemon implements Runnable {

    final static int COMMAND_CODE_PRINT_JOBS = 0x01;
    final static int COMMAND_CODE_RECEIVE_PRINTER_JOB = 0x02;
    final static int COMMAND_CODE_REPORT_QUEUE_STATE_SHORT = 0x03;
    final static int COMMAND_CODE_REPORT_QUEUE_STATE_LONG = 0x04;
    final static int COMMAND_CODE_REMOVE_PRINT_JOBS = 0x05;

    private final int portNumber;
    private final Logger logger;
    private final CommandParser[] parsers;

    private volatile ServerSocket serverSocket;
    private volatile boolean isRunning;
    private volatile boolean isShutdownRequested;

    /**
     * Constructor. Use the {@link de.textmode.lpdbox.LinePrinterDaemonBuilder} to
     * build the {@link LinePrinterDaemon}.
     */
    LinePrinterDaemon(final int portNumber, final CommandParser[] parsers, final Logger logger) {

        this.portNumber = portNumber;
        this.parsers = parsers;
        this.logger = logger;

        this.serverSocket = null;
        this.isRunning = false;
        this.isShutdownRequested = false;
    }

    /**
     * Opens the {@link ServerSocket} but do not wait and/or accept incoming connections.
     * This method can be used to check if the server can be started (i. e. check if the
     * port number is available).
     */
    public void startup() throws IOException {
        this.serverSocket = new ServerSocket(this.portNumber);
        this.logger.info("Line Printer Daemon initialized (listening on port " + this.portNumber + ")");
    }

    /**
     * Returns <code>true</code> if the server is up and running.
     */
    public boolean isRunning() {
        return this.serverSocket != null && this.isRunning;
    }

    /**
     * Starts the server. If {@link #startup()} has not been called, {@link #startup()}
     * is called implicit.
     */
    @Override
    public void run() {

        try {
            if (this.serverSocket == null) {
                this.startup();
            }
            this.handleConnections();
        } catch (final IOException e) {
            if (!this.isShutdownRequested) {
                this.logger.error(e.getMessage());
            }
        }

        this.isRunning = false;

        if (this.serverSocket != null && !this.serverSocket.isClosed()) {
            this.closeSocketQuietly(this.serverSocket);
        }
    }

    /**
     * Closes the {@link ServerSocket} quietly (means, all errors are catched and forgotten).
     */
    private void closeSocketQuietly(final Closeable closeable) {
        try {
            closeable.close();
        } catch (final Throwable e) {
        }
    }

    /**
     * Accepts connections from clients and handles them.
     */
    private void handleConnections() throws IOException {
        this.isRunning = true;
        
        while (!this.isShutdownRequested) {
            this.logger.debug("Waiting for incoming connection");
            final Socket connection = this.serverSocket.accept();

            // TODO: We should handle it in a separate Thread... maybe a ThreadPool...
            final String peer = connection.getInetAddress().toString();
            this.logger.info("Accepted connection from " + peer);

            try {
                this.handleConnection(connection);
            } catch (final IOException e) {
                this.logger.error("An error occurred while handling connection from " + peer + ": " + e.getMessage());
            } finally {
                this.closeSocketQuietly(connection);
            }
        }
    }

    /**
     * Handles a connection from a client.
     */
    private void handleConnection(final Socket connection) throws IOException {
        final String peer = connection.getInetAddress().toString();

        // Read the first byte - it's value is used to determine the CommandParser that is
        // responsible to parse the incoming data.
        final InputStream is = connection.getInputStream();
        final OutputStream os = connection.getOutputStream();

        final int commandCode = is.read();
        if (commandCode == -1) {
            this.logger.error("Connection from " + peer + " is down");
            return;
        }

        if (commandCode < COMMAND_CODE_PRINT_JOBS || commandCode > COMMAND_CODE_REMOVE_PRINT_JOBS) {
            this.logger.error("Peer " + peer + " passed an unknwon command code " + Integer.toHexString(commandCode));
            return;
        }

        final CommandParser parser = this.parsers[commandCode];
        if (parser == null) {
            this.logger.error(
                    "Unable to parse command code " + Integer.toHexString(commandCode) + " due to an internal error");
            return;
        }

        parser.parse(is, os);
    }

    /**
     * Stops the {@link LinePrinterDaemon}. Note that this method will not wait until the
     * {@link LinePrinterDaemon} has been stopped.
     */
    public void stop() {
        this.closeSocketQuietly(this.serverSocket);
        this.isShutdownRequested = true;

        // TODO: maybe we should also kill the client sockets so the server can *really* quit!
        // or pass a "ServerState" to the Handlers so they can check if they should stop
        // working...
    }
    
    /**
     * Stops the {@link LinePrinterDaemon}. This method waits up to the given milliseconds until
     * the {@link LinePrinterDaemon} has been stopped. If the {@link LinePrinterDaemon} has been
     * ended within that given timeout, <code>true</code> is returned, otherwise <code>false</code>.
     */
    public boolean stop(final long timeoutInMillis) throws InterruptedException {
        this.closeSocketQuietly(this.serverSocket);
        this.isShutdownRequested = true;
        
        for (int ix=0; ix<timeoutInMillis; ix+=100) {
            if (!this.isRunning) {
                return true;
            }
            Thread.sleep(100);
        }
        
        return false;
    }
}
