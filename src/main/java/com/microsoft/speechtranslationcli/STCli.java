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

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.management.Query;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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

    @Option(names = "--output-postfix",
            description = "File name postfix attached to the end of the translated output file(s). " +
                    "The default is \".translation\". E.g. a file input.wav will result in " +
                    "input.translation.wav and input.translation.json.", required = false)
    private String postfix;

    @Option(names = "--omit-text", description = "Do not write text translation(s) to file(s). " +
            "This option is only useful when using the TextToSpeech feature.", required = false)
    private boolean omitTextFiles;

    @Option(names = "--output-dir", description = "Directory to which translated files will be written. " +
            "The default is the current working directory.", required = false)
    private File outputLocation;

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
    private String audio;

    @Option(names = "--profanity-action",
            description = "Specifies how the service should handle profanities recognized in the speech. " +
                    "Valid actions are: NoAction, Marked, Deleted.", required = false)
    private String profanityAction;

    @Option(names = "--profanity-marker",
            description = "Specifies how detected profanities are handled when ProfanityAction is set to Marked. " +
                    "Valid options are: Asterisk, Tag. The default is Asterisk.", required = false)
    private String profanityMarker;

    @Option(names = "--subscription-key",
            description = "Cognitive Services Translator Speech API key", required = false)
    private String subscriptionKey;

    @Parameters(arity = "1..*", paramLabel = "FILE", description = "WAV file(s) to translate.")
    private File[] inputFiles;

    // TODO: add option to define output file suffix matching [^-_.A-Za-z0-9]

    public static void main(String[] args) throws Exception {
        classLogger.trace(stringsCli.getString("log4jMainTraceStart"));
        CommandLine.run(new STCli(), System.out, args);
        classLogger.trace(stringsCli.getString("log4jMainTraceEnd"));
    }

    public void run() {
        /*
        will only be called if command line parameters parse correctly through picocli
         */
        classLogger.trace(stringsCli.getString("log4jRunTraceStart"));
        overlayConfiguration(configInstance.getConfiguration());
        validateParameters();
        validateOptions();
        classLogger.trace(stringsCli.getString("log4jStcTraceConfigurationReady"));
    }

    private void validateOptions() {
        // run validation on some of the input parameters and options
        // TODO: write more (and better) validation code to cover all input

        try {
            STValidate.validateAudioFormat(configInstance.getConfiguration().getString(STConfigurationDefault.API_AUDIO.getKey()));
            STValidate.validateFeature(configInstance.getConfiguration().getString(STConfigurationDefault.API_FEATURES.getKey()));
            STValidate.validateOutputDir(new File(configInstance.getConfiguration().getString(STConfigurationDefault.CLI_OUTPUT_DIR.getKey())));
            STValidate.validateProfanityAction(configInstance.getConfiguration().getString(STConfigurationDefault.API_PROFANITY_ACTION.getKey()));
            STValidate.validateProfanityMarker(configInstance.getConfiguration().getString(STConfigurationDefault.API_PROFANITY_MARKER.getKey()));
        } catch (STValidationException e) {
            classLogger.debug(stringsCli.getString("StvValidationDebugValidationException"));
            classLogger.debug(e.getMessage() + e.getOptionOrParameter(), e);
            if (e.isFatal()) {
                classLogger.fatal(e.getMessage() + e.getOptionOrParameter());
                System.exit(STExitCode.VALIDATION_ERROR.getId());
            } else {
                classLogger.warn(e.getMessage() + e.getOptionOrParameter());
            }
        }
    }

    private void validateParameters() {
        try {
            STValidate.validateFiles((File[]) this.configInstance.getConfiguration().getArray(File.class, STConfigurationOverlay.API_FILES.getKey()));
        } catch (STValidationException e) {
            classLogger.debug(stringsCli.getString("StvValidationDebugValidationException"));
            classLogger.debug(e.getMessage() + e.getOptionOrParameter(), e);
            if (e.isFatal()) {
                classLogger.fatal(e.getMessage() + e.getOptionOrParameter());
                System.exit(STExitCode.VALIDATION_ERROR.getId());
            } else {
                classLogger.warn(e.getMessage() + e.getOptionOrParameter());
            }
        }
    }

    private void overlayConfiguration(Configuration configuration) {
        // add mandatory options and parameter
        configuration.addProperty(STConfigurationOverlay.API_TO.getKey(), to);
        configuration.addProperty(STConfigurationOverlay.API_FROM.getKey(), from);
        configuration.addProperty(STConfigurationOverlay.API_FILES.getKey(), inputFiles);
        // add other options
        if(verbose.length != 0) configuration.addProperty("settings.cli.verbosity", verbose);
        //overlay other options
        if(StringUtils.isAllBlank(subscriptionKey)) configuration.setProperty(STConfigurationDefault.API_KEY.getKey(), subscriptionKey);
        if(StringUtils.isBlank(audio)) configuration.setProperty(STConfigurationDefault.API_AUDIO.getKey(), audio);
        if(StringUtils.isBlank(features)) configuration.setProperty(STConfigurationDefault.API_FEATURES.getKey(), features);
        if(StringUtils.isBlank(profanityAction)) configuration.setProperty(STConfigurationDefault.API_PROFANITY_ACTION.getKey(), profanityAction);
        if(StringUtils.isBlank(profanityMarker)) configuration.setProperty(STConfigurationDefault.API_PROFANITY_MARKER.getKey(), profanityMarker);
        if(StringUtils.isBlank(voice)) configuration.setProperty(STConfigurationDefault.API_VOICE.getKey(), voice);
        if(configuration.getString(STConfigurationDefault.CLI_OMIT_TEXT.getKey()).isEmpty()) {
            configuration.setProperty(STConfigurationDefault.CLI_OMIT_TEXT.getKey(), omitTextFiles);
        }
        if(configuration.getString(STConfigurationDefault.CLI_OUTPUT_DIR.getKey()).isEmpty() && outputLocation != null && !outputLocation.getAbsolutePath().isEmpty()) {
            configuration.setProperty(STConfigurationDefault.CLI_OUTPUT_DIR.getKey(), outputLocation.getAbsolutePath());
        } else {
            configuration.setProperty(STConfigurationDefault.CLI_OUTPUT_DIR.getKey(), configInstance.getCurrentWorkingDirectory());
        }
        if(StringUtils.isBlank(postfix)) configuration.setProperty(STConfigurationDefault.CLI_POSTFIX.getKey(), postfix);
    }
}
