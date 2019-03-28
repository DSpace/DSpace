/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.WorkflowItemRest;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.discovery.IndexableObject;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the WorkflowItem in the DSpace API data model
 * and the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class WorkflowItemConverter
    extends AInprogressItemConverter<XmlWorkflowItem, org.dspace.app.rest.model.WorkflowItemRest, Integer> {

    public WorkflowItemConverter() throws SubmissionConfigReaderException {
        super();
    }

    @Override
    public WorkflowItemRest fromModel(XmlWorkflowItem obj) {
        WorkflowItemRest witem = new WorkflowItemRest();
        fillFromModel(obj, witem);
        return witem;
    }

    @Override
    public XmlWorkflowItem toModel(WorkflowItemRest obj) {
        return null;
    }

    @Override
    public boolean supportsModel(IndexableObject object) {
        return object instanceof XmlWorkflowItem;
    }
}
