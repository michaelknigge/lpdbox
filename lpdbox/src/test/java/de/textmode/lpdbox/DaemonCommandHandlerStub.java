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
import java.util.Collections;
import java.util.List;

/**
 * Stub implementation of a {@link DaemonCommandHandler} for unit tests.
 */
final class DaemonCommandHandlerStub implements DaemonCommandHandler {

    private volatile String printerQueueName;

    private volatile String controlFileName;
    private volatile int controlFileLength;
    private volatile byte[] controlFileContent;

    private volatile String dataFileName;
    private volatile long dataFileLength;
    private volatile byte[] dataFileContent;

    private volatile String userName;
    private volatile List<String> jobList;

    private volatile boolean lockedQueue;


    /**
     * Constructor.
     */
    DaemonCommandHandlerStub() {
        this.dataFileLength = -1;
        this.controlFileLength = -1;
        this.jobList = Collections.emptyList();
    }

    /**
     * Locks the printer queue (a "start printer job" will send a negative acknowledge
     * to the peer).
     */
    void lockQueue() {
        this.lockedQueue = true;
    }

    /**
     * Unlocks the printer queue.
     */
    void unlockQueue() {
        this.lockedQueue = false;
    }

    /**
     * Returns the last set printer queue name.
     */
    String getPrinterQueueName() {
        return this.printerQueueName;
    }

    /**
     * Returns the name of the control file (if already set, <code>null</code> otherwise).
     */
    String getControlFileName() {
        return this.controlFileName;
    }

    /**
     * Returns the length of the control file (if already set, -1 otherwise).
     */
    int getControlFileLength() {
        return this.controlFileLength;
    }

    /**
     * Returns the content of the control file (if already set, <code>null</code> otherwise).
     */
    byte[] getControlFileContent() {
        return this.controlFileContent;
    }

    /**
     * Returns the content of the control file as a {@link List} of {@link String}s.
     */
    String[] getControlFileContentAsArray() {
        return new String(this.getControlFileContent(), Util.ISO_8859_1).split("\n");
    }

    /**
     * Returns the name of the data file (if already set, <code>null</code> otherwise).
     */
    String getDataFileName() {
        return this.dataFileName;
    }

    /**
     * Returns the length of the data file (if already set, -1 otherwise).
     */
    long getDataFileLength() {
        return this.dataFileLength;
    }

    /**
     * Returns the content of the data file (if already set, <code>null</code> otherwise).
     */
    byte[] getDataFileContent() {
        return this.dataFileContent;
    }

    /**
     * Returns the the name of the user that made the last "Send queue state"
     * request (if known, <code>null</code> otherwise).
     */
    String getUserName() {
        return this.userName;
    }

    /**
     * Returns a {@link List} of job names that have been received with the last "Send queue state"
     * request (if known, <code>null</code> otherwise).
     */
    List<String> getJobs() {
        return this.jobList;
    }

    @Override
    public void printJobs(final String queueName) throws IOException {
        this.printerQueueName = queueName;
    }

    @Override
    public boolean startPrinterJob(final String queueName) throws IOException {
        if (this.lockedQueue) {
            return false;
        }
        this.printerQueueName = queueName;
        return true;
    }

    @Override
    public boolean receiveControlFile(final InputStream is, final int length, final String name) throws IOException {
        this.controlFileName = name;
        this.controlFileLength = length;

        this.controlFileContent = new byte[length];
        return is.read(this.controlFileContent) == length;
    }

    @Override
    public boolean receiveDataFile(final InputStream is, final long length, final String name) throws IOException {
        this.dataFileName = name;
        this.dataFileLength = length;

        this.dataFileContent = new byte[(int) length];
        return is.read(this.dataFileContent) == length;
    }

    @Override
    public void abortPrinterJob() throws IOException {
    }

    @Override
    public void endPrinterJob() throws IOException {
    }

    @Override
    public void removeJobs(final String queueName, final String agent, final List<String> jobs) throws IOException {
        this.printerQueueName = queueName;
        this.userName = agent;
        this.jobList = jobs;
    }

    @Override
    public String sendQueueStateLong(final String queueName, final List<String> jobs) throws IOException {
        this.printerQueueName = queueName;
        this.jobList = jobs;

        return "this is a long list";
    }

    @Override
    public String sendQueueStateShort(final String queueName, final List<String> jobs) throws IOException {
        this.printerQueueName = queueName;
        this.jobList = jobs;

        return "this is a short list";
    }
}
