/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.springframework.beans.factory.annotation.Required;

public abstract class DSpaceRunnable implements Runnable {

    private String name;
    private String description;
    protected CommandLine commandLine;
    protected Options options;
    protected DSpaceRunnableHandler handler;

    public String getName() {
        return name;
    }

    @Required
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    @Required
    public void setDescription(String description) {
        this.description = description;
    }

    public Options getOptions() {
        return options;
    }

    private void parse(String[] args) throws ParseException {
        commandLine = new DefaultParser().parse(getOptions(), args);
        setup();
    }

    public void printHelp() {
        handler.printHelp(options, name);
    }


    @Override
    public void run() {
        try {
            handler.start();
            internalRun();
            handler.handleCompletion();
        } catch (Exception e) {
            handler.handleException(e);
        }
    }

    private void setHandler(DSpaceRunnableHandler dSpaceRunnableHandler) {
        this.handler = dSpaceRunnableHandler;
    }

    public void initialize(String[] args, DSpaceRunnableHandler dSpaceRunnableHandler) throws ParseException {
        this.setHandler(dSpaceRunnableHandler);
        this.parse(args);
    }

    public abstract void internalRun() throws Exception;

    public abstract void setup() throws ParseException;
}
