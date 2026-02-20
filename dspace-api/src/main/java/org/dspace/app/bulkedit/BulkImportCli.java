/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.sql.SQLException;

import org.apache.commons.cli.ParseException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;

/**
 * Extension of {@link BulkImport} for CLI.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class BulkImportCli extends BulkImport {

    @Override
    protected void assignCurrentUserInContext(Context context) throws ParseException {
        if (commandLine.hasOption('e')) {
            String ePersonEmail = commandLine.getOptionValue('e');
            try {
                EPerson ePerson =
                    EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, ePersonEmail);
                if (ePerson == null) {
                    super.handler.logError("EPerson not found: " + ePersonEmail);
                    throw new IllegalArgumentException("Unable to find a user with email: " + ePersonEmail);
                }
                context.setCurrentUser(ePerson);
            } catch (SQLException e) {
                throw new IllegalArgumentException("SQLException trying to find user with email: " + ePersonEmail);
            }
        } else {
            throw new ParseException("Required parameter -e missing!");
        }
    }

}
