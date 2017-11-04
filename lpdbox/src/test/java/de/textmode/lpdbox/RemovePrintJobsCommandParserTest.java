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
 * Unit-Tests of class {@link RemovePrintJobsCommandParser}.
 */
public final class RemovePrintJobsCommandParserTest extends TestCase {

    private static class RemovePrintJobsCommandHandlerStub implements RemovePrintJobsCommandHandler {
        private String queueName;
        private String agent;
        private List<String> jobs;

        @Override
        public void handle(final String queueName, final String agent, final List<String> jobs) {
            this.queueName = queueName;
            this.agent = agent;
            this.jobs = jobs;
        }
    };

    /**
     * Performs the test and returns a {@link RemovePrintJobsCommandHandlerStub} for checking
     * the results.
     */
    private static RemovePrintJobsCommandHandlerStub performTest(final String input) throws Exception {
        final RemovePrintJobsCommandHandlerStub handler = new RemovePrintJobsCommandHandlerStub();
        final RemovePrintJobsCommandParser parser = new RemovePrintJobsCommandParser(handler);

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(input.getBytes("ISO-8859-1"));

        final ByteArrayInputStream is = new ByteArrayInputStream(data.toByteArray());
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        parser.parse(is, os);

        assertEquals(0, os.size());

        return handler;
    }

    /**
     * Without a list of jobs.
     */
    public void testNoList() throws Exception {
        final RemovePrintJobsCommandHandlerStub handler = performTest("my_queue root\n");
        assertEquals("my_queue", handler.queueName);
        assertEquals("root", handler.agent);
        assertTrue(handler.jobs.isEmpty());
    }

    /**
     * With a list of jobs.
     */
    public void testWithList() throws Exception {
        final RemovePrintJobsCommandHandlerStub handler = performTest("queue_abc foo 1 x abc\n");
        assertEquals("queue_abc", handler.queueName);
        assertEquals("foo", handler.agent);
        assertEquals("1", handler.jobs.get(0));
        assertEquals("x", handler.jobs.get(1));
        assertEquals("abc", handler.jobs.get(2));
    }

    /**
     * Without an agent.
     */
    public void testWithoutAgent() throws Exception {
        try {
            performTest("queue_abc\n");
            fail();
        } catch (final IOException e) {
            assertEquals("Peer sent inavlid data: queue_abc", e.getMessage());

        }
    }

    /**
     * Without an queue name.
     */
    public void testWithoutQueue() throws Exception {
        try {
            performTest("\n");
            fail();
        } catch (final IOException e) {
            assertEquals("Peer sent inavlid data: ", e.getMessage());
        }
    }
}