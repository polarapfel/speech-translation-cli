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

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.EnumSet;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Tobias Weisserth <tobias.weisserth@microsoft.com>
 */
public class STConfiguration {
    
    private static STConfiguration instance = null;
    
    private Parameters parameters;
    private FileBasedConfigurationBuilder<FileBasedConfiguration> builder;
    private Configuration configuration;
    
    private byte configInitRecursion = -1;

    private static byte debugInstanceCount = 0;
    
    private final Logger classLogger = LogManager.getLogger(STConfiguration.class);
    
    private final String HOMEPATH = System.getProperty("user.home");
    private final String CURRENTWORKINGDIRECTORY = System.getProperty("user.dir");
    
    private final String FILENAME = "Settings.properties";
    private final String FILEPATH = ".speechtranslation";
    private final String CONFIGPATH = HOMEPATH + File.separator + FILEPATH;
    private final String FULLPATH = CONFIGPATH + File.separator + FILENAME;
    
    private final File f = new File(FULLPATH);
    private final File p = new File(CONFIGPATH);
    
    private final Locale currentLocale = Locale.getDefault();
    private final ResourceBundle stringsCli = ResourceBundle.getBundle("com.microsoft.speechtranslationcli.i18n.StringBundleCli", currentLocale);
    private final ResourceBundle stringsClient = ResourceBundle.getBundle("com.microsoft.speechtranslationclient.i18n.StringBundleClient", currentLocale);

    private String version;
    
    
    /**
     * Constructor, protected to enforce singleton
     */
    protected STConfiguration() {
        if (debugInstanceCount > 1) classLogger.debug(stringsCli.getString("log4jStcDebugSingletonViolated"));
        classLogger.trace(stringsCli.getString("log4jStcTraceConstructor"));
        debugInstanceCount++;
    }
    
    /**
     * @return the singleton instance of this class
     */
    public synchronized static STConfiguration getInstance() {
        if (instance == null) {
            instance = new STConfiguration();
            instance.setVersion();
            instance.initConfigFile();
            instance.loadConfig();
        }
        return instance;
    }

