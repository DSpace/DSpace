package org.dspace.app.rest.submit;

import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.step.SectionData;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.WorkspaceItem;

public interface AbstractRestProcessingStep {

	public SectionData getData(WorkspaceItem obj, SubmissionStepConfig config);


}
