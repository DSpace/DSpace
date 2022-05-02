/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.PoolTaskRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.discovery.IndexableObject;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    @Override
    public PoolTaskRest convert(PoolTask obj, Projection projection) {
        PoolTaskRest taskRest = new PoolTaskRest();
        taskRest.setProjection(projection);

        XmlWorkflowItem witem = obj.getWorkflowItem();
        taskRest.setId(obj.getID());
        taskRest.setWorkflowitem(converter.toRest(witem, projection));
        if (obj.getEperson() != null) {
            taskRest.setEperson(converter.toRest(obj.getEperson(), projection));
        }
        if (obj.getGroup() != null) {
            taskRest.setGroup(converter.toRest(obj.getGroup(), projection));
        }
        taskRest.setAction(obj.getActionID());
        return taskRest;
    }

    @Override
    public Class<PoolTask> getModelClass() {
        return PoolTask.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof PoolTask;
    }
}
