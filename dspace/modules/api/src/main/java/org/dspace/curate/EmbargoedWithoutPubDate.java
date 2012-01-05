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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Curation task to report embargoed items without publication date
 * 
 * @author pmidford
 * created Dec 9, 2011
 *
 */

@Suspendable
public class EmbargoedWithoutPubDate extends AbstractCurationTask {

    
    private int total;
    private int unpublishedCount;
    private List<DatedEmbargo> embargoes;
    private DatedEmbargo[] dummy = new DatedEmbargo[1];
    
    private static Logger LOGGER = LoggerFactory.getLogger(EmbargoedWithoutPubDate.class);
    

    @Override
    public void init(Curator curator, String taskID) throws IOException{
        super.init(curator, taskID);
    }
    
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        
        if (dso instanceof Collection){
            total = 0;
            unpublishedCount = 0;
            embargoes = new ArrayList<DatedEmbargo>();
            distribute(dso);
            if (!embargoes.isEmpty()){
                DatedEmbargo[] s = embargoes.toArray(dummy);
                Arrays.sort(s);
                this.report("Collection: " + dso.getName() + "; Total items = " + total + "; unpublished items = " + unpublishedCount); 
                for(DatedEmbargo d : s)
                    this.report(d.toString());
            }
            else if (total > 0)
                this.report("Collection: " + dso.getName() + "; Total items = " + total + "; no unpublished items"); 
        }
        return Curator.CURATE_SUCCESS;
    }

    @Override
    protected void performItem(Item item){
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
                if (itemEmbargoDate != null){
                    DatedEmbargo de = new DatedEmbargo(itemEmbargoDate.toDate(),item);
                    embargoes.add(de);
                }
            } catch (Exception e) {
                this.report("Exception " + e + " encountered while processing " + item);
            }
        }

    }
    
    private static class DatedEmbargo implements Comparable<DatedEmbargo>{

        private Date embargoDate;
        private Item embargoedItem;
        
        public DatedEmbargo(Date date, Item item) {
            embargoDate = date;
            embargoedItem = item;
        }

        @Override
        public int compareTo(DatedEmbargo o) {
            return embargoDate.compareTo(o.embargoDate);
        }
        
        @Override
        public String toString(){
            return embargoedItem.getName() + " " + embargoDate.toString();
        }
    }
}
