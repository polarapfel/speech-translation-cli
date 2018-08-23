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

/**
 *
 * @author Tobias Weisserth <tobias.weisserth@microsoft.com>
 */
public enum STExitCode {
    NORMAL(0, "Normal exit."),
    CONFIGURATION_INIT_ERROR(1, "Configuration folder and file could not be initialized."),
    CONFIGURATION_READ_ERROR(2, "Configuration file could not be read."),
    VALIDATION_ERROR(3, "Command line parameter validation failed."),
    CONNECTION_ERROR(4, "Websocket Connection Error."),
    UPLOAD_TIMEOUT(5, "Upload timeout exceeded."),
    FILE_READ_ERROR(6, "File could not be read."),
    FILE_WRITE_ERROR(7, "File could not be written."),
    RUNTIME_ERROR(8, "A runtime error occurred."),
    INTERNAL_ERROR(9, "Internal error occurred."),
    UPLOAD_ERROR(10, "File upload error.");


    private final int id;
    private final String msg;

    STExitCode(int id, String msg) {
        this.id = id;
        this.msg = msg;
    }

    public int getId() {
        return this.id;
    }

    public String getMsg() {
        return this.msg;
    }
}
