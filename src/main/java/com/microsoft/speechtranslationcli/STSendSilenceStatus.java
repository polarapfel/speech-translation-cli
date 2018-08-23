package com.microsoft.speechtranslationcli;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.io.File;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class STSendSilenceStatus implements WriteCallback {

    private final Logger classLogger = LogManager.getLogger(STSendSilenceStatus.class);
    private final STConfiguration configInstance = STConfiguration.getInstance();
    private final ResourceBundle stringsClient = configInstance.getStringsClient();

    private int length;

    public STSendSilenceStatus(int length) {
        this.length = length;
    }

    /**
     * <p>
     * Callback invoked when the write fails.
     * </p>
     *
     * @param x the reason for the write failure
     */
    @Override
    public void writeFailed(Throwable x) {
        classLogger.debug(MessageFormat.format(stringsClient.getString("log4jSCSDebugSilenceWriteFailure"), length), x);
        classLogger.warn(MessageFormat.format(stringsClient.getString("log4jSCSDebugSilenceWriteFailure"), length));
        //System.exit(STExitCode.UPLOAD_ERROR.getId());
    }

    /**
     * <p>
     * Callback invoked when the write completes.
     * </p>
     *
     * @see #writeFailed(Throwable)
     */
    @Override
    public void writeSuccess() {
        classLogger.trace(MessageFormat.format(stringsClient.getString("log4jSCSDebugSilenceWriteSuccess"), length));
    }
}
