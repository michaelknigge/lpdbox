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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Unit-Tests of class {@link PrintJobsCommandParser}.
 */
public final class PrintJobsCommandParserTest extends TestCase {

    private static class PrintJobsCommandHandlerStub implements PrintJobsCommandHandler {
        private String queueName;

        @Override
        public void handle(final String queueName) {
            this.queueName = queueName;
        }
    };

    /**
     * Checks the happy flow.
     */
    public void testHappyFlow() throws Exception {

        final PrintJobsCommandHandlerStub handler = new PrintJobsCommandHandlerStub();
        final PrintJobsCommandParser parser = new PrintJobsCommandParser(handler);

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write("MY_QUEUE_NAME\n".getBytes("ISO-8859-1"));

        final ByteArrayInputStream is = new ByteArrayInputStream(data.toByteArray());
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        parser.parse(is, os);

        assertEquals("MY_QUEUE_NAME", handler.queueName);
        assertEquals(0, os.size());
    }

    /**
     * Missing queue name.
     */
    public void testMissingQueueName() throws Exception {

        final PrintJobsCommandHandlerStub handler = new PrintJobsCommandHandlerStub();
        final PrintJobsCommandParser parser = new PrintJobsCommandParser(handler);

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write("\n".getBytes("ISO-8859-1"));

        final ByteArrayInputStream is = new ByteArrayInputStream(data.toByteArray());
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            parser.parse(is, os);
            fail();
        } catch (final IOException e) {
            assertEquals("No queue name was provided.", e.getMessage());
        }
    }

    /**
     * Missing line feed.
     */
    public void testMissingLineFeed() throws Exception {

        final PrintJobsCommandHandlerStub handler = new PrintJobsCommandHandlerStub();
        final PrintJobsCommandParser parser = new PrintJobsCommandParser(handler);

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write("FOO".getBytes("ISO-8859-1"));

        final ByteArrayInputStream is = new ByteArrayInputStream(data.toByteArray());
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            parser.parse(is, os);
            fail();
        } catch (final IOException e) {
            assertEquals(Util.ERROR_END_OF_STREAM, e.getMessage());
        }
    }
}
