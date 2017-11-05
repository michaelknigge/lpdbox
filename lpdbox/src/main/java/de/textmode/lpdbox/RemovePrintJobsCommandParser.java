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

/**
 * The {@link RemovePrintJobsCommandParser} parses the daemon command "Remove print jobs"
 * and sends the response back to the client.
 */
class RemovePrintJobsCommandParser implements CommandParser {

    private final RemovePrintJobsCommandHandler handler;

    /**
     * Constructor that initializes the {@link RemovePrintJobsCommandParser} with a
     * non-op {@link RemovePrintJobsCommandHandler}.
     */
    RemovePrintJobsCommandParser() {
        this.handler = new RemovePrintJobsCommandHandler() {
        };
    }

    /**
     * Constructor that initializes the {@link RemovePrintJobsCommandParser} with the
     * given {@link RemovePrintJobsCommandHandler}.
     */
    RemovePrintJobsCommandParser(final RemovePrintJobsCommandHandler handler) {
        this.handler = handler;
    }

    @Override
    public void parse(final InputStream is, final OutputStream os) throws IOException {
        final String parameterString = Util.readLine(is);
        final String parameters[] = parameterString.split("\\s+");

        if (parameters.length < 2) {
            throw new IOException("Peer sent inavlid data: " + parameterString);
        }

        final ArrayList<String> jobs = new ArrayList<>(parameters.length);
        for (int ix = 2; ix < parameters.length; ++ix) {
            jobs.add(parameters[ix]);
        }

        this.handler.handle(parameters[0], parameters[1], jobs);
    }
}
