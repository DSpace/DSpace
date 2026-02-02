/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;

/**
 * Extension of {@link BulkAccessControl} for CLI.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
public class BulkAccessControlCli extends BulkAccessControl {

    @Override
    protected void setEPerson(Context context) throws SQLException {
        EPerson myEPerson;
        eperson = commandLine.getOptionValue('e');

        if (eperson == null) {
            handler.logError("An eperson to do the the Bulk Access Control must be specified " +
                "(run with -h flag for details)");
            throw new UnsupportedOperationException("An eperson to do the Bulk Access Control must be specified");
        }

        if (StringUtils.contains(eperson, '@')) {
            myEPerson = epersonService.findByEmail(context, eperson);
        } else {
            myEPerson = epersonService.find(context, UUID.fromString(eperson));
        }

        if (myEPerson == null) {
            handler.logError("EPerson cannot be found: " + eperson + " (run with -h flag for details)");
            throw new UnsupportedOperationException("EPerson cannot be found: " + eperson);
        }

        context.setCurrentUser(myEPerson);
    }

    @Override
    protected boolean isAuthorized(Context context) {

        if (context.getCurrentUser() == null) {
            return false;
        }

        return getScriptConfiguration().isAllowedToExecute(context,
            Arrays.stream(commandLine.getOptions())
                  .map(option ->
                      new DSpaceCommandLineParameter("-" + option.getOpt(), option.getValue()))
                  .collect(Collectors.toList()));
    }
}
