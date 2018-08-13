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

import java.util.HashMap;
import java.util.Map;

/**
 * Default configuration values.
 *
 * @author Tobias Weisserth <tobias.weisserth@microsoft.com>
 */
public enum STConfigurationDefault {
    API_ENDPOINT("azure.speechtranslation.api.endpoint", "wss://dev.microsofttranslator.com"),
    API_PATH("azure.speechtranslation.api.path", "/speech/translate"),
    API_VERSION("azure.speechtranslation.api.version", "api-version=1.0"),
    API_FEATURES("azure.speechtranslation.api.features", "TextToSpeech"),
    API_PROFANITY_ACTION("azure.speechtranslation.api.profanityaction", "Marked"),
    API_PROFANITY_MARKER("azure.speechtranslation.api.profanitymarker", "Asterisk"),
    API_AUDIO("azure.speechtranslation.api.audio", "audio/wav"),
    API_VOICE("azure.speechtranslation.api.voice", ""),
    API_KEY("azure.speechtranslation.api.key", ""),
    WEBSOCKET_TIMEOUT("settings.websocket.upload.timeout", "1000"),
    WEBSOCKET_MAX_BINARY_MSG("settings.websocket.maxbinary", ""),
    WEBSOCKET_MAX_TEXT_MSG("settings.websocket.maxtext", ""),
    WEBSOCKET_BUFFER("settings.websocket.buffer", ""),
    CLI_POSTFIX("settings.cli.postfix", ".translation"),
    CLI_OUTPUT_DIR("settings.cli.outputdir", ""),
    CLI_OMIT_TEXT("settings.cli.omittext", "");

    private final String key;
    private final String value;

    // Reverse-lookup map for getting a default from a key, safe to use in modern JVM implementations (>1.6)
    private static final Map<String, STConfigurationDefault> lookup = new HashMap<>();
    static {
        for (STConfigurationDefault d : STConfigurationDefault.values()) {
            lookup.put(d.getKey(), d);
        }
    }

    STConfigurationDefault(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return this.key;
    }
    public String getValue() {
        return this.value;
    }

    public static STConfigurationDefault get(String key) {
        return lookup.get(key);
    }
    
    @Override
    public String toString() {
        return getKey() + " = " + getValue();
    }
}
