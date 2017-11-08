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

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.step.DataDescribe;
import org.dspace.app.rest.model.step.SectionData;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Utils;
import org.springframework.beans.factory.annotation.Autowired;

public class DescribeStep extends org.dspace.submit.step.DescribeStep implements AbstractRestProcessingStep {

	private static final Logger log = Logger.getLogger(DescribeStep.class);

	private DCInputsReader inputReader;
	
	public DescribeStep() throws DCInputsReaderException {
		inputReader = new DCInputsReader();
	}
	
	@Override
	public DataDescribe getData(WorkspaceItem obj, SubmissionStepConfig config) {		
		DataDescribe data = new DataDescribe();
		try {
			DCInputSet inputConfig = inputReader.getInputsByFormName(config.getId());
			for(DCInput input : inputConfig.getFields()) {
				List<MetadataValue> mdv = itemService.getMetadataByMetadataString(obj.getItem(), input.getFieldName());
				for(MetadataValue md : mdv) {
					MetadataValueRest dto = new MetadataValueRest();
					dto.setAuthority(md.getAuthority());
					dto.setConfidence(md.getConfidence());
					dto.setLanguage(md.getLanguage());
					dto.setPlace(md.getPlace());
					dto.setValue(md.getValue());
					
					if(data.getMetadata().containsKey(md.getMetadataField().toString())) {
						data.getMetadata().get(Utils.standardize(md.getMetadataField().getMetadataSchema().getName(), md.getMetadataField().getElement(), md.getMetadataField().getQualifier(), ".")).add(dto);
					}
					else {
						List<MetadataValueRest> listDto = new ArrayList<>();
						listDto.add(dto);
						data.getMetadata().put(Utils.standardize(md.getMetadataField().getMetadataSchema().getName(), md.getMetadataField().getElement(), md.getMetadataField().getQualifier(), "."), listDto);
					}
				}
			}
		} catch (DCInputsReaderException e) {
			log.error(e.getMessage(), e);
		}
		return data;
	}


}
