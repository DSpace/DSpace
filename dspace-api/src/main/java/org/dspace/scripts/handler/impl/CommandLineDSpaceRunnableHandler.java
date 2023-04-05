/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.handler.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;

/**
 * This is an implementation for the CommandLineDSpaceRunnables which means that these implementations
 * are used by DSpaceRunnables which are called from the CommandLine
 */
public class CommandLineDSpaceRunnableHandler implements DSpaceRunnableHandler {
    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(CommandLineDSpaceRunnableHandler.class);

    @Override
    public void start() {
        System.out.println("The script has started");
    }

    @Override
    public void handleCompletion() {
        System.out.println("The script has completed");
    }

    @Override
    public void handleException(Exception e) {
        handleException(null, e);
    }

    @Override
    public void handleException(String message) {
        handleException(message, null);
    }

    @Override
    public void handleException(String message, Exception e) {
        if (message != null) {
            System.err.println(message);
            log.error(message);
        }
        if (e != null) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }

        System.exit(1);
    }

    @Override
    public void logDebug(String message) {
        log.debug(message);
    }

    @Override
    public void logInfo(String message) {
        System.out.println(message);
        log.info(message);
    }

    @Override
    public void logWarning(String message) {
        System.out.println(message);
        log.warn(message);
    }

    @Override
    public void logError(String message) {
        System.err.println(message);
        log.error(message);
    }

    @Override
    public void logError(String message, Throwable throwable) {
        System.err.println(message);
        log.error(message, throwable);
    }

    @Override
    public void printHelp(Options options, String name) {
        if (options != null) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(name, options);
        }
    }

    @Override
    public Optional<InputStream> getFileStream(Context context, String fileName) throws IOException {
        File file = new File(fileName);
        if (!(file.exists() && file.isFile())) {
            return Optional.empty();
        }
        return Optional.of(FileUtils.openInputStream(file));
    }

    @Override
    public void writeFilestream(Context context, String fileName, InputStream inputStream, String type)
        throws IOException {
        File file = new File(fileName);
        FileUtils.copyInputStreamToFile(inputStream, file);
    }

    @Override
    public List<UUID> getSpecialGroups() {
        return Collections.emptyList();
    }
}
