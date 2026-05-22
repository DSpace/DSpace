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
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;

@Distributive
public class NoOpCurationDistributeTask extends AbstractCurationTask {

    protected int status = Curator.CURATE_UNSET;
    protected String result = null;

    @Override
    public int perform(Context context, DSpaceObject dso) throws IOException {
        distribute(context, dso);
        return status;
    }

    @Override
    protected void performItem(Context context, Item item) throws SQLException, IOException {
        status = Curator.CURATE_SUCCESS;
        result = "No operation performed on " + item.getHandle();

        setResult(result);
        report(result);
    }
}
