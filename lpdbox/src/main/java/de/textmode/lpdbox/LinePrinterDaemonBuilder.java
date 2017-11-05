package de.textmode.lpdbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for a {@link LinePrinterDaemon}. Every Command-Handler that is not set
 * is internally set to a non-op Command-Handler.
 */
public final class LinePrinterDaemonBuilder {

    private static final int DEFAULT_PORT_NUMBER = 515;

    private int portNumber;
    private Logger logger;
    private final CommandParser[] parsers;

    /**
     * Constructor of the {@link LinePrinterDaemonBuilder}.
     */
    public LinePrinterDaemonBuilder() {
        this.portNumber = DEFAULT_PORT_NUMBER;
        this.parsers = new CommandParser[6];
        this.logger = LoggerFactory.getLogger(LinePrinterDaemonBuilder.class);

        this.parsers[LinePrinterDaemon.COMMAND_CODE_PRINT_JOBS] = new PrintJobsCommandParser();
        this.parsers[LinePrinterDaemon.COMMAND_CODE_RECEIVE_PRINTER_JOB] = new ReceivePrinterJobCommandParser();
        this.parsers[LinePrinterDaemon.COMMAND_CODE_REPORT_QUEUE_STATE_SHORT] = new ReportQueueStateShortCommandParser();
        this.parsers[LinePrinterDaemon.COMMAND_CODE_REPORT_QUEUE_STATE_LONG] = new ReportQueueStateLongCommandParser();
        this.parsers[LinePrinterDaemon.COMMAND_CODE_REMOVE_PRINT_JOBS] = new RemovePrintJobsCommandParser();
    }

    /**
     * Sets the {@link PrintJobsCommandHandler}.
     */
    LinePrinterDaemonBuilder logger(final Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Sets the {@link PrintJobsCommandHandler}.
     */
    LinePrinterDaemonBuilder portNumber(final int portNumber) {
        this.portNumber = portNumber;
        return this;
    }

    /**
     * Sets the {@link PrintJobsCommandHandler}.
     */
    LinePrinterDaemonBuilder printJobsCommandHandler(final PrintJobsCommandHandler handler) {
        this.parsers[LinePrinterDaemon.COMMAND_CODE_PRINT_JOBS] = new PrintJobsCommandParser(handler);
        return this;
    }

    /**
     * Sets the {@link ReceivePrinterJobCommandHandler}.
     */
    LinePrinterDaemonBuilder receiveJobCommandHandler(final ReceivePrinterJobCommandHandler handler) {
        this.parsers[LinePrinterDaemon.COMMAND_CODE_RECEIVE_PRINTER_JOB] = new ReceivePrinterJobCommandParser(handler);
        return this;
    }

    /**
     * Sets the {@link ReportQueueStateShortCommandHandler}.
     */
    LinePrinterDaemonBuilder reportQueueStateShortCommandHandler(final ReportQueueStateShortCommandHandler handler) {
        this.parsers[LinePrinterDaemon.COMMAND_CODE_REPORT_QUEUE_STATE_SHORT] = new ReportQueueStateShortCommandParser(handler);
        return this;
    }

    /**
     * Sets the {@link ReportQueueStateLongCommandHandler}.
     */
    LinePrinterDaemonBuilder reportQueueStateLongCommandHandler(final ReportQueueStateLongCommandHandler handler) {
        this.parsers[LinePrinterDaemon.COMMAND_CODE_REPORT_QUEUE_STATE_LONG] = new ReportQueueStateLongCommandParser(handler);
        return this;
    }

    /**
     * Sets the {@link RemovePrintJobsCommandHandler}.
     */
    LinePrinterDaemonBuilder removePrintJobCommandHandler(final RemovePrintJobsCommandHandler handler) {
        this.parsers[LinePrinterDaemon.COMMAND_CODE_REMOVE_PRINT_JOBS] = new RemovePrintJobsCommandParser(handler);
        return this;
    }

    /**
     * Builds the {@link LinePrinterDaemon}.
     */
    LinePrinterDaemon build() {
        return new LinePrinterDaemon(this.portNumber, this.parsers, this.logger);
    }
}
