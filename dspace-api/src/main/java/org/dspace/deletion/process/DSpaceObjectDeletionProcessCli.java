/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deletion.process;

import java.sql.SQLException;

import org.apache.commons.cli.ParseException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;

/**
 * Extension of {@link DSpaceObjectDeletionProcess} for CLI execution.
 * This class enables command-line execution of the deletion process with
 * explicit user specification via the -e (email) parameter.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 *
 */
public class DSpaceObjectDeletionProcessCli extends DSpaceObjectDeletionProcess {

    /**
     * Assigns the current user to the context based on the email parameter for CLI execution.
     *
     * This method overrides the parent implementation to support command-line execution
     * with explicit user specification via the -e (email) parameter. This is required for
     * CLI scenarios where the script is invoked directly from the command line rather than
     * through the REST API or other programmatic interfaces.
     *
     * Process:
     * <ul>
     *   <li>Creates a new DSpace Context instance</li>
     *   <li>Validates that the -e (email) parameter is provided</li>
     *   <li>Looks up the EPerson by email address using EPersonService</li>
     *   <li>Validates that the EPerson exists in the system</li>
     *   <li>Sets the found EPerson as the current user in the context</li>
     * </ul>
     *
     * Usage Example:
     * <pre>
     * ./dspace object-deletion -e admin@example.com -i [object-uuid]
     * </pre>
     *
     * @throws SQLException             if a database error occurs during EPerson lookup
     * @throws ParseException           if the required -e parameter is missing from command line
     * @throws IllegalArgumentException if the provided email address doesn't match any existing EPerson
     */
    @Override
    protected void assignCurrentUserInContext() throws SQLException, ParseException {
        this.context = new org.dspace.core.Context();
        if (commandLine.hasOption('e')) {
            String ePersonEmail = commandLine.getOptionValue('e');
            EPerson ePerson =
                EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, ePersonEmail);
            if (ePerson == null) {
                super.handler.logError("EPerson not found: " + ePersonEmail);
                throw new IllegalArgumentException("Unable to find a user with email: " + ePersonEmail);
            }
            context.setCurrentUser(ePerson);
        } else {
            throw new ParseException("Required parameter -e missing!");
        }
    }

    @Override
    public DSpaceObjectDeletionProcessScriptConfiguration<DSpaceObjectDeletionProcess> getScriptConfiguration() {
        org.dspace.kernel.ServiceManager sm = new org.dspace.utils.DSpace().getServiceManager();
        return sm.getServiceByName(OBJECT_DELETION_SCRIPT, DSpaceObjectDeletionProcessCliScriptConfiguration.class);
    }

}
