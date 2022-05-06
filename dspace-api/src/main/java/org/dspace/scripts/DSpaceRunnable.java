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
 * This class will contain the common logic needed to run a "script".  It'll
 * fetch the information that it needs from the {@link ScriptConfiguration}
 * provided through the diamond operators.  This will be the DSpaceRunnable
 * class for the {@link ScriptConfiguration} beans. Specifically sub-classed for
 * each script.
 * @param <T>
 */
public abstract class DSpaceRunnable<T extends ScriptConfiguration> implements Runnable {

    /**
     * The CommandLine object for the script, holding the options given for the
     * current invocation of the script.
     */
    protected CommandLine commandLine;

    /**
     * The minimal CommandLine object for the script that'll hold help information
     */
    protected CommandLine helpCommandLine;

    /**
     * This EPerson identifier variable is the UUID of the EPerson that's
     * running the script.
     */
    private UUID epersonIdentifier;

    /**
     * The handler that deals with this script. This handler can currently be
     * either a {@link org.dspace.scripts.handler.impl.RestDSpaceRunnableHandler}
     * or a {@link org.dspace.scripts.handler.impl.CommandLineDSpaceRunnableHandler}
     * depending from where the script is called.  It has useful methods for
     * communicating with the user.
     */
    protected DSpaceRunnableHandler handler;

    /**
     * Get the Configuration that the implementing DSpaceRunnable uses.
     * @return  The {@link ScriptConfiguration} for the concrete subclass.
     */
    public abstract T getScriptConfiguration();


    private void setHandler(DSpaceRunnableHandler dSpaceRunnableHandler) {
        this.handler = dSpaceRunnableHandler;
    }

    /**
     * This method sets the appropriate DSpaceRunnableHandler depending on where
     * it was run from and parses the arguments given to the script.
     * @param args                  The arguments given to the script
     * @param dSpaceRunnableHandler The DSpaceRunnableHandler object that
     *                              defines from where the script was run
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
     * Parse the array of String objects that represent the parameters given to
     * the tool into a CommandLine object that can be used by the script to
     * retrieve options.
     * @param args              The Strings representing the parameters
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
     * Called by {@link initialize} after the arguments have been parsed.  Your
     * subclass can use this to analyze options and do other initialization.
     * @throws ParseException   If something goes wrong
     */
    public abstract void setup() throws ParseException;

    /**
     * Handle the running of the script and all the database modifications
     * needed for the Process object that resulted from this script.
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
     * The main execution block for the script.  Called by {@link run()}.
     * @throws Exception    If something goes wrong
     */
    public abstract void internalRun() throws Exception;

    /**
     * This method will call upon the {@link DSpaceRunnableHandler#printHelp(Options, String)}
     * method with the script's options and name.
     */
    public void printHelp() {
        handler.printHelp(getScriptConfiguration().getOptions(), getScriptConfiguration().getName());
    }

    /**
     * Traverse all the options and grab options defined as an InputStream type to then save
     * the filename specified by that option in a list of Strings that'll be returned in the end.
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
     * Get the UUID of the EPerson that's running the script.
     * @return the epersonIdentifier value of this DSpaceRunnable
     */
    public UUID getEpersonIdentifier() {
        return epersonIdentifier;
    }

    /**
     * Set the UUID of the EPerson that's running the script.
     * @param epersonIdentifier   The epersonIdentifier to be set on this DSpaceRunnable
     */
    public void setEpersonIdentifier(UUID epersonIdentifier) {
        this.epersonIdentifier = epersonIdentifier;
    }

    public enum StepResult {
        Continue, Exit;
    }
}
