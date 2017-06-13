package edu.tamu.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

public class ReportRestrictedItems extends AbstractCurationTask {

    private int result = Curator.CURATE_SUCCESS;
    private StringBuilder sb = new StringBuilder();
    
    private AuthorizeService authorizeService;

    @Override
    public void init(Curator curator, String taskId) throws IOException {
        sb = new StringBuilder();
        super.init(curator, taskId);
        authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        if (dso.getType() == Constants.SITE) {
            sb.append("Cannot perform this task at site level.");
            this.setResult(sb.toString());
            return Curator.CURATE_FAIL;
        } else {
            distribute(dso);
            this.setResult(sb.toString());
            return result;
        }

    }

    @Override
    protected void performItem(Item item) throws SQLException, IOException {
        Context context = Curator.curationContext();
        
        boolean restrictedItem;
		if (authorizeService.authorizeActionBoolean(context, item, Constants.READ))
		    restrictedItem = false;
		else
		    restrictedItem = true;

		int restrictedBitstreams = 0;
		for (Bundle bundle : item.getBundles()) {
		    for (Bitstream bitstream : bundle.getBitstreams()) {
		        if (!authorizeService.authorizeActionBoolean(context, bitstream, Constants.READ))
		            restrictedBitstreams++;
		    }
		}
		if (restrictedItem || restrictedBitstreams > 0) {
		    sb.append(item.getHandle() + ": ");

		    if (restrictedItem)
		        sb.append("item restricted");
		    else
		        sb.append("item open");

		    sb.append(", " + restrictedBitstreams + " restricted bitstreams.\n");
		}
    }

}
