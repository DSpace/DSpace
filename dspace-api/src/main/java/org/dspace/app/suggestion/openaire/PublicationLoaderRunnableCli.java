/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.openaire;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.dspace.utils.DSpace;

public class PublicationLoaderRunnableCli extends PublicationLoaderRunnable {

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public PublicationLoaderCliScriptConfiguration getScriptConfiguration() {
        PublicationLoaderCliScriptConfiguration configuration = new DSpace().getServiceManager()
                .getServiceByName("import-openaire-suggestions", PublicationLoaderCliScriptConfiguration.class);
        return configuration;
    }

    @Override
    public void setup() throws ParseException {
        super.setup();

        // in case of CLI we show the help prompt
        if (commandLine.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Import Researchers Suggestions", getScriptConfiguration().getOptions());
            System.exit(0);
        }
    }

}
