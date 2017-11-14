/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CheckSumRest;
import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.app.rest.model.step.UploadBitstreamRest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service to manipulate in-progress submissions.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
@Component
public class SubmissionService {

	private static final Logger log = Logger.getLogger(SubmissionService.class);
	
	@Autowired
	protected ConfigurationService configurationService;
	@Autowired
	protected CollectionService collectionService;
	@Autowired
	protected WorkspaceItemService workspaceItemService;
	
	public WorkspaceItem createWorkspaceItem(Context context, Request request) {
		WorkspaceItem wsi = null;
		String collectionUUID = request.getHttpServletRequest().getParameter("collection");
		if(StringUtils.isBlank(collectionUUID)) 
		{
			String uuid = configurationService.getProperty("submission.default.collection");
			Collection collection = null;
			try {
			if(StringUtils.isNotBlank(uuid)) {
				collection = collectionService.find(context, UUID.fromString(uuid));
			}
			else {
				collection = collectionService.findAll(context, 1, 0).get(0);
			}
			wsi = workspaceItemService.create(context, collection, true);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		else {
			//TODO manage setup of default collection in the case WSI it is not null
			//TODO manage setup of collection discovered into request
		}
		return wsi;
	}
	
	public void saveWorkspaceItem(Context context, WorkspaceItem wsi) {
		try {
			workspaceItemService.update(context, wsi);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	

	public static UploadBitstreamRest buildUploadBitstream(ConfigurationService configurationService, Bitstream source) {
		UploadBitstreamRest b = new UploadBitstreamRest();
		List<MetadataEntryRest> metadata = new ArrayList<MetadataEntryRest>();
		for (MetadataValue mv : source.getMetadata()) {
			MetadataEntryRest me = new MetadataEntryRest();
			me.setKey(mv.getMetadataField().toString('.'));
			me.setValue(mv.getValue());
			me.setLanguage(mv.getLanguage());
			metadata.add(me);
		}
		b.setMetadata(metadata);
		b.setUuid(source.getID());
		CheckSumRest checksum = new CheckSumRest();
		checksum.setCheckSumAlgorithm(source.getChecksumAlgorithm());
		checksum.setValue(source.getChecksum());
		b.setCheckSum(checksum);
		b.setSizeBytes(source.getSize());
		b.setUrl(configurationService.getProperty("dspace.url")+"/api/"+BitstreamRest.CATEGORY +"/"+ English.plural(BitstreamRest.NAME) + "/" + source.getID() + "/content");
		return b;
	}
	
}
