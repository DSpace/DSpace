/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.CollectionServiceImpl;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.model.Request;
import org.dspace.submit.AbstractProcessingStep;
import org.springframework.beans.factory.annotation.Autowired;

public class SelectCollectionStep extends AbstractProcessingStep
{
	
	private static final Logger log = Logger.getLogger(SelectCollectionStep.class);
	
	@Override
	public void doProcessing(Context context, Request req) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doPostProcessing(Context context, Request obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doPreProcessing(Context context, WorkspaceItem wsi) {
		if(wsi == null) {
			String uuid = configurationService.getProperty("submission.default.collection");						
			try {
				Collection collection = collectionService.find(context, UUID.fromString(uuid));
				wsi = workspaceItemService.create(context, collection, true);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			   
		}
	}
}
