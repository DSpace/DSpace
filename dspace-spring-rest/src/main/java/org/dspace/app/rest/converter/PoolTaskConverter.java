/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.PoolTaskRest;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the PoolTask in the DSpace API data model
 * and the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class PoolTaskConverter
    extends BrowsableDSpaceObjectConverter<PoolTask, org.dspace.app.rest.model.PoolTaskRest> {

    private static final Logger log = Logger.getLogger(PoolTaskConverter.class);

    @Autowired
    private WorkflowItemConverter workflowItemConverter;

    @Override
    public PoolTaskRest fromModel(PoolTask obj) {
        PoolTaskRest taskRest = new PoolTaskRest();

        XmlWorkflowItem witem = obj.getWorkflowItem();
        taskRest.setId(obj.getID());
        taskRest.setWorkflowitem(workflowItemConverter.convert(witem));
        taskRest.setAction(obj.getActionID());
        taskRest.setStep(obj.getStepID());
        return taskRest;
    }

    @Override
    public PoolTask toModel(PoolTaskRest obj) {
        return null;
    }

    @Override
    public boolean supportsModel(BrowsableDSpaceObject object) {
        return object instanceof PoolTask;
    }

}