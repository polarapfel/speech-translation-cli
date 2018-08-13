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

import com.microsoft.speechtranslationcli.STCli;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import static org.apache.commons.io.IOUtils.copy;

import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket(maxTextMessageSize = 64 * 1024, maxBinaryMessageSize = 64 * 4096,
        batchMode = BatchMode.AUTO, inputBufferSize = 64 * 4096, maxIdleTime = 60)
public class SpeechClientSocket {
    private Parameters parameters;
    private FileBasedConfigurationBuilder<FileBasedConfiguration> builder;
    private Configuration configuration;

    private STCli commandLine;
    
    private final Logger classLogger = LogManager.getLogger(SpeechClientSocket.class);
    static String host = "wss://dev.microsofttranslator.com";
    static String path = "/speech/translate";
    static String params = "?api-version=1.0&from=en-US&to=it-IT&features=texttospeech&voice=it-IT-Elsa";
    static String uri = host + path + params;
      
    private final CountDownLatch closeLatch;
    private Session session = null;
    
    public SpeechClientSocket() {
        this.parameters = new Parameters();
        builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                .configure(parameters.properties()
                        .setFileName("Settings.properties"));
        try {
            configuration = builder.getConfiguration();
        } catch (ConfigurationException e) {
            classLogger.error("Configuration properties could not be loaded.", e);
        }
        this.closeLatch = new CountDownLatch(1);
    }
    
    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }
    
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        classLogger.trace("Connection closed: %d - %s%n", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown(); // trigger latch
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        classLogger.trace("Got connect: %s%n", session);
        this.session = session;
        try {
            File inputFile = new File(configuration.getString("settings.default.file.input"));
            FileInputStream inputStream = new FileInputStream(inputFile);
            int inputFileLength = (int) inputFile.length();
            byte inputBuffer[] = new byte[inputFileLength];
            inputStream.read(inputBuffer);
            inputStream.close();
            
            classLogger.trace("Sending file.");
            Future<Void> fut;
            fut = session.getRemote().sendBytesByFuture(ByteBuffer.wrap(inputBuffer));
            fut.get(inputFileLength * configuration.getInt("settings.websocket.upload.timeout"), TimeUnit.NANOSECONDS); // wait for send to complete. TODO make this dependant on length of file.
            classLogger.trace("File sent.");

            session.close(StatusCode.NORMAL, "I'm done");
        } catch (InterruptedException | ExecutionException t) {
            classLogger.error("Something went wrong when connecting.", t);
        } catch (TimeoutException o){
            classLogger.error("Timeout for upload was exceeded.", o);
        } catch (IOException e) {
            classLogger.error("Error reading input file.", e);
        }
    }
    
    @OnWebSocketMessage
    public void onMessage(Session session, InputStream stream) {
        classLogger.trace("Receiving streamed data.");
        try {
            FileOutputStream outputStream = new FileOutputStream("");
            copy(stream, outputStream);
            stream.close();
            classLogger.trace("Received buffer written to file.");
            session.close(StatusCode.NORMAL, "I'm done");
        } catch (IOException e) {
            classLogger.error("Error in OnMessage while receiving streamed data.", e);
        }
    }
    
    @OnWebSocketMessage
    public void onMessage(String msg) {
        classLogger.trace("Got msg: %s%n", msg);
    }
}