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
package com.microsoft.speechtranslationcli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.musicg.wave.WaveHeader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.IOUtils.copy;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.*;

/*
General idea is to always handle a single file with a single web socket connection. If you want concurrency, open multiple
sockets at a time with each socket handling its single file.
 */

@WebSocket
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
        classLogger.trace(MessageFormat.format(stringsClient.getString("log4jSCSTraceConnectionClose"), String.valueOf(statusCode), reason));
        this.session = null;
        this.closeLatch.countDown(); // trigger latch
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        classLogger.trace(stringsClient.getString("log4jSCSTraceConnectionOpen"), session);
        this.session = session;

        session.getPolicy().setMaxBinaryMessageSize(configInstance.getConfiguration().getInt(STConfigurationDefault.WEBSOCKET_MAX_BINARY_MSG.getKey()));
        session.getPolicy().setMaxTextMessageSize(configInstance.getConfiguration().getInt(STConfigurationDefault.WEBSOCKET_MAX_TEXT_MSG.getKey()));
        session.getPolicy().setIdleTimeout(configInstance.getConfiguration().getInt(STConfigurationDefault.WEBSOCKET_MAX_IDLE.getKey()));
        session.getPolicy().setInputBufferSize(configInstance.getConfiguration().getInt(STConfigurationDefault.WEBSOCKET_BUFFER.getKey()));

        sendFileInChunks();
    }

    @OnWebSocketMessage
    public void onMessage(Session session, InputStream stream) {
        classLogger.trace(stringsClient.getString("log4jSCSTraceOnMessageBinary"));
        try {

            // TODO change file type extension based on format


            //String test = STValidate.AudioFormat.WAV.getOptionValue();

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
            classLogger.trace(stringsClient.getString("log4jSCSTraceOmitMessageReceived"));
        } else {
            classLogger.trace(MessageFormat.format(stringsClient.getString("log4jSCSTraceOnMessageText"), msg));
            File fileTranslatedJson = new File(configInstance.getConfiguration().getString(STConfigurationDefault.CLI_OUTPUT_DIR.getKey()),
                    FilenameUtils.getBaseName(inputFile.getName())
                            + configInstance.getConfiguration().getString(STConfigurationDefault.CLI_POSTFIX.getKey())
                            + ".json");
            try {
                FileUtils.writeStringToFile(fileTranslatedJson, msg, "UTF8");
            } catch (IOException e) {
                classLogger.debug(stringsClient.getString("log4jSCSIOExceptionWrite"), e);
                classLogger.error(stringsClient.getString("log4jSCSIOExceptionWrite"));
                System.exit(STExitCode.FILE_WRITE_ERROR.getId());
            }
        }
    }

    @OnWebSocketError
    public void onError(Session s, Throwable t) {
        classLogger.debug(stringsClient.getString("log4jSCSDebugOnWebSocketError"), t);
        classLogger.error(stringsClient.getString("log4jSCSDebugOnWebSocketError"));
        System.exit(STExitCode.CONNECTION_ERROR.getId());
    }

    /*
     * The general idea here is to play nice with the way the API service endpoint has been designed. The service expects
     * real time communication, not large pre-recorded audio files. The service will be unable to accept a very large
     * audio file that is being sent in one piece faster than the service can work through it. To prevent malicious
     * resource allocation, the connection will be closed by the endpoint if that happens.
     *
     * The safest way to deal with this is to chunk the audio into 0.25s chunks and upload them with a delay in between.
     *
     * This method offers an implementation for this, determining the length of the audio file by parsing the WAV header
     * using the musicg library.
     *
     * A buffer of 320000 bytes of silence is sent after the last chunk uploaded.
     */
    private void sendFileInChunks() {
        try {
            InputStream inputFileHeader = new FileInputStream(inputFile);

            InputStream inputStream = new FileInputStream(inputFile);
            WaveHeader fileHeader = new WaveHeader(inputFileHeader);
            int sampleRate = fileHeader.getSampleRate();
            double lengthInSeconds = (inputFileLength / ((sampleRate * fileHeader.getChannels() * fileHeader.getBitsPerSample()) / 8.0));
            int numberOfChunks = (int) Math.ceil(lengthInSeconds / 0.25);
            int chunkSize = (int) Math.ceil((double) inputFileLength / numberOfChunks);
            inputFileHeader.close();

            byte[] inputBuffer = new byte[inputFileLength];

            int delta = chunkSize * numberOfChunks - inputFileLength;

            int bytesRead = inputStream.read(inputBuffer);
            classLogger.debug(bytesRead + stringsClient.getString("log4jSCSDebugBytesReadToBuffer") + inputFileLength);
            inputStream.close();

            if (bytesRead < inputFileLength) {
                classLogger.warn(MessageFormat.format(stringsClient.getString("log4jSCSWarnPartialFileRead"), bytesRead, inputFileLength, inputFile.getAbsolutePath()));
            }

            classLogger.trace(MessageFormat.format(stringsClient.getString("log4jSCSTraceSendingFile"), inputFile.getAbsolutePath(), numberOfChunks));
            for (int i = 0; i < numberOfChunks; i++) {
                int from = i * chunkSize;
                int to = i * chunkSize + chunkSize;
                if (i == numberOfChunks - 1) {
                    to = to - delta;
                }
                byte[] chunkBuffer = Arrays.copyOfRange(inputBuffer, from, to);
                session.getRemote().sendBytes(ByteBuffer.wrap(chunkBuffer), new STSendChunkStatus(inputFile, i, numberOfChunks));
                if (i != numberOfChunks - 1) {
                    try {
                        classLogger.debug(MessageFormat.format(stringsClient.getString("log4jDebugChunkDelay"), String.valueOf(500)));
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        classLogger.debug(stringsClient.getString("log4jSCSDebugInternalError"), e);
                    }
                }
            }

            byte[] silence = new byte[320000];
            session.getRemote().sendBytes(ByteBuffer.wrap(silence), new STSendSilenceStatus(320000));
        } catch (IOException e) {
            classLogger.debug(stringsClient.getString("log4jSCSIOExceptionRead") + inputFile.getAbsolutePath(), e);
            classLogger.error(stringsClient.getString("log4jSCSIOExceptionRead") + inputFile.getAbsolutePath());
            System.exit(STExitCode.FILE_READ_ERROR.getId());
        }
    }
}