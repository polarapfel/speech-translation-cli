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
 * Default configuration values. These can either be read directly (overriding values from actual configuration files)
 * or be used to populate newly created configuration files.
 *
 * @author Tobias Weisserth <tobias.weisserth@microsoft.com>
 */
public enum STConfigurationDefault {
    API_ENDPOINT("azure.speechtranslation.api.endpoint", "wss://dev.microsofttranslator.com"),
    API_PATH("azure.speechtranslation.api.path", "/speech/translate"),
    API_VERSION("azure.speechtranslation.api.version", "api-version=1.0"),
    API_FEATURES("azure.speechtranslation.api.features", "texttospeech"),
    WEBSOCKET_TIMEOUT("settings.websocket.upload.timeout", "1000");

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
