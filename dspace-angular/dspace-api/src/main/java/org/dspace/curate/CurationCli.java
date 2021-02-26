/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.sql.SQLException;

import org.apache.commons.cli.ParseException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * This is the CLI version of the {@link Curation} script.
 * This will only be called when the curate script is called from a commandline instance.
 */
public class CurationCli extends Curation {

    /**
     * This is the overridden instance of the {@link Curation#assignCurrentUserInContext()} method in the parent class
     * {@link Curation}.
     * This is done so that the CLI version of the Script is able to retrieve its currentUser from the -e flag given
     * with the parameters of the Script.
     * @throws ParseException   If the e flag was not given to the parameters when calling the script
     */
    @Override
    protected void assignCurrentUserInContext() throws ParseException {
        if (this.commandLine.hasOption('e')) {
            String ePersonEmail = this.commandLine.getOptionValue('e');
            this.context = new Context(Context.Mode.BATCH_EDIT);
            try {
                EPerson ePerson = ePersonService.findByEmail(this.context, ePersonEmail);
                if (ePerson == null) {
                    super.handler.logError("EPerson not found: " + ePersonEmail);
                    throw new IllegalArgumentException("Unable to find a user with email: " + ePersonEmail);
                }
                this.context.setCurrentUser(ePerson);
            } catch (SQLException e) {
                throw new IllegalArgumentException("SQLException trying to find user with email: " + ePersonEmail);
            }
        } else {
            throw new ParseException("Required parameter -e missing!");
        }
    }
}
