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
import java.util.List;

/**
 * The {@link DaemonCommandHandler} provides methods that handle the operations
 * requested from the LPD clients.
 */
public interface DaemonCommandHandler {

    /**
     * Handles the daemon command "Print any waiting jobs".
     */
    void printJobs(final String queueName) throws IOException;

    /**
     * Gets called when the LPD-Server received the daemon command "Receive printer job".
     * Returns <code>false</code> if the LPD-Server should refuse the printer job (i. e.
     * the queue is currently stopped).
     */
    boolean startPrinterJob(final String queueName) throws IOException;

    /**
     * Handles the subcommand "Receive control file" of the daemon command "Receive printer job".
     * The handler <b>MUST</b> read the control file completely from the {@link InputStream} or
     * throw an {@link IOException} to let the underlying connection be closed.
     */
    boolean receiveControlFile(final InputStream is, final int fileLength, final String fileName) throws IOException;

    /**
     * Handles the subcommand "Receive data file" of the daemon command "Receive printer job".
     * The handler <b>MUST</b> read the data file completely from the {@link InputStream} or
     * throw an {@link IOException} to let the underlying connection be closed.
     */
    boolean receiveDataFile(final InputStream is, final long fileLength, final String fileName) throws IOException;

    /**
     * Handles the subcommand "Abort job" of the daemon command "Receive printer job". The handler should delete
     * all files which have been created during this "Receive printer job" command.
     */
    void abortPrinterJob() throws IOException;

    /**
     * Gets called when the client has closed the connection. The application is responsible to
     * check if the control file and data file have been received. If one of them is missing,
     * the application should delete the received files.
     */
    void endPrinterJob() throws IOException;

    /**
     * Handles the daemon command "Remove jobs".
     */
    void removeJobs(final String queueName, final String agent, final List<String> jobs) throws IOException;

    /**
     * Handles the daemon command "Send queue state (long)". Returns an textual description
     * of the print queue with the given name. If the List is empty, all jobs are
     * returned. Note that every line of the textual description must end with an line feed.
     */
    String sendQueueStateLong(final String queueName, final List<String> jobs) throws IOException;

    /**
     * Handles the daemon command "Send queue state (short)". Returns an textual description
     * of the print queue with the given name. If the List is empty, all jobs are
     * returned. Note that every line of the textual description must end with an line feed.
     */
    String sendQueueStateShort(final String queueName, final List<String> jobs) throws IOException;
}
