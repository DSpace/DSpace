package edu.tamu.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.eperson.Group;

public class ReportRestrictedItems extends AbstractCurationTask {

    private int result = Curator.CURATE_SUCCESS;
    private StringBuilder sb = new StringBuilder();

    @Override
    public void init(Curator curator, String taskId) throws IOException {
        sb = new StringBuilder();
        super.init(curator, taskId);
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        if (dso.getType() == Constants.SITE) {
            sb.append("Cannot perform this task at site level.");
            this.setResult(sb.toString());
            return Curator.CURATE_FAIL;
        } else {
            distribute(dso);
        }

        this.setResult(sb.toString());
        return result;
    }

    @Override
    protected void performItem(Item item) throws SQLException, IOException {
        Context context = new Context();

        try {
            boolean restrcitedItem;
            if (AuthorizeManager.authorizeActionBoolean(context, item, Constants.READ))
                restrcitedItem = false;
            else
                restrcitedItem = true;

            int restrictedBitstreams = 0;
            for (Bundle bundle : item.getBundles()) {
                for (Bitstream bitstream : bundle.getBitstreams()) {
                    if (!AuthorizeManager.authorizeActionBoolean(context, bitstream, Constants.READ))
                        restrictedBitstreams++;
                }
            }
            if (restrcitedItem || restrictedBitstreams > 0) {
                sb.append(item.getHandle() + ": ");

                if (restrcitedItem)
                    sb.append("item restricted");
                else
                    sb.append("item open");

                sb.append(", " + restrictedBitstreams + " restricted bitstreams.\n");
            }
        } finally {
            if (context != null)
                context.complete();
        }
    }

}
