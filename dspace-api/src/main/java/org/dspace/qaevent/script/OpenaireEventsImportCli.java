/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.script;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.dspace.utils.DSpace;

/**
 * Extensions of {@link OpenaireEventsImport} to run the script on console.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class OpenaireEventsImportCli extends OpenaireEventsImport {

    @Override
    @SuppressWarnings({ "rawtypes" })
    public OpenaireEventsImportCliScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("import-openaire-events", OpenaireEventsImportCliScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        super.setup();

        // in case of CLI we show the help prompt
        if (commandLine.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Import Notification event json file", getScriptConfiguration().getOptions());
            System.exit(0);
        }

    }

}
