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

import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class STValidate {
    private static final Logger classLogger = LogManager.getLogger(STValidate.class);
    private static final STConfiguration configInstance = STConfiguration.getInstance();
    private static final ResourceBundle stringsCli = configInstance.getStringsCli();

    public static void validateFiles(File[] inputFiles) throws STValidationException {
        new ArrayList<File>(Arrays.asList(inputFiles)).forEach(new Consumer<File>() {
            public void accept(File file) {
                if(!file.exists()){
                    throw new STValidationException(file.getAbsolutePath(),
                            stringsCli.getString("StvValidationFileDoesNotExist"), true);
                } else if(!file.canRead()) {
                    throw new STValidationException(file.getAbsolutePath(),
                            stringsCli.getString("StvValidationFileCannotRead"), true);
                } else if(file.isDirectory()) {
                    throw new STValidationException(file.getAbsolutePath(),
                            stringsCli.getString("StvValidationFileIsDirectory"), true);
                } else if(file.length() == 0) {
                    throw new STValidationException(file.getAbsolutePath(),
                            stringsCli.getString("StvValidationFileLengthZero"), true);
                } else if(file.isHidden()) {
                    throw new STValidationException(file.getAbsolutePath(),
                            stringsCli.getString("StvValidationFileIsHidden"), true);
                }
            }
        });
    }

    public static void validateOutputDir(File outputDir) throws STValidationException {
        if(!outputDir.exists()){
            throw new STValidationException(outputDir.getAbsolutePath(),
                    stringsCli.getString("StvValidationOutputDirDoesNotExist"), true);
        } else if(!outputDir.canWrite()) {
            throw new STValidationException(outputDir.getAbsolutePath(),
                    stringsCli.getString("StvValidationOutputDirCannotWrite"), true);
        } else if (outputDir.isHidden()) {
            throw new STValidationException(outputDir.getAbsolutePath(),
                    stringsCli.getString("StvValidationOutputDirIsHidden"), true);
        } else if (!outputDir.canRead()) {
            throw new STValidationException(outputDir.getAbsolutePath(),
                    stringsCli.getString("StvValidationOutputDirCannotRead"), false);
        } else if (!outputDir.isDirectory()) {
            throw new STValidationException(outputDir.getAbsolutePath(),
                    stringsCli.getString("StvValidationOutputDirIsNoDir"), true);
        }
    }

    // TODO: validate filename suffix for output files with [^-_.A-Za-z0-9]

    private static <E extends Enum<E>> void validateStringInEnum(final Class<E> enumClass, String s)
            throws STValidationException {
        if(!Optionable.isValidOption(enumClass, s)) {
            throw new STValidationException(s, stringsCli.getString("StvValidationInvalidOption"), true);
        }
    }

    public static void validateProfanityAction(String profanityAction) throws STValidationException {
        try {
            validateStringInEnum(ProfanityAction.class, profanityAction);
        } catch (STValidationException ex) {
            throw ex;
        }
    }

    public static void validateProfanityMarker(String profanityMarker) throws STValidationException {
        try {
            validateStringInEnum(ProfanityMarker.class, profanityMarker);
        } catch (STValidationException ex) {
            throw ex;
        }
    }

    public static void validateAudioFormat(String audioFormat) throws STValidationException {
        try {
            validateStringInEnum(AudioFormat.class, audioFormat);
        } catch (STValidationException ex) {
            throw ex;
        }
    }

    public static void validateFeature(String feature) throws STValidationException {
        try {
            validateStringInEnum(Feature.class, feature);
        } catch (STValidationException ex) {
            throw ex;
        }
    }

    interface Optionable {
        String getOptionValue();

        static <E extends Enum<E>> boolean isValidOption(final Class<E> enumClass, String testOption) {
            for (E e : EnumUtils.getEnumList(enumClass)) {
                try {
                    Optionable ce = (Optionable) e;
                    if(ce.getOptionValue().compareTo(testOption) == 0) {
                        return true;
                    }
                } catch (Exception ex) {
                    return false;
                }
            }
            ;
            return false;
        }
    }

    enum ProfanityAction implements Optionable {
        MARKED ("Marked"),
        DELETED ("Deleted"),
        NOACTION ("NoAction");

        private String profanityAction;

        ProfanityAction(String s) {
            this.profanityAction = s;
        }

        @Override
        public String getOptionValue() {
            return profanityAction;
        }
    }

    enum ProfanityMarker implements Optionable {
        ASTERISK ("Asterisk"),
        TAG ("Tag");

        private String profanityMarker;

        ProfanityMarker(String s) {
            this.profanityMarker = s;
        }

        public String getOptionValue() {
            return profanityMarker;
        }
    }

    enum AudioFormat implements Optionable {
        WAV ("audio/wav"),
        MP3 ("audio/mp3");

        private String audioFormat;

        AudioFormat(String s) {
            this.audioFormat = s;
        }

        public String getOptionValue() {
            return audioFormat;
        }
    }

    enum Feature implements Optionable {
        TESTTOSPEACH ("TextToSpeech"),
        PARTIAL ("Partial"),
        TIMINGINFO ("TimingInfo");

        private final String feature;

        Feature(String s) {
            this.feature = s;
        }

        public String getOptionValue() {
            return feature;
        }
    }
}