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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

/**
 * Unit-Tests of class {@link ReportQueueStateLongCommandParser}.
 */
public final class ReportQueueStateLongCommandParserTest extends TestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportQueueStateLongCommandParserTest.class);

    /**
     * Performs the test and returns a {@link DaemonCommandHandlerStub} for checking
     * the results.
     */
    private static DaemonCommandHandlerStub performTest(final String input) throws Exception {
        final DaemonCommandHandlerStub handler = new DaemonCommandHandlerStub();
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(input.getBytes("ISO-8859-1"));

        final ByteArrayInputStream is = new ByteArrayInputStream(data.toByteArray());
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        new ReportQueueStateLongCommandParser(LOGGER, handler).parse(is, os);

        assertEquals("this is a long list", os.toString("ISO-8859-1"));

        return handler;
    }

    /**
     * Without a list of jobs.
     */
    public void testNoList() throws Exception {
        final DaemonCommandHandlerStub handler = performTest("my_queue\n");
        assertEquals("my_queue", handler.getPrinterQueueName());
        assertTrue(handler.getJobs().isEmpty());
    }

    /**
     * With a list of jobs.
     */
    public void testWithList() throws Exception {
        final DaemonCommandHandlerStub handler = performTest("queue_abc 1 x abc\n");
        assertEquals("queue_abc", handler.getPrinterQueueName());
        assertEquals("1", handler.getJobs().get(0));
        assertEquals("x", handler.getJobs().get(1));
        assertEquals("abc", handler.getJobs().get(2));
    }

    /**
     * Without an queue name.
     */
    public void testWithoutQueue() throws Exception {
        try {
            performTest("\n");
            fail();
        } catch (final IOException e) {
            assertEquals("No queue name was provided by the client", e.getMessage());
        }
    }

    /**
     * Without an queue name (just spaces).
     */
    public void testInvalidQueueName() throws Exception {
        try {
            performTest("   \n");
            fail();
        } catch (final IOException e) {
            assertEquals("No queue name was provided by the client", e.getMessage());
        }
    }
}
