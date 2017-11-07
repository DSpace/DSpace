/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import org.dspace.app.rest.model.step.DataCollection;
import org.dspace.app.rest.model.step.SectionData;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.WorkspaceItem;

public class CollectionStep extends org.dspace.submit.step.SelectCollectionStep implements AbstractRestProcessingStep {

	@Override
	public SectionData getData(WorkspaceItem obj, SubmissionStepConfig config) {
		DataCollection collection = new DataCollection();
		collection.setCollection(obj.getCollection());
		return collection;
	}


}
