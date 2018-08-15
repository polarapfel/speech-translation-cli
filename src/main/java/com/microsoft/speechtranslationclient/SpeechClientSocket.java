/*
 * The MIT License
 *
 * Copyright 2018 Microsoft.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.microsoft.speechtranslationclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.microsoft.speechtranslationcli.STConfiguration;
import com.microsoft.speechtranslationcli.STConfigurationDefault;
import com.microsoft.speechtranslationcli.STExitCode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.apache.commons.io.IOUtils.copy;

import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/*
General idea is to always handle a single file with a single web socket connection. If you want concurrency, open multiple
sockets at a time with each socket handling its single file.
 */

@WebSocket(maxTextMessageSize = 64 * 1024, maxBinaryMessageSize = 64 * 4096,
        batchMode = BatchMode.AUTO, inputBufferSize = 64 * 1024, maxIdleTime = 60) // defaults, will be overwritten from settings
public class SpeechClientSocket {
    private final Logger classLogger = LogManager.getLogger(SpeechClientSocket.class);

    private final CountDownLatch closeLatch;
    private Session session = null;

    // Everything this socket needs to operate from comes from the shared configuration and the file reference
    private final STConfiguration configInstance = STConfiguration.getInstance();
    private File inputFile;
    int inputFileLength;

    private final ResourceBundle stringsClient = configInstance.getStringsClient();
    
    public SpeechClientSocket(File file) {
        inputFile = file;
        inputFileLength = (int) inputFile.length();
        this.closeLatch = new CountDownLatch(1);
    }
    
    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }
    
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        classLogger.trace( stringsClient.getString("log4jSCSTraceConnectionClose") + "%d - %s%n", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown(); // trigger latch
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        classLogger.trace(stringsClient.getString("log4jSCSTraceConnectionOpen") + "%s%n", session);
        this.session = session;

        session.getPolicy().setMaxBinaryMessageSize(configInstance.getConfiguration().getInt(STConfigurationDefault.WEBSOCKET_MAX_BINARY_MSG.getKey()));
        session.getPolicy().setMaxTextMessageSize(configInstance.getConfiguration().getInt(STConfigurationDefault.WEBSOCKET_MAX_TEXT_MSG.getKey()));
        session.getPolicy().setIdleTimeout(configInstance.getConfiguration().getInt(STConfigurationDefault.WEBSOCKET_MAX_IDLE.getKey()));
        session.getPolicy().setInputBufferSize(configInstance.getConfiguration().getInt(STConfigurationDefault.WEBSOCKET_BUFFER.getKey()));

        try {
            FileInputStream inputStream = new FileInputStream(inputFile);
            byte inputBuffer[] = new byte[inputFileLength];
            inputStream.read(inputBuffer);
            inputStream.close();
            
            classLogger.trace(stringsClient.getString("log4jSCSTraceSendingFile") + inputFile.getAbsolutePath());
            Future<Void> fut;
            fut = session.getRemote().sendBytesByFuture(ByteBuffer.wrap(inputBuffer));
            fut.get(inputFileLength * configInstance.getConfiguration().getInt(STConfigurationDefault.WEBSOCKET_TIMEOUT.getKey()),
                    TimeUnit.NANOSECONDS);
            classLogger.trace(stringsClient.getString("log4jSCSTraceSendingFileDone") + inputFile.getAbsolutePath());
            session.close(StatusCode.NORMAL, stringsClient.getString("SCSSessionCloseReasonDone"));
        } catch (InterruptedException | ExecutionException t) {
            classLogger.debug(stringsClient.getString("log4jSCSInterruptionException"), t);
            classLogger.error(stringsClient.getString("log4jSCSInterruptionException"));
            System.exit(STExitCode.CONNECTION_ERROR.getId());
            // TODO: be more forgiving when prompted to at the CLI
            // TODO: add retry logic
        } catch (TimeoutException o){
            classLogger.debug(inputFile.getAbsolutePath() + stringsClient.getString("log4jSCSTimeoutException")
                    + inputFileLength * configInstance.getConfiguration().getInt(STConfigurationDefault.WEBSOCKET_TIMEOUT.getKey()) + "ns", o);
            classLogger.error(inputFile.getAbsolutePath() + stringsClient.getString("log4jSCSTimeoutException")
                    + inputFileLength * configInstance.getConfiguration().getInt(STConfigurationDefault.WEBSOCKET_TIMEOUT.getKey()) + "ns");
            System.exit(STExitCode.UPLOAD_TIMEOUT.getId());
        } catch (IOException e) {
            classLogger.debug(stringsClient.getString("log4jSCSIOExceptionRead") + inputFile.getAbsolutePath(), e);
            classLogger.error(stringsClient.getString("log4jSCSIOExceptionRead") + inputFile.getAbsolutePath());
            System.exit(STExitCode.FILE_READ_ERROR.getId());
        }
    }
    
    @OnWebSocketMessage
    public void onMessage(Session session, InputStream stream) {
        classLogger.trace(stringsClient.getString("log4jSCSTraceOnMessageBinary"));
        try {
            File fileTranslated = new File(configInstance.getConfiguration().getString(STConfigurationDefault.CLI_OUTPUT_DIR.getKey()),
                    FilenameUtils.getBaseName(inputFile.getName())
                            + configInstance.getConfiguration().getString(STConfigurationDefault.CLI_POSTFIX.getKey())
                            + "." + FilenameUtils.getExtension(inputFile.getName()));

            FileOutputStream outputStream = new FileOutputStream(fileTranslated);
            copy(stream, outputStream);
            stream.close();
            classLogger.trace(stringsClient.getString("log4jSCSTraceReceivingFileDone") + fileTranslated.getAbsolutePath());
            session.close(StatusCode.NORMAL, stringsClient.getString("SCSSessionCloseReasonDone"));
        } catch (IOException e) {
            classLogger.debug(stringsClient.getString("log4jSCSIOExceptionWrite"), e);
            classLogger.error(stringsClient.getString("log4jSCSIOExceptionWrite"));
            System.exit(STExitCode.FILE_WRITE_ERROR.getId());
        }
    }
    
    @OnWebSocketMessage
    public void onMessage(String msg) {
        if (configInstance.getConfiguration().getBoolean(STConfigurationDefault.CLI_OMIT_TEXT.getKey())) {
            classLogger.trace(stringsClient.getString("log4jSCSTraceOmitMessageReceived"), msg);
        } else {
            classLogger.trace(stringsClient.getString("log4jSCSTraceOnMessageText"), msg);
            File fileTranslatedJson = new File(configInstance.getConfiguration().getString(STConfigurationDefault.CLI_OUTPUT_DIR.getKey()),
                    FilenameUtils.getBaseName(inputFile.getName())
                            + configInstance.getConfiguration().getString(STConfigurationDefault.CLI_POSTFIX.getKey())
                            + ".json");
            try {
                FileUtils.writeStringToFile(fileTranslatedJson, msg);
            } catch (IOException e) {
                classLogger.debug(stringsClient.getString("log4jSCSIOExceptionWrite"), e);
                classLogger.error(stringsClient.getString("log4jSCSIOExceptionWrite"));
                System.exit(STExitCode.FILE_WRITE_ERROR.getId());
            }
        }
    }
}