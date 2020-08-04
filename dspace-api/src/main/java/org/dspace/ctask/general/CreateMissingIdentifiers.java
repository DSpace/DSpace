/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensure that an object has all of the identifiers that it should, minting them
 * as necessary.
 *
 * @author Mark H. Wood {@literal <mwood@iupui.edu>}
 */
public class CreateMissingIdentifiers
        extends AbstractCurationTask {
    private static final Logger LOG
            = LoggerFactory.getLogger(CreateMissingIdentifiers.class);

    @Override
    public int perform(DSpaceObject dso)
            throws IOException {
        // Only some kinds of model objects get identifiers
        if (!(dso instanceof Item)) {
            return Curator.CURATE_SKIP;
        }

        String typeText = Constants.typeText[dso.getType()];

        // Get a Context
        Context context;
        try {
            context = Curator.curationContext();
        } catch (SQLException ex) {
            report("Could not get the curation Context:  " + ex.getMessage());
            return Curator.CURATE_ERROR;
        }

        // Find the IdentifierService implementation
        IdentifierService identifierService = new DSpace()
                .getServiceManager()
                .getServiceByName(null, IdentifierService.class);

        // Register any missing identifiers.
        try {
            identifierService.register(context, dso);
        } catch (AuthorizeException | IdentifierException | SQLException ex) {
            String message = ex.getMessage();
            report(String.format("Identifier(s) not minted for %s %s:  %s%n",
                    typeText, dso.getID().toString(), message));
            LOG.error("Identifier(s) not minted:  {}", message);
            return Curator.CURATE_ERROR;
        }

        // Success!
        report(String.format("%s %s registered.%n",
                typeText, dso.getID().toString()));
        return Curator.CURATE_SUCCESS;
    }
}
