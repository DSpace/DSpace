/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * This abstract class is the class that should be extended by each script.
 * it provides the basic variables to be hold by the script as well as the means to initialize, parse and run the script
 * Every DSpaceRunnable that is implemented in this way should be defined in the scripts.xml config file as a bean
 */
public abstract class DSpaceRunnable implements Runnable {

    /**
     * The name of the script
     */
    private String name;
    /**
     * The description of the script
     */
    private String description;
    /**
     * The CommandLine object for the script that'll hold the information
     */
    protected CommandLine commandLine;
    /**
     * The possible options for this script
     */
    protected Options options;
    /**
     * The handler that deals with this script. This handler can currently either be a RestDSpaceRunnableHandler or
     *  a CommandlineDSpaceRunnableHandler depending from where the script is called
     */
    protected DSpaceRunnableHandler handler;

    @Autowired
    private AuthorizeService authorizeService;

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

    /**
     * This method will take the primitive array of String objects that represent the parameters given to the String
     * and it'll parse these into a CommandLine object that can be used by the script to retrieve the data
     * @param args              The primitive array of Strings representing the parameters
     * @throws ParseException   If something goes wrong
     */
    private void parse(String[] args) throws ParseException {
        commandLine = new DefaultParser().parse(getOptions(), args);
        setup();
    }

    /**
     * This method will call upon the {@link DSpaceRunnableHandler#printHelp(Options, String)} method with the script's
     * options and name
     */
    public void printHelp() {
        handler.printHelp(options, name);
    }


    /**
     * This is the run() method from the Runnable interface that we implement. This method will handle the running
     * of the script and all the database modifications needed for the Process object that resulted from this script
     */
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

    /**
     * This method sets the appropriate DSpaceRunnableHandler depending on where it was ran from and it parses
     * the arguments given to the script
     * @param args                  The arguments given to the script
     * @param dSpaceRunnableHandler The DSpaceRunnableHandler object that defines from where the script was ran
     * @throws ParseException       If something goes wrong
     */
    public void initialize(String[] args, DSpaceRunnableHandler dSpaceRunnableHandler) throws ParseException {
        this.setHandler(dSpaceRunnableHandler);
        this.parse(args);
    }

    /**
     * This method has to be included in every script and this will be the main execution block for the script that'll
     * contain all the logic needed
     * @throws Exception    If something goes wrong
     */
    public abstract void internalRun() throws Exception;

    /**
     * This method has to be included in every script and handles the setup of the script by parsing the CommandLine
     * and setting the variables
     * @throws ParseException   If something goes wrong
     */
    public abstract void setup() throws ParseException;

    /**
     * This method will return if the script is allowed to execute in the given context. This is by default set
     * to the currentUser in the context being an admin, however this can be overwritten by each script individually
     * if different rules apply
     * @param context   The relevant DSpace context
     * @return          A boolean indicating whether the script is allowed to execute or not
     */
    public boolean isAllowedToExecute(Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            handler.logError("Error occured when trying to verify permissions for script: " + name);
        }
        return false;
    }
}
