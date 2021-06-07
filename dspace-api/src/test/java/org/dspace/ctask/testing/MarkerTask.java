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

import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

/**
 * Curation task to mark an Item so that we can see that it has been curated.
 * A provenance record is added, which contains the canonical name of this class.
 *
 * @author mwood
 */
public class MarkerTask
        extends AbstractCurationTask {
    public static final String SCHEMA = MetadataSchemaEnum.DC.getName();
    public static final String ELEMENT = "description";
    public static final String QUALIFIER = "provenance";
    public static final String LANGUAGE = null;

    @Override
    public int perform(DSpaceObject dso)
            throws IOException {
        if (dso instanceof Item) {
            Context context;
            try {
                context = Curator.curationContext();
            } catch (SQLException ex) {
                throw new IOException("Failed to get a Context:", ex);
            }

            Item item = (Item) dso;
            String marker = String.format("Marked by %s on %s",
                    MarkerTask.class.getCanonicalName(),
                    DCDate.getCurrent().toString());

            context.turnOffAuthorisationSystem();
            try {
                itemService.addMetadata(context, item,
                        SCHEMA, ELEMENT, QUALIFIER, LANGUAGE,
                        marker);
            } catch (SQLException ex) {
                throw new IOException("Failed to mark the Item:", ex);
            } finally {
                context.restoreAuthSystemState();
            }

            String result = String.format("Item %s marked.", item.getID().toString());
            setResult(result);
            report(result);

            return Curator.CURATE_SUCCESS;
        } else {
            return Curator.CURATE_SKIP;
        }
    }
}
