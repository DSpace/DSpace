/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.testing;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

/**
 * Curation task to mark an Item only if it is already archived and has a
 * handle, so that we can see that it has been curated after being archived.
 * A provenance record is added, which contains the canonical name of this class.
 */
public class ArchivedTask
        extends AbstractCurationTask {
    @Override
    public int perform(Context context, DSpaceObject dso)
            throws IOException {
        if (dso instanceof Item) {
            Item item = (Item) dso;
            if (!item.isArchived() || item.getHandle() == null) {
                setResult(String.format("Item %s is not archived.", item.getID().toString()));
                return Curator.CURATE_FAIL;
            }
            String marker = String.format("Archived with handle %s, marked by %s",
                    item.getHandle(), ArchivedTask.class.getCanonicalName());

            context.turnOffAuthorisationSystem();
            try {
                itemService.addMetadata(context, item,
                        MarkerTask.SCHEMA, MarkerTask.ELEMENT, MarkerTask.QUALIFIER, MarkerTask.LANGUAGE,
                        marker);
            } catch (SQLException ex) {
                throw new IOException("Failed to mark the Item:", ex);
            } finally {
                context.restoreAuthSystemState();
            }

            return Curator.CURATE_SUCCESS;
        } else {
            return Curator.CURATE_SKIP;
        }
    }
}
