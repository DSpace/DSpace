package edu.tamu.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BundleService;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

public class DeleteThumbnails extends AbstractCurationTask {

    private int result;

    private StringBuilder sb;

    private int numberOfItemsToCurate;

    private int currentItemCount;

    private int currentImageCount;

    private String topHandle;

    private String topCurationType;
    
    private BundleService bundleService;

    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);
        result = Curator.CURATE_SUCCESS;
        sb = new StringBuilder();
        numberOfItemsToCurate = currentItemCount = currentImageCount = 0;
        
        bundleService = ContentServiceFactory.getInstance().getBundleService();

    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        switch (dso.getType()) {
        case Constants.SITE:
            sb.append("Cannot perform this task at site level.");
            setResult(sb.toString());
            result = Curator.CURATE_FAIL;
            break;
        case Constants.COMMUNITY:
            if (numberOfItemsToCurate == 0) {
                topCurationType = "Community";
                Community community = (Community) dso;
                // get community handle for reporting
                topHandle = community.getHandle();
                try {
                    // count items of community to know when all have been curated to set results
                    numberOfItemsToCurate = itemService.countItems(Curator.curationContext(),community);
                } catch (SQLException e) {
                    sb.append("Failed to count items on community: " + topHandle + "\nAborting...");
                    result = Curator.CURATE_ERROR;
                    setResult(sb.toString());
                }
            }
            break;
        case Constants.COLLECTION:
            if (numberOfItemsToCurate == 0) {
                topCurationType = "Collection";
                Collection collection = (Collection) dso;
                // get collection handle for reporting
                topHandle = collection.getHandle();
                try {
                    // count items of collection to know when all have been curated to set results
                    numberOfItemsToCurate = itemService.countItems(Curator.curationContext(),collection);
                } catch (SQLException e) {
                    sb.append("Failed to count items on collection: " + topHandle + "\nAborting...");
                    result = Curator.CURATE_ERROR;
                    setResult(sb.toString());
                }
            }
            break;
        case Constants.ITEM:
            if (numberOfItemsToCurate == 0) {
                numberOfItemsToCurate = 1;
            }
            Item item = (Item) dso;
            // increment current item count
            currentItemCount++;
            try {
                int count = 0;
                // remove bitstrams from all bundles and count them
                
                List<Bitstream> removableBitstreams = new ArrayList<Bitstream>();
                Bundle removableBundle = null;
                for (Bundle bundle : item.getBundles()) {
                	if (bundle.getName().equals("THUMBNAIL")) {
	                    for (Bitstream bitstream : bundle.getBitstreams()) {
                        	removableBitstreams.add(bitstream);
	                        count++;
	                    }
	                    removableBundle = bundle;
	                    break;
                	}
                }
                for (Bitstream bitstream: removableBitstreams) {
                	bundleService.removeBitstream(Curator.curationContext(), removableBundle, bitstream);
                }
                itemService.removeBundle(Curator.curationContext(), item, removableBundle);
                itemService.update(Curator.curationContext(),item);
                // accumulate images removed count
                currentImageCount += count;
                sb.append("Item: " + item.getHandle() + ": " + count + " images deleted.\n");
                if (currentItemCount == numberOfItemsToCurate) {
                    if (topCurationType != null) {
                        sb.append(topCurationType + ": " + topHandle + ": " + currentItemCount + " items curated.\n");
                        sb.append(topCurationType + ": " + topHandle + ": " + currentImageCount + " images deleted.\n");
                    }
                    setResult(sb.toString());
                }
            } catch (SQLException e) {
                sb.append("Failed to persist change on item: " + item.getHandle() + "\nAborting...");
                result = Curator.CURATE_ERROR;
                setResult(sb.toString());
            } catch (AuthorizeException e) {
                sb.append("Authorization failure on item: " + item.getHandle() + "\nAborting...");
                result = Curator.CURATE_ERROR;
                setResult(sb.toString());
            }
            break;
        default:
            break;
        }
        return result;
    }

}
