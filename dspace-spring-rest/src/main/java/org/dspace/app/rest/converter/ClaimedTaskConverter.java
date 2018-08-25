/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.ClaimedTaskRest;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the laimTask in the DSpace API data model
 * and the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ClaimedTaskConverter
    extends BrowsableDSpaceObjectConverter<ClaimedTask, org.dspace.app.rest.model.ClaimedTaskRest> {

    private static final Logger log = Logger.getLogger(ClaimedTaskConverter.class);

    @Autowired
    private WorkflowItemConverter workflowItemConverter;

    @Override
    public ClaimedTaskRest fromModel(ClaimedTask obj) {
        ClaimedTaskRest taskRest = new ClaimedTaskRest();

        XmlWorkflowItem witem = obj.getWorkflowItem();
        taskRest.setId(obj.getID());
        taskRest.setWorkflowitem(workflowItemConverter.convert(witem));
        taskRest.setAction(obj.getActionID());
        taskRest.setStep(obj.getStepID());
        return taskRest;
    }

    @Override
    public ClaimedTask toModel(ClaimedTaskRest obj) {
        return null;
    }

    @Override
    public boolean supportsModel(BrowsableDSpaceObject object) {
        return object instanceof ClaimedTask;
    }

}