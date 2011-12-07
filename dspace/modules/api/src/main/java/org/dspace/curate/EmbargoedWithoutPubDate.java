package org.dspace.curate;


import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamIngestionCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.doi.CDLDataCiteService;
import org.dspace.identifier.DOIIdentifierProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class EmbargoedWithoutPubDate extends AbstractCurationTask {

    @Override
    public void init(Curator curator, String taskID) throws IOException{
        
    }
    
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        
        if (dso instanceof Item){
            Item item = (Item)dso;
            
            
        }
        else if (dso instanceof Collection){
            
        }
        
        
        return Curator.CURATE_SUCCESS;
    }

}
