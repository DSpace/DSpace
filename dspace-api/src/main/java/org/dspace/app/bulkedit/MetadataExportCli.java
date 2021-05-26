/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.apache.commons.cli.ParseException;

public class MetadataExportCli extends MetadataExport {

    @Override
    protected String getFileNameForExportFile() {
        return commandLine.getOptionValue('f');
    }

    @Override
    public void setup() throws ParseException {
        super.setup();
        // Check a filename is given
        if (!commandLine.hasOption('f')) {
            throw new ParseException("Required parameter -f missing!");
        }
    }

    @Override
    protected void logHelpInfo() {
        handler.logInfo("\nfull export: metadata-export -f filename");
        handler.logInfo("partial export: metadata-export -i handle -f filename");
    }
}
