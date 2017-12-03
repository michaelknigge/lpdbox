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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for a {@link LinePrinterDaemon}.
 */
public final class LinePrinterDaemonBuilder {

    private static final int DEFAULT_PORT_NUMBER = 515;
    private static final int DEFAULT_MAX_THREADS = 10;

    private final DaemonCommandHandlerFactory factory;

    private int portNumber;
    private int maxThreads;
    private Logger logger;

    /**
     * Constructor of the {@link LinePrinterDaemonBuilder}.
     */
    public LinePrinterDaemonBuilder(final DaemonCommandHandlerFactory factory) {
        this.factory = factory;

        this.portNumber = DEFAULT_PORT_NUMBER;
        this.maxThreads = DEFAULT_MAX_THREADS;

        this.logger = LoggerFactory.getLogger(LinePrinterDaemon.class);
    }

    /**
     * Sets the {@link Logger}.
     */
    public LinePrinterDaemonBuilder logger(final Logger value) {
        this.logger = value;
        return this;
    }

    /**
     * Sets the port number on which the {@link LinePrinterDaemon} should listen.
     */
    public LinePrinterDaemonBuilder portNumber(final int value) {
        this.portNumber = value;
        return this;
    }

    /**
     * Sets the maximum number of threads the {@link LinePrinterDaemon} should start.
     */
    public LinePrinterDaemonBuilder maxThreads(final int value) {
        this.maxThreads = value;
        return this;
    }

    /**
     * Builds the {@link LinePrinterDaemon}.
     */
    public LinePrinterDaemon build() {
        return new LinePrinterDaemon(this.portNumber, this.maxThreads, this.factory, this.logger);
    }
}
