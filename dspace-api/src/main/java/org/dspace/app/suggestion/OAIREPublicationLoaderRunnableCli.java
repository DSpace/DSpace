/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.dspace.utils.DSpace;

public class OAIREPublicationLoaderRunnableCli extends OAIREPublicationLoaderRunnable {

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public OAIREPublicationLoaderCliScriptConfiguration getScriptConfiguration() {
        OAIREPublicationLoaderCliScriptConfiguration configuration = new DSpace().getServiceManager()
                .getServiceByName("import-oaire-suggestions", OAIREPublicationLoaderCliScriptConfiguration.class);
        return configuration;
    }

    @Override
    public void setup() throws ParseException {
        super.setup();

        // in case of CLI we show the help prompt
        if (commandLine.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Import Readearchers Suggestions", getScriptConfiguration().getOptions());
            System.exit(0);
        }
    }

}
