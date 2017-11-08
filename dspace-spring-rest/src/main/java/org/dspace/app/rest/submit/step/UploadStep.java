/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.ArrayList;
import java.util.List;

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CheckSumRest;
import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.app.rest.model.step.DataUpload;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;

public class UploadStep extends org.dspace.submit.step.UploadStep implements AbstractRestProcessingStep {

	@Override
	public DataUpload getData(WorkspaceItem obj, SubmissionStepConfig config) throws Exception {
		
		DataUpload result = new DataUpload(); 
		List<Bundle> bundles = itemService.getBundles(obj.getItem(), Constants.CONTENT_BUNDLE_NAME);
		for(Bundle bundle : bundles) {
			for(Bitstream source : bundle.getBitstreams()) {
				DataUpload.UploadBitstreamRest b = result.createUploadBitstreamRest();
				List<MetadataEntryRest> metadata = new ArrayList<MetadataEntryRest>();
				for (MetadataValue mv : source.getMetadata()) {
					MetadataEntryRest me = new MetadataEntryRest();
					me.setKey(mv.getMetadataField().toString('.'));
					me.setValue(mv.getValue());
					me.setLanguage(mv.getLanguage());
					metadata.add(me);
				}
				b.setMetadata(metadata);
				CheckSumRest checksum = new CheckSumRest();
				checksum.setCheckSumAlgorithm(source.getChecksumAlgorithm());
				checksum.setValue(source.getChecksum());
				b.setCheckSum(checksum);
				b.setSizeBytes(source.getSize());
				b.setUrl(configurationService.getProperty("dspace.url")+"/api/"+BitstreamRest.CATEGORY +"/"+ English.plural(BitstreamRest.NAME) + "/" + source.getID() + "/content");
				result.getFiles().add(b);
			}
		}
		return result; 
	}


}
