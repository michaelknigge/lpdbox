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
 * The {@link CommandParser} provides the concrete implementations with
 * the {@link Logger} and {@link DaemonCommandHandler} to use.
 */
abstract class CommandParser {

    private final Logger logger;
    private final DaemonCommandHandler handler;

    /**
     * Constructor.
     */
    CommandParser(final Logger logger, final DaemonCommandHandler handler) {
        this.logger = logger;
        this.handler = handler;
    }

    /**
     * Reads the command from the {@link InputStream}, processes it and writes the result
     * (if any) to the {@link OutputStream}.
     */
    abstract void parse(final InputStream is, final OutputStream os) throws IOException;

    /**
     * Returns the {@link Logger} to be used.
     */
    Logger getLogger() {
        return this.logger;
    }

    /**
     * Returns the {@link DaemonCommandHandler} to be used.
     */
    DaemonCommandHandler getDaemonCommandHandler() {
        return this.handler;
    }

    /**
     * Reads the queue name from the {@link InputStream}.
     */
    String getQueueName(final InputStream is) throws IOException {
        final String queueName = Util.readLine(is);
        if (queueName.isEmpty()) {
            throw new IOException("No queue name was provided by the client");
        }
        return queueName;
    }
}
