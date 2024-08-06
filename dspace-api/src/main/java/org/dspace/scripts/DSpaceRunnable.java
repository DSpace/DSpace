/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.cli.DSpaceSkipUnknownArgumentsParser;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.handler.DSpaceRunnableHandler;

/**
 * This is the class that should be extended for each Script. This class will contain the logic needed to run and it'll
 * fetch the information that it needs from the {@link ScriptConfiguration} provided through the diamond operators.
 * This will be the dspaceRunnableClass for the {@link ScriptConfiguration} beans. Specifically created for each
 * script
 * @param <T>
 */
public abstract class DSpaceRunnable<T extends ScriptConfiguration> implements Runnable {

    /**
     * The CommandLine object for the script that'll hold the information
     */
    protected CommandLine commandLine;

    /**
     * The minimal CommandLine object for the script that'll hold help information
     */
    protected CommandLine helpCommandLine;

    /**
     * This EPerson identifier variable is the UUID of the EPerson that's running the script
     */
    private UUID epersonIdentifier;

    /**
     * The handler that deals with this script. This handler can currently either be a RestDSpaceRunnableHandler or
     *  a CommandlineDSpaceRunnableHandler depending from where the script is called
     */
    protected DSpaceRunnableHandler handler;

    /**
     * This method will return the Configuration that the implementing DSpaceRunnable uses
     * @return  The {@link ScriptConfiguration} that this implementing DspaceRunnable uses
     */
    public abstract T getScriptConfiguration();


    private void setHandler(DSpaceRunnableHandler dSpaceRunnableHandler) {
        this.handler = dSpaceRunnableHandler;
    }

    /**
     * This method sets the appropriate DSpaceRunnableHandler depending on where it was ran from and it parses
     * the arguments given to the script
     * @param args                  The arguments given to the script
     * @param dSpaceRunnableHandler The DSpaceRunnableHandler object that defines from where the script was ran
     * @param currentUser
     * @return the result of this step; StepResult.Continue: continue the normal process,
     * initialize is successful; otherwise exit the process (the help or version is shown)
     * @throws ParseException       If something goes wrong
     */
    public StepResult initialize(String[] args, DSpaceRunnableHandler dSpaceRunnableHandler,
                           EPerson currentUser) throws ParseException {
        if (currentUser != null) {
            this.setEpersonIdentifier(currentUser.getID());
        }
        this.setHandler(dSpaceRunnableHandler);

        // parse the command line in a first step for the help options
        // --> no other option is required
        StepResult result = this.parseForHelp(args);
        switch (result) {
            case Exit:
                // arguments of the command line matches the help options, handle this
                handleHelpCommandLine();
                break;

            case Continue:
                // arguments of the command line matches NOT the help options, parse the args for the normal options
                result = this.parse(args);
                break;
            default:
                break;
        }

        return result;
    }


    /**
     * This method handle the help command line. In this easy implementation only the help is printed. For more
     * complexity override this method.
     */
    private void handleHelpCommandLine() {
        printHelp();
    }


    /**
     * This method will take the primitive array of String objects that represent the parameters given to the String
     * and it'll parse these into a CommandLine object that can be used by the script to retrieve the data
     * @param args              The primitive array of Strings representing the parameters
     * @throws ParseException   If something goes wrong
     */
    private StepResult parse(String[] args) throws ParseException {
        commandLine = new DefaultParser().parse(getScriptConfiguration().getOptions(), args);
        setup();
        return StepResult.Continue;
    }

    private StepResult parseForHelp(String[] args) throws ParseException {
        helpCommandLine = new DSpaceSkipUnknownArgumentsParser().parse(getScriptConfiguration().getHelpOptions(), args);
        if (helpCommandLine.getOptions() != null && helpCommandLine.getOptions().length > 0) {
            return StepResult.Exit;
        }

        return StepResult.Continue;
    }

    /**
     * This method has to be included in every script and handles the setup of the script by parsing the CommandLine
     * and setting the variables
     * @throws ParseException   If something goes wrong
     */
    public abstract void setup() throws ParseException;

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

    /**
     * This method has to be included in every script and this will be the main execution block for the script that'll
     * contain all the logic needed
     * @throws Exception    If something goes wrong
     */
    public abstract void internalRun() throws Exception;

    /**
     * This method will call upon the {@link DSpaceRunnableHandler#printHelp(Options, String)} method with the script's
     * options and name
     */
    public void printHelp() {
        handler.printHelp(getScriptConfiguration().getOptions(), getScriptConfiguration().getName());
    }

    /**
     * This method will traverse all the options and it'll grab options defined as an InputStream type to then save
     * the filename specified by that option in a list of Strings that'll be returned in the end
     * @return  The list of Strings representing filenames from the options given to the script
     */
    public List<String> getFileNamesFromInputStreamOptions() {
        List<String> fileNames = new ArrayList<>();

        for (Option option : getScriptConfiguration().getOptions().getOptions()) {
            if (option.getType() == InputStream.class &&
                StringUtils.isNotBlank(commandLine.getOptionValue(option.getOpt()))) {
                fileNames.add(commandLine.getOptionValue(option.getOpt()));
            }
        }

        return fileNames;
    }

    /**
     * Generic getter for the epersonIdentifier
     * This EPerson identifier variable is the uuid of the eperson that's running the script
     * @return the epersonIdentifier value of this DSpaceRunnable
     */
    public UUID getEpersonIdentifier() {
        return epersonIdentifier;
    }

    /**
     * Generic setter for the epersonIdentifier.
     * This EPerson identifier variable is the UUID of the EPerson that's running the script.
     * @param epersonIdentifier   The epersonIdentifier to be set on this DSpaceRunnable
     */
    public void setEpersonIdentifier(UUID epersonIdentifier) {
        this.epersonIdentifier = epersonIdentifier;
    }

    public enum StepResult {
        Continue, Exit;
    }
}
