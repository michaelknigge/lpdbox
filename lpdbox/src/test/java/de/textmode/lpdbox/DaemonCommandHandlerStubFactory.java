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

/**
 * Factory for {@link DaemonCommandHandlerStub}.
 */
final class DaemonCommandHandlerStubFactory implements DaemonCommandHandlerFactory {

    private final DaemonCommandHandlerStub stubHandler = new DaemonCommandHandlerStub();

    @Override
    public DaemonCommandHandler create() {
        return this.stubHandler;
    }

    /**
     * Returns the {@link DaemonCommandHandlerStub} that has been created by this
     * {@link DaemonCommandHandlerStubFactory}.
     */
    DaemonCommandHandlerStub getStubHandler() {
        return this.stubHandler;
    }
}
