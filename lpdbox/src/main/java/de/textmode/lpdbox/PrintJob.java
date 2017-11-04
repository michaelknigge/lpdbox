package de.textmode.lpdbox;

import java.util.List;

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

/**
 * Holds information about the print job that is currently handled.
 */
public final class PrintJob {

    private final String queueName;

    private String dataFileName;
    private long dataFileLength;
    private String controlFileName;
    private byte[] controlFileContent;

    /**
     * Constructor.
     */
    public PrintJob(final String queueName) {
        this.queueName = queueName;
    }

    /**
     * Returns the name of the printer queue.
     */
    public String getQueueName() {
        return this.queueName;
    }

    /**
     * Sets the length of the data file.
     */
    void setDataFileLength(final long length) {
        this.dataFileLength = length;
    }

    /**
     * Returns the length of the data file.
     */
    public long getDataFileLength() {
        return this.dataFileLength;
    }

    /**
     * Sets the name of the data file.
     */
    void setDataFileName(final String fileName) {
        this.dataFileName = fileName;
    }

    /**
     * Returns the name of the data file.
     */
    public String getDataFileName() {
        return this.dataFileName;
    }

    /**
     * Sets the name of the control file.
     */
    void setControlFileName(final String fileName) {
        this.controlFileName = fileName;
    }

    /**
     * Returns the name of the control file.
     */
    public String getControlFileName() {
        return this.controlFileName;
    }

    /**
     * Sets the content of the control file.
     */
    void setControlFileContent(final byte[] content) {
        this.controlFileContent = content;
    }

    /**
     * Gets the content of the control file. Returns <code>null</code> if the
     * control file has not been received yet.
     */
    public byte[] getControlFileContent() {
        return this.controlFileContent == null 
                ? null
                : this.controlFileContent.clone();
    }

    /**
     * Gets the content of the control file as a {@link List} of {@link String}s.
     */
    public String[] getControlFileContentAsArray() {
        return new String(this.getControlFileContent(), Util.ISO_8859_1).split("\n");
    }
}
