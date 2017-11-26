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
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

/**
 * The {@link LinePrinterDaemon} implements a RFC1179 compliant line printer daemon.
 * It implements {@link Runnable} so it can be run without a wrapper class
 * within a thread.
 */
public final class LinePrinterDaemon implements Runnable {

    private final int portNumber;
    private final Logger logger;
    private final DaemonCommandHandlerFactory factory;
    private final ExecutorService executorService;

    private volatile ServerSocket serverSocket;
    private volatile boolean isRunning;
    private volatile boolean isShutdownRequested;

    /**
     * Constructor. Use the {@link de.textmode.lpdbox.LinePrinterDaemonBuilder} to
     * build the {@link LinePrinterDaemon}.
     */
    LinePrinterDaemon(
            final int portNumber,
            final int maxThreads,
            final DaemonCommandHandlerFactory factory,
            final Logger logger) {

        this.portNumber = portNumber;
        this.factory = factory;
        this.logger = logger;

        this.serverSocket = null;
        this.isRunning = false;
        this.isShutdownRequested = false;

        this.executorService = Executors.newFixedThreadPool(maxThreads, new ThreadFactory() {

            @Override
            public Thread newThread(final Runnable r) {
                final Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setDaemon(true);
                thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(final Thread t, final Throwable e) {
                        LinePrinterDaemon.this.logger.error(
                                "An unhandled error occurred in thread "
                                        + t.getName()
                                        + ": "
                                        + e.getMessage());
                    }
                });

                return thread;
            }
        });
    }

    /**
     * Opens the {@link ServerSocket} but do not wait and/or accept incoming connections.
     * This method can be used to check if the server can be started (i. e. check if the
     * port number is available).
     */
    public void startup() throws IOException {
        if (this.serverSocket == null) {
            this.serverSocket = new ServerSocket(this.portNumber);
            this.serverSocket.setReuseAddress(true);

            this.logger.info("Line Printer Daemon initialized (listening on port " + this.portNumber + ")");
        }
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
        } catch (final Throwable e) {
            if (!this.isShutdownRequested) {
                this.logger.error(e.getMessage());
            }
        }

        this.isRunning = false;

        if (this.serverSocket != null && !this.serverSocket.isClosed()) {
            Util.closeQuietly(this.serverSocket);
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

            this.logger.info("Accepted connection from " + Util.getClientString(connection));
            this.handleConnection(connection);
        }
    }

    /**
     * Handles a connection from a client. The connection is handled in a separate thread.
     */
    private void handleConnection(final Socket connection) {
        this.executorService.execute(new LinePrinterDaemonConnectionHandler(
                this.logger,
                connection,
                this.factory));
    }

    /**
     * Stops the {@link LinePrinterDaemon}. Note that this method will not wait until the
     * {@link LinePrinterDaemon} has been stopped. It will also not wait until all threads (that
     * handle client connections) are finished.
     */
    public void stop() {
        this.logger.info("Stopping line printer daemon");

        this.isShutdownRequested = true;
        this.executorService.shutdown();

        // Closing the server socket causes an exception on the
        // ServerSocket.accept() method.... And this let's the server end...
        Util.closeQuietly(this.serverSocket);
    }

    /**
     * Stops the {@link LinePrinterDaemon}. This method waits up to the given milliseconds until
     * the {@link LinePrinterDaemon} has been stopped. If the {@link LinePrinterDaemon} has been
     * ended within that given timeout, <code>true</code> is returned, otherwise <code>false</code>.
     */
    public boolean stop(final long timeoutInMillis) throws InterruptedException {
        this.stop();

        for (int ix = 0; ix < timeoutInMillis; ix += 100) {
            if (!this.isRunning) {
                this.logger.info("Line printer daemon stopped");
                this.executorService.awaitTermination(timeoutInMillis, TimeUnit.MILLISECONDS);
                return true;
            }
            Thread.sleep(100);
        }

        this.logger.info("The line printer daemon is still alive (refuses to stop)");
        return false;
    }
}
