/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.PoolTaskRest;
import org.dspace.discovery.IndexableObject;
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
    implements IndexableObjectConverter<PoolTask, org.dspace.app.rest.model.PoolTaskRest> {

    @Autowired
    private WorkflowItemConverter workflowItemConverter;

    @Autowired
    private EPersonConverter epersonConverter;

    @Autowired
    private GroupConverter groupConverter;

    @Override
    public PoolTaskRest fromModel(PoolTask obj) {
        PoolTaskRest taskRest = new PoolTaskRest();

        XmlWorkflowItem witem = obj.getWorkflowItem();
        taskRest.setId(obj.getID());
        taskRest.setWorkflowitem(workflowItemConverter.convert(witem));
        if (obj.getEperson() != null) {
            taskRest.setEperson(epersonConverter.convert(obj.getEperson()));
        }
        if (obj.getGroup() != null) {
            taskRest.setGroup(groupConverter.convert(obj.getGroup()));
        }
        taskRest.setAction(obj.getActionID());
        taskRest.setStep(obj.getStepID());
        return taskRest;
    }

    @Override
    public PoolTask toModel(PoolTaskRest obj) {
        return null;
    }

    @Override
    public boolean supportsModel(IndexableObject object) {
        return object instanceof PoolTask;
    }

}