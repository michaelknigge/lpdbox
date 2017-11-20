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
 * The {@link ReportQueueStateLongCommandParser} parses the daemon command "Send queue state (long)"
 * and sends the response back to the client.
 */
final class ReportQueueStateLongCommandParser {

    private ReportQueueStateLongCommandParser() {
    }

    /**
     * Parses the daemon command "Send queue state (long)" and delegates the work to
     * the {@link DaemonCommandHandler}.
     */
    static void parse(
            final Logger logger,
            final DaemonCommandHandler handler,
            final InputStream is,
            final OutputStream os) throws IOException {

        final String parameterString = Util.readLine(is);
        if (parameterString.isEmpty()) {
            throw new IOException("No queue name was provided by the client");
        }

        final String[] parameters = parameterString.split("\\s+");
        final ArrayList<String> jobs = new ArrayList<>(parameters.length - 1);
        for (int ix = 1; ix < parameters.length; ++ix) {
            jobs.add(parameters[ix]);
        }

        Util.writeString(handler.sendQueueStateLong(parameters[0], jobs), os);
    }

}
