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
import java.util.List;

import junit.framework.TestCase;

/**
 * Unit-Tests of class {@link ReportQueueStateShortCommandParser}.
 */
public final class ReportQueueStateShortCommandParserTest extends TestCase {

    private final static String TEXT = "Dummy text from ReportQueueStateShortCommandParserTest\n";

    private static class ReportQueueStateShortCommandHandlerStub implements ReportQueueStateShortCommandHandler {
        private String queueName;
        private List<String> jobs;

        @Override
        public String handle(final String queueName, final List<String> jobs) {
            this.queueName = queueName;
            this.jobs = jobs;

            return TEXT;
        }
    };

    /**
     * Performs the test and returns a {@link ReportQueueStateShortCommandHandlerStub} for checking
     * the results.
     */
    private static ReportQueueStateShortCommandHandlerStub performTest(final String input) throws Exception {
        final ReportQueueStateShortCommandHandlerStub handler = new ReportQueueStateShortCommandHandlerStub();
        final ReportQueueStateShortCommandParser parser = new ReportQueueStateShortCommandParser(handler);

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(input.getBytes("ISO-8859-1"));

        final ByteArrayInputStream is = new ByteArrayInputStream(data.toByteArray());
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        parser.parse(is, os);

        assertEquals(TEXT, os.toString("ISO-8859-1"));

        return handler;
    }

    /**
     * Without a list of jobs.
     */
    public void testNoList() throws Exception {
        final ReportQueueStateShortCommandHandlerStub handler = performTest("my_queue\n");
        assertEquals("my_queue", handler.queueName);
        assertTrue(handler.jobs.isEmpty());
    }

    /**
     * With a list of jobs.
     */
    public void testWithList() throws Exception {
        final ReportQueueStateShortCommandHandlerStub handler = performTest("queue_abc 1 x abc\n");
        assertEquals("queue_abc", handler.queueName);
        assertEquals("1", handler.jobs.get(0));
        assertEquals("x", handler.jobs.get(1));
        assertEquals("abc", handler.jobs.get(2));
    }

    /**
     * Without an queue name.
     */
    public void testWithoutQueue() throws Exception {
        try {
            performTest("\n");
            fail();
        } catch (final IOException e) {
            assertEquals("No queue name was provided.", e.getMessage());
        }
    }

}
