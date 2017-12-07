/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * Submission "replace" common PATCH operation.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public abstract class MetadataValueReplacePatchOperation<DSO extends DSpaceObject> extends ReplacePatchOperation<MetadataValueRest> {

	@Override
	protected Class<MetadataValueRest[]> getArrayClassForEvaluation() {
		return MetadataValueRest[].class;
	}

	@Override
	protected Class<MetadataValueRest> getClassForEvaluation() {
		return MetadataValueRest.class;
	}

	protected void replaceValue(Context context, DSO source, String target, List<MetadataValue> list, MetadataValueRest object, int index)
			throws SQLException {
		String[] metadata = Utils.tokenize(target);
		List<MetadataValue> values = new ArrayList<MetadataValue>();
		int idx = 0;
		for (MetadataValue ll : list) {
			
			if(idx==index) {
				values.add(ll);
			}
			idx++;
		}
		
		getDSpaceObjectService().removeMetadataValues(context, source, values);
		
		getDSpaceObjectService().addAndShiftRightMetadata(context, source, metadata[0], metadata[1], metadata[2],
				object.getLanguage(), object.getValue(), object.getAuthority(), object.getConfidence(), index);
	}
	
	protected abstract DSpaceObjectService<DSO> getDSpaceObjectService();
}
