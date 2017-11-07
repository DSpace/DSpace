/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.step.SectionData;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.WorkspaceItem;

public interface AbstractRestProcessingStep {

	public SectionData getData(WorkspaceItem obj, SubmissionStepConfig config);


}
