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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.ResourceBundle;

// TODO: implement input validation for picocli options and parameters

@CommandLine.Command(name = "speechtranslate", mixinStandardHelpOptions = true, versionProvider = STManifestVersionProvider.class)
public class STCli implements Runnable {
    private static final Logger classLogger = LogManager.getLogger(STCli.class);
    private static final STConfiguration configInstance = STConfiguration.getInstance();
    private static final ResourceBundle stringsCli = configInstance.getStringsCli();

    /*
     * TODO: Currently, picocli does not allow for ResourceBundle properties in its annotations, so all text literals are hard-coded :(
     */

    @Option(names = { "-v", "--verbose" }, description = "Verbose mode. Helpful for troubleshooting. " +
            "Multiple -v options increase the verbosity.")
    private boolean[] verbose = new boolean[0];

    @Option(names = "--from", description = "Specifies the language of the incoming speech.", required = true)
    private String from;

    @Option(names = "--to",
            description = "Specifies the language to translate the transcribed text into.", required = true)
    private String to;

    @Option(names = "--output-dir", description = "Directory to which translated files will be written. " +
            "The default is the current working directory.")
    private String outputLocation = configInstance.getCurrentWorkingDirectory();

    @Option(names = "--features", description = "Comma-separated set of features selected by the client. " +
            "Available features include: TextToSpeech, Partial, TimingInfo", required = false)
    private String features;

    @Option(names = {"--voice"},
            description = "Identifies what voice to use for text-to-speech rendering of the translated text. " +
                    "The value is one of the voice identifiers from the tts scope in the response from the Languages API. " +
                    "If a voice is not specified the system will automatically choose one when the text-to-speech " +
                    "feature is enabled.", required = false)
    private String voice;

    @Option(names = "--audio",
            description = "Specifies the format of the text-to-speech audio stream returned by the service. " +
                    "Available options are: audio/wav, audio/mp3. Default is audio/wav.", required = false)
    private String audio = "audio/wav";

    @Option(names = "--profanity-action",
            description = "Specifies how the service should handle profanities recognized in the speech. " +
                    "Valid actions are: NoAction, Marked, Deleted.", required = false)
    private String profanityAction = "Marked";

    @Option(names = "--profanity-marker",
            description = "Specifies how detected profanities are handled when ProfanityAction is set to Marked. " +
                    "Valid options are: Asterisk, Tag. The default is Asterisk.", required = false)
    private String profanityMarker = "Asterisk";

    @Option(names = "--subscription-key",
            description = "Cognitive Services Translator Speech API key", required = true)
    private String subscriptionKey;

    @Parameters(arity = "1..*", paramLabel = "FILE", description = "WAV file(s) to translate.")
    private File[] inputFiles;

    // TODO: add option to define output file suffix matching [^-_.A-Za-z0-9]

    public static void main(String[] args) {
        CommandLine.run(new STCli(), System.out, args);
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        classLogger.trace(stringsCli.getString("log4jMainTraceStart"));


    }
}
