/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import java.io.IOException;

public class NoOpCurationTask extends AbstractCurationTask
{

    protected int status = Curator.CURATE_UNSET;
    protected String result = null;

    @Override
    public int perform(DSpaceObject dso) throws IOException
    {

		if (dso instanceof Item)
        {
            Item item = (Item)dso;
            status = Curator.CURATE_SUCCESS;
            result = "No operation performed on " + item.getHandle();
            
            setResult(result);
            report(result);
		}
        
        return status;
    }


}
