/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.step.DataDescribe;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.model.Request;
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;

/**
 * Describe step for DSpace Spring Rest. Handle the exposition of metadata own by the in progress submission.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
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
					
					String[] metadataToCheck = Utils.tokenize(md.getMetadataField().toString());					
					if(data.getMetadata().containsKey(Utils.standardize(metadataToCheck[0], metadataToCheck[1], metadataToCheck[2], "."))) {
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

	@Override
	public void doPatchProcessing(Context context, Request currentRequest, WorkspaceItem source, String operation,
			String target, String index, Object value) throws Exception {
		MetadataValueRest[] list = null;
		if(value!=null) {
			LateObjectEvaluator object = (LateObjectEvaluator)value;
			list = (MetadataValueRest[])object.evaluate(MetadataValueRest[].class);
		}
		switch (operation) {
		case "add":
			addValue(context, source, target, list);
			break;
		case "replace":
			replaceValue(context, source, target, list);
			break;
		case "remove":
			deleteValue(context, source, target, index);
			break;
		default:
			throw new RuntimeException("Operation "+operation+" not yet implemented!");
		}
	}

	private void deleteValue(Context context, WorkspaceItem source, String target, String index) throws SQLException {
		String[] metadata = Utils.tokenize(target);
		List<MetadataValue> mm = itemService.getMetadata(source.getItem(),  metadata[0], metadata[1], metadata[2], Item.ANY);
		itemService.clearMetadata(context, source.getItem(), metadata[0], metadata[1], metadata[2], Item.ANY);
		int idx = 0;
		for(MetadataValue m : mm) {
			Integer toDelete = Integer.parseInt(index);
			if(idx != toDelete) {
				itemService.addMetadata(context, source.getItem(), metadata[0], metadata[1], metadata[2], m.getLanguage(), m.getValue(), m.getAuthority(), m.getConfidence());	
			}
			idx++;
		}
	}

	private void replaceValue(Context context, WorkspaceItem source, String target, MetadataValueRest[] list) throws SQLException {		
		String[] metadata = Utils.tokenize(target);
		itemService.clearMetadata(context, source.getItem(), metadata[0], metadata[1], metadata[2], Item.ANY);
		for(MetadataValueRest ll : list) {
			itemService.addMetadata(context, source.getItem(), metadata[0], metadata[1], metadata[2], ll.getLanguage(), ll.getValue(), ll.getAuthority(), ll.getConfidence());
		}		
	}

	private void addValue(Context context, WorkspaceItem source, String target, MetadataValueRest[] list) throws SQLException {
		String[] metadata = Utils.tokenize(target);
		for(MetadataValueRest ll : list) {
			itemService.addMetadata(context, source.getItem(), metadata[0], metadata[1], metadata[2], ll.getLanguage(), ll.getValue(), ll.getAuthority(), ll.getConfidence());
		}
	}

}
