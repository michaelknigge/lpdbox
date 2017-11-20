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
import java.util.ArrayList;

import org.slf4j.Logger;

/**
 * The {@link RemovePrintJobsCommandParser} parses the daemon command "Remove print jobs"
 * and sends the response back to the client.
 */
final class RemovePrintJobsCommandParser {

    private RemovePrintJobsCommandParser() {
    }

    /**
     * Parses the daemon command "Remove print jobs" and delegates the work to
     * the {@link DaemonCommandHandler}.
     */
    static void parse(
            final Logger logger,
            final DaemonCommandHandler handler,
            final InputStream is,
            final OutputStream os) throws IOException {

        final String parameterString = Util.readLine(is);
        final String[] parameters = parameterString.split("\\s+");

        if (parameters.length < 2) {
            throw new IOException("Client sent inavlid data: " + parameterString);
        }

        final ArrayList<String> jobs = new ArrayList<>(parameters.length);
        for (int ix = 2; ix < parameters.length; ++ix) {
            jobs.add(parameters[ix]);
        }

        handler.removeJobs(parameters[0], parameters[1], jobs);
    }
}