    private void loadConfig() {
        this.parameters = new Parameters();
        builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class).configure(parameters.properties().setFileName(FULLPATH));
        builder.setAutoSave(false); // we don't want to write back to the config file
        try {
            configuration = builder.getConfiguration();
        } catch (Exception e) {
            classLogger.debug(stringsCli.getString("log4jStcDebugConfigurationBuilderFailed"), e);
            classLogger.fatal(stringsCli.getString("log4jStcFatalConfigurationBuilderFailed" + FULLPATH));
        }
    }

    private void setVersion() {
        URLClassLoader classLoader = (URLClassLoader) STConfiguration.class.getClassLoader();
        try {
            URL url = classLoader.findResource("META-INF/MANIFEST.MF");
            Manifest manifest = new Manifest(url.openStream());
            Attributes mainAttributes = manifest.getMainAttributes();
            this.version = mainAttributes.getValue("Implementation-Version");
            classLogger.debug(stringsCli.getString("log4jStcDebugRunningVersion") + this.getVersion());
        } catch (IOException ex) {
            classLogger.debug(stringsCli.getString("log4jStcDebugVersionNotFound"), ex);
            classLogger.warn(stringsCli.getString("log4jStcWarnVersionNotFound"));
            this.version = "unknown";
        }
    }

    public String getVersion() {
        return version;
    }

    /**
     * @return the configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * @return the currentWorkingDirectory
     */
    public String getCurrentWorkingDirectory() {
        return CURRENTWORKINGDIRECTORY;
    }
    
    /**
     * This creates a new configuration file (if required) in its proper location, depending
     * on operating system type.
     */
    private synchronized void initConfigFile() {
        configInitRecursion++;
        classLogger.trace(stringsCli.getString("log4jStcTraceInitConfigFile"));
        classLogger.debug(stringsCli.getString("log4jStcDebugInitConfigFileRecursion") + configInitRecursion);
        if (configInitRecursion >= 100) {
            classLogger.fatal(stringsCli.getString("log4jStcFatalMaxRetries"));
            System.exit(STExitCode.CONFIGURATION_INIT_ERROR.getId());
        }        
        if (f.exists()) {
            if (f.isFile()) {
                if (f.canRead()) {
                    classLogger.trace(stringsCli.getString("log4jStcTraceLogFileFound") + f.getAbsolutePath());
                    if (f.length() == 0 && f.canWrite()) {
                        writeDefaultValues();
                        return;
                    }
                    if (f.length() == 0 && !f.canWrite()) {
                        classLogger.warn(stringsCli.getString("log4jStcWarnLogFileNotWritable") + f.getAbsolutePath());
                    }
                } else {
                    classLogger.fatal(stringsCli.getString("log4jStcFatalLogFileNotReadable") + f.getAbsolutePath());
                    System.exit(STExitCode.CONFIGURATION_INIT_ERROR.getId());
                }
            } else {
                try {
                    FileUtils.moveDirectory(f, new File(FULLPATH + "_moved"));
                } catch (IOException ex) {
                    classLogger.fatal(FULLPATH + stringsCli.getString("log4jIoConfigFileIsDirectoryError") + FULLPATH + "_moved");
                    classLogger.debug(FULLPATH + stringsCli.getString("log4jIoConfigFileIsDirectoryError") + FULLPATH + "_moved", ex);
                    System.exit(STExitCode.CONFIGURATION_INIT_ERROR.getId());
                }
                STConfiguration.getInstance().initConfigFile();
            }
        } else {
            if (f.getParentFile().exists()) {
                if (f.getParentFile().isDirectory()) {
                    if (f.getParentFile().canRead() && f.getParentFile().canWrite()) {
                        try {
                            FileUtils.touch(f);
                        } catch (IOException ex) {
                            classLogger.debug(stringsCli.getString("log4jStcDebugFileCouldNotBeCreated") + f.getAbsolutePath(), ex);
                            classLogger.fatal(stringsCli.getString("log4jStcFatalFileCouldNotBeCreated") + f.getAbsolutePath());
                            System.exit(STExitCode.CONFIGURATION_INIT_ERROR.getId());
                        }
                        writeDefaultValues();
                    } else {
                        classLogger.fatal(stringsCli.getString("log4jStcFatalLogFileNotReadableWritable") + f.getParent());
                        System.exit(STExitCode.CONFIGURATION_INIT_ERROR.getId());
                    }
                } else {
                    try {
                        FileUtils.moveFile(f.getParentFile(), new File(f.getParent() + "_moved"));
                    } catch (IOException ex) {
                        classLogger.debug(FULLPATH + stringsCli.getString("log4jIoConfigFileIsDirectoryError") + FULLPATH + "_moved", ex);
                        classLogger.fatal(FULLPATH + stringsCli.getString("log4jIoConfigFileIsDirectoryError") + FULLPATH + "_moved");
                        System.exit(STExitCode.CONFIGURATION_INIT_ERROR.getId());
                    }
                    STConfiguration.getInstance().initConfigFile();
                }
            } else {
                try {
                    FileUtils.forceMkdirParent(f);
                } catch (IOException ex) {
                    classLogger.fatal(stringsCli.getString("log4jStcFatalCannotCreateDirectory") + f.getParent());
                    classLogger.debug(stringsCli.getString("log4jStcDebugCannotCreateDirectory") + f.getParent(), ex);
                    System.exit(STExitCode.CONFIGURATION_INIT_ERROR.getId());
                }
                STConfiguration.getInstance().initConfigFile();
            }
        }
    }
    
    /**
     * Copies the defaults into the target location
     */
    private synchronized void writeDefaultValues() {
        classLogger.trace(stringsCli.getString("log4jStcTraceOpeningSettingsToWrite"));
        try {
            if(f.length() == 0 && f.exists()) {
                EnumSet<STConfigurationDefault> defaultSet = EnumSet.allOf(STConfigurationDefault.class);
                FileUtils.writeLines(f, defaultSet, true);
            } else {
                classLogger.warn(stringsCli.getString(stringsCli.getString("log4jStcWarnWriteAttemptToNonExistingOrNonEmptyFile") + f.getAbsolutePath()));
            }
        } catch (IOException ex) {
            classLogger.fatal(stringsCli.getString("log4jStcFatalLogFileDefaultWriteError") + f.getAbsolutePath());
            classLogger.debug(stringsCli.getString("log4jStcDebugLogFileDefaultWriteError") + f.getAbsolutePath(), ex);
            System.exit(STExitCode.CONFIGURATION_INIT_ERROR.getId());
        }
    }

    /**
     * @return the stringsCli
     */
    public ResourceBundle getStringsCli() {
        return stringsCli;
    }

    /**
     * @return the stringsClient
     */
    public ResourceBundle getStringsClient() {
        return stringsClient;
    }

}
