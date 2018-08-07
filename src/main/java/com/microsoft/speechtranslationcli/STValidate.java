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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class STValidate {
    private static final Logger classLogger = LogManager.getLogger(STValidate.class);
    private static final STConfiguration configInstance = STConfiguration.getInstance();
    private static final ResourceBundle stringsCli = configInstance.getStringsCli();

    public static void validateFiles(File[] inputFiles) throws ValidationException {
        new ArrayList<File>(Arrays.asList(inputFiles)).forEach(new Consumer<File>() {
            public void accept(File file) {
                if(!file.exists()){
                    throw new ValidationException(file.getAbsolutePath(), stringsCli.getString("StvValidationFileDoesNotExist"), true);
                } else if(!file.canRead()) {
                    throw new ValidationException(file.getAbsolutePath(), stringsCli.getString("StvValidationFileCannotRead"), true);
                } else if(file.isDirectory()) {
                    throw new ValidationException(file.getAbsolutePath(), stringsCli.getString("StvValidationFileIsDirectory"), true);
                } else if(file.length() == 0) {
                    throw new ValidationException(file.getAbsolutePath(), stringsCli.getString("StvValidationFileLengthZero"), true);
                } else if(file.isHidden()) {
                    throw new ValidationException(file.getAbsolutePath(), stringsCli.getString("StvValidationFileIsHidden"), true);
                }
            }
        });
    }

    public static void validateOutputDir(File outputDir) throws ValidationException {
        if(!outputDir.exists()){
            throw new ValidationException(outputDir.getAbsolutePath(), stringsCli.getString("StvValidationOutputDirDoesNotExist"), true);
        } else if(!outputDir.canWrite()) {
            throw new ValidationException(outputDir.getAbsolutePath(), stringsCli.getString("StvValidationOutputDirCannotWrite"), true);
        } else if (outputDir.isHidden()) {
            throw new ValidationException(outputDir.getAbsolutePath(), stringsCli.getString("StvValidationOutputDirIsHidden"), true);
        } else if (!outputDir.canRead()) {
            throw new ValidationException(outputDir.getAbsolutePath(), stringsCli.getString("StvValidationOutputDirCannotRead"), false);
        } else if (!outputDir.isDirectory()) {
            throw new ValidationException(outputDir.getAbsolutePath(), stringsCli.getString("StvValidationOutputDirIsNoDir"), true);
        }
    }

    // TODO: validate filename suffix for output files with [^-_.A-Za-z0-9]

    private static class ValidationException extends IllegalArgumentException {
        private String optionOrParameter;
        private boolean isFatal;

        ValidationException(String optionOrParameter, String message, boolean isFatal) {
            super(message);
            this.optionOrParameter = optionOrParameter;
            this.isFatal = isFatal;
        }

        public String getOptionOrParameter() {
            return optionOrParameter;
        }

        public boolean isFatal() {
            return isFatal;
        }
    }
}

enum ProfanityActionEnum {
    MARKED ("Marked"),
    DELETED ("Deleted"),
    NOACTION ("NoAction");

    private String profanityAction;

    ProfanityActionEnum(String s) {
        this.profanityAction = s;
    }

    public String getProfanityAction() {
        return profanityAction;
    }
}

enum ProfanityMarkerEnum {
    ASTERISK ("Asterisk"),
    TAG ("Tag");

    private String profanityMarker;

    ProfanityMarkerEnum(String s) {
        this.profanityMarker = s;
    }

    public String getProfanityMarker() {
        return profanityMarker;
    }
}

enum AudioFormatEnum {
    WAV ("audio/wav"),
    MP3 ("audio/mp3");

    private String audioFormat;

    AudioFormatEnum(String s) {
        this.audioFormat = s;
    }

    public String getAudioFormat() {
        return audioFormat;
    }
}

enum FeaturesEnum {
    TESTTOSPEACH ("TextToSpeech"),
    PARTIAL ("Partial"),
    TIMINGINFO ("TimingInfo");

    private final String feature;

    FeaturesEnum(String s) {
        this.feature = s;
    }

    public String getFeature() {
        return feature;
    }
}