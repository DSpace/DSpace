/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.runnable;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.utils.DSpace;

/**
 * CLI implementation of the {@link PublicationLoaderRunnable} script.
 * This class extends {@link PublicationLoaderRunnable} and provides additional
 * functionality specific to command-line execution.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class PublicationLoaderRunnableCli extends PublicationLoaderRunnable {

    /**
     * Retrieves the script configuration associated with this CLI script.
     * This method fetches the configuration from the DSpace service manager.
     *
     * @return The {@link ScriptConfiguration} instance for the import-loader-suggestions script.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ScriptConfiguration<?> getScriptConfiguration() {
        return new DSpace().getServiceManager()
                           .getServiceByName("import-loader-suggestions", ScriptConfiguration.class);
    }


    /**
     * Sets up the script execution environment.
     * This method also checks for the presence of the help option and, if found,
     * displays usage instructions before terminating the script.
     *
     * @throws ParseException If there is an error parsing the command-line options.
     */
    @Override
    public void setup() throws ParseException {
        super.setup();

        // in case of CLI we show the help prompt
        if (commandLine.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Imports suggestions from external providers for publication claim",
                                getScriptConfiguration().getOptions());
            System.exit(0);
        }
    }
}
