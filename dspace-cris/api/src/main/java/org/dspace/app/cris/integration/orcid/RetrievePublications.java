/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.integration.orcid;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.dspace.app.cris.batch.OrcidFeed;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import gr.ekt.bte.exceptions.BadTransformationSpec;
import gr.ekt.bte.exceptions.MalformedSourceException;

public class RetrievePublications extends AORCIDWebHookCallbackProcessor {
    
    /** Logger */
    private static Logger log = Logger.getLogger(RetrievePublications.class);

    private Integer collectionID;
    
    private String status = "p";
    
	@Override
	public boolean processChange(Context context, ResearcherPage rp, String orcid, HttpServletRequest req) {
	    try
        {
	        Integer epersonIDInteger = rp.getEpersonID();
	        if(collectionID==null) {
	            log.info("CollectionID not configured into orcid-webhooks.xml try to retrieve the first collection from findAll");
	            collectionID = Collection.findAll(context)[0].getID();
	            log.info("Found collectionID:" + collectionID);
	        }
	        if(epersonIDInteger!=null) {
	            OrcidFeed.retrievePublication(context, EPerson.find(context, epersonIDInteger), collectionID, true, orcid, status);    
	        }
            
        }
        catch (SQLException | BadTransformationSpec | MalformedSourceException | HttpException | IOException e)
        {
            log.error(e.getMessage());
        }
	    return true;
	}

    public void setCollectionID(Integer collectionID)
    {
        this.collectionID = collectionID;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getCollectionID()
    {
        return collectionID;
    }

}
