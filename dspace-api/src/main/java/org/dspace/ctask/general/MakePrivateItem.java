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

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;
import java.util.ArrayList;
import java.util.List;

/**
 * MakePrivateItem is a task that sets the discoverable flag for item DSOs to false, making the item "private"
 *
 * @author hardyoyo
 */
@Distributive
public class MakePrivateItem extends AbstractCurationTask
{
	private List<String> results;


    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException
    {
        distribute(dso);
		formatResults();
	    return Curator.CURATE_SUCCESS;
    }
    
    @Override
    protected void performItem(Item item) throws SQLException, IOException
    {

		// set the discoverable flag
		// no time to explain, HIDE!
		item.setDiscoverable(false);

		// update the item
		try {
			item.update();
			addResult(item, "success", "Set discoverable to false" );
			return;

		} catch (Exception e) {

			addResult(item, "error", "Unable to save results");
			return;
    	}
 
	}

		private void addResult(Item item, String status, String message) {
			results.add(item.getHandle() + " (" + status + ") " + message);
		}   

		private void formatResults() {
			StringBuilder outputResult = new StringBuilder();
			for(String result : results) {
				outputResult.append(result).append("\n");
			}
			setResult(outputResult.toString());
		}

}
