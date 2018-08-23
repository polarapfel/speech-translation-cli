package com.microsoft.speechtranslationcli;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.io.File;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class STSendChunkStatus implements WriteCallback {

    private final Logger classLogger = LogManager.getLogger(STSendChunkStatus.class);
    private final STConfiguration configInstance = STConfiguration.getInstance();
    private final ResourceBundle stringsClient = configInstance.getStringsClient();

    private final File file;
    private int chunk;
    private int total;

    public STSendChunkStatus(File f, int chunk, int total) {
        file = f;
        this.chunk = chunk;
        this.total = total;
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
        classLogger.debug(MessageFormat.format(stringsClient.getString("log4jSCSDebugSendingFileFailed"), file.getAbsolutePath(), chunk + 1, total), x);
        classLogger.error(MessageFormat.format(stringsClient.getString("log4jSCSDebugSendingFileFailed"), file.getAbsolutePath(), chunk + 1, total));
        System.exit(STExitCode.UPLOAD_ERROR.getId());
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
        if (chunk == total) {
            classLogger.trace(stringsClient.getString("log4jSCSTraceSendingFileDone") + file.getAbsolutePath());
        } else {
            classLogger.trace(MessageFormat.format(stringsClient.getString("log4jSCSTraceSendingChunkDone"), chunk + 1, total, file.getAbsolutePath()));
        }

    }
}
