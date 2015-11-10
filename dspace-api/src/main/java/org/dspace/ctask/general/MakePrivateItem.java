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

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import java.io.IOException;

import static org.dspace.curate.Curator.*;

/**
 * MakePrivateItem is pretty straightforward, it simply sets the
 * discoverable flag to false for all items it touches
 * rendering them "private"
 *
 * @author hardyoyo
 */

public class MakePrivateItem extends AbstractCurationTask
{
    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException  {
        if (dso.getType() == Constants.ITEM) {
                Item item = (Item)dso;

                // no time to explain, HIDE!
                item.setDiscoverable(false);
                return CURATE_SUCCESS;

        } else {
            return CURATE_SKIP;
        }
    }
}
