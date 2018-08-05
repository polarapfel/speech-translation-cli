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

import picocli.CommandLine;

public class STManifestVersionProvider implements CommandLine.IVersionProvider {

    private STConfiguration configuration;

    public STManifestVersionProvider() {
        configuration = STConfiguration.getInstance();
    }

    /**
     * Returns version information for a command.
     *
     * @return version information (each string in the array is displayed on a separate line)
     * @throws Exception an exception detailing what went wrong when obtaining version information
     */
    @Override
    public String[] getVersion() throws Exception {
        try {
            return new String[]{
                    "@|yellow Version " + configuration.getVersion() +", Copyright \u00a9 Microsoft Corporation|@",
                    "@|yellow Using JRE " + System.getProperty("java.vendor") + ", " + System.getProperty("java.version") + "|@",
                    "@|yellow OS: " + System.getProperty("os.name") + ", (" + System.getProperty("os.arch") + "), Version " + System.getProperty("os.version") + "|@"
            };
        } catch (Exception e) {
            throw e; // won't happen
        }
    }
}
