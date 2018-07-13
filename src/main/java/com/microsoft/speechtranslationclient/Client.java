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

import java.io.*;
import java.net.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/* Useful reference links:
    https://docs.oracle.com/javaee/7/api/javax/websocket/Session.html
    https://docs.oracle.com/javaee/7/api/javax/websocket/RemoteEndpoint.Basic.html
    https://docs.oracle.com/javaee/7/api/javax/websocket/OnMessage.html
    http://www.oracle.com/technetwork/articles/java/jsr356-1937161.html
*/

@ClientEndpoint(configurator = Config.class)
public class Client {
    
    private Logger classLogger = LogManager.getLogger(Client.class);
    static String host = "wss://dev.microsofttranslator.com";
    static String path = "/speech/translate";
    static String params = "?api-version=1.0&from=en-US&to=it-IT&features=texttospeech&voice=it-IT-Elsa";
    static String uri = host + path + params;
    //static String uri = "wss://echo.websocket.org";
    
    //static String input_path = "speak.wav";
    static String input_path = "/tmp/speak.wav";
    static String output_path = "/tmp/speak2.wav";

    Session session = null;

    @OnOpen
    public void onOpen(Session session) {
        if (session == null){
            classLogger.error("onOpen session is null.");
        } else{
            classLogger.trace("OnOpen query string: " + session.getQueryString());
            classLogger.trace("OnOpen protocol version: " + session.getProtocolVersion());
            classLogger.trace("OnOpen id: " + session.getId());
            classLogger.trace("OnOpen sub protocol: " + session.getNegotiatedSubprotocol());
            classLogger.trace("OnOpen secure? " + session.isSecure());
        }
        this.session = session;
        SendAudio ();
    }

    @OnMessage
    public void onTextMessage(String message, Session session){
        classLogger.trace("OnMessage:String " + message);
    }

/*
Use the following signature to receive the message in parts:
    public void onBinaryMessage(byte[] buffer, boolean last, Session session)
Use the following signature to receive the entire message:
    public void onBinaryMessage(byte[] buffer, Session session)
See:
    https://docs.oracle.com/javaee/7/api/javax/websocket/OnMessage.html
*/
    @OnMessage
    public void onBinaryMessage(byte[] buffer, Session session) {
        classLogger.trace("OnMessage:byte buffer " + buffer.length + "bytes received.");
        try {
            FileOutputStream stream = new FileOutputStream(output_path);
            stream.write(buffer);
            stream.close();
            classLogger.trace("Received buffer written to file.");
            Close ();
        } catch (Exception e) {
            classLogger.error("Error in OnMessage: byte buffer", e);
        }
    }

    @OnError
    public void onError(Session session, Throwable e) {
        classLogger.error("OnError " + e.getMessage(), e);
    }

    @OnClose
    public void myOnClose (CloseReason reason) {
        classLogger.trace("OnClose " + reason.getReasonPhrase());
    }

    public void SendAudio() {
        try {
            File file = new File(input_path);
            FileInputStream stream = new FileInputStream (file);
            byte buffer[] = new byte[(int)file.length()];
            stream.read(buffer);
            stream.close();
            sendMessage (buffer);
        } catch (Exception e) {
            classLogger.error("Error while sending audio.", e);
        }
    }

    public void sendMessage(byte[] message) {
        try {
            classLogger.trace("Sending audio.");
            OutputStream stream = this.session.getBasicRemote().getSendStream();
            stream.write (message);
/* Make sure the audio file is followed by silence.
 * This lets the service know that the audio input is finished. */
            classLogger.trace("Sending silence.");
            byte silence[] = new byte[3200000];
            stream.write (silence);
            stream.flush();
        } catch (Exception e) {
            classLogger.error("Error while sending message.", e);
        }
    }

    public void Connect () throws Exception {
        try {
            classLogger.trace("Connecting.");
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
/* Some code samples show container.connectToServer as returning a Session, but this seems to be false. */
            container.connectToServer(this, new URI(uri));
        } catch (Exception e) {
            classLogger.error("Error connecting.", e);
        }
    }

    public void Close () throws Exception {
        try {
            classLogger.trace("Closing connection.");
            this.session.close ();
        } catch (Exception e) {
            classLogger.error("Error closing.", e);
        }
    }
}