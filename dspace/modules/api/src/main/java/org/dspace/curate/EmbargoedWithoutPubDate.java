package org.dspace.curate;


import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.crosswalk.StreamIngestionCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.doi.CDLDataCiteService;
import org.dspace.embargo.EmbargoManager;
import org.dspace.identifier.DOIIdentifierProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Curation task to report embargoed items without publication date
 * 
 * @author pmidford
 * created Dec 9, 2011
 *
 */

//@Suspendable
public class EmbargoedWithoutPubDate extends AbstractCurationTask {

    private static Logger LOGGER = LoggerFactory.getLogger(EmbargoedWithoutPubDate.class);
    

    @Override
    public void init(Curator curator, String taskID) throws IOException{
        super.init(curator, taskID);
    }
    
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        int total = 0;
        int unpublishedCount = 0;
        
        if (dso instanceof Collection){
            try {
                ItemIterator iit = ((Collection)dso).getAllItems();
                while (iit.hasNext()){
                    Item item = iit.next();
                    total++;
                    boolean unpublished = false;
                    DCDate itemPubDate;
                    DCValue values[] = item.getMetadata("dc", "date", "available", Item.ANY);
                    if (values== null || values.length==0){ //no available date - save and report
                        unpublished = true;
                    }
                    
                    DCDate itemEmbargoDate = null;
                    if (unpublished){
                        unpublishedCount++;
                        try {  //want to continue if a problem comes up
                            itemEmbargoDate = EmbargoManager.getEmbargoDate(null, item);
                        } catch (Exception e) {
                            this.report("Exception " + e + " encountered while processing " + item);
                            //return Curator.CURATE_ERROR;
                        }
                    }

                }
                
            } catch (SQLException e) {
                this.report("Failed on SQL Error");
                //return Curator.CURATE_ERROR;
            }
        }

        this.setResult("Total items = " + total + "; unpublished items = " + unpublishedCount);
        return Curator.CURATE_SUCCESS;
    }

}
