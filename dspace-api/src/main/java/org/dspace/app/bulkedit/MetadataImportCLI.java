/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.handler.DSpaceRunnableHandler;

/**
 * CLI variant for the {@link MetadataImport} class
 * This has been made so that we can specify the behaviour of the determineChanges method to be specific for the CLI
 */
public class MetadataImportCLI extends MetadataImport {

    @Override
    protected boolean determineChange(DSpaceRunnableHandler handler) throws IOException {
        handler.logInfo("Do you want to make these changes? [y/n] ");
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
            String yn = bufferedReader.readLine();
            if ("y".equalsIgnoreCase(yn)) {
                return true;
            }
            return false;
        }
    }

    @Override
    protected void assignCurrentUserInContext(Context context) throws ParseException {
        try {
            if (commandLine.hasOption('e')) {
                EPerson eperson;
                String e = commandLine.getOptionValue('e');
                if (e.indexOf('@') != -1) {
                    eperson = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, e);
                } else {
                    eperson = EPersonServiceFactory.getInstance().getEPersonService().find(context, UUID.fromString(e));
                }

                if (eperson == null) {
                    throw new ParseException("Error, eperson cannot be found: " + e);
                }
                context.setCurrentUser(eperson);
            }
        } catch (Exception e) {
            throw new ParseException("Unable to find DSpace user: " + e.getMessage());
        }
    }

    @Override
    public void setup() throws ParseException {
        super.setup();
        if (!commandLine.hasOption('e')) {
            throw new ParseException("Required parameter -e missing!");
        }
    }
}
