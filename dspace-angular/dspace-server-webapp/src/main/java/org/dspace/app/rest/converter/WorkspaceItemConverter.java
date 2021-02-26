/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.content.WorkspaceItem;
import org.dspace.discovery.IndexableObject;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the WorkspaceItem in the DSpace API data model
 * and the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class WorkspaceItemConverter
    extends AInprogressItemConverter<WorkspaceItem, WorkspaceItemRest> {

    public WorkspaceItemConverter() throws SubmissionConfigReaderException {
        super();
    }

    @Override
    public WorkspaceItemRest convert(org.dspace.content.WorkspaceItem obj, Projection projection) {
        WorkspaceItemRest witem = new WorkspaceItemRest();
        witem.setProjection(projection);
        fillFromModel(obj, witem, projection);
        return witem;
    }

    public org.dspace.content.WorkspaceItem toModel(WorkspaceItemRest obj) {
        return null;
    }

    @Override
    public Class<WorkspaceItem> getModelClass() {
        return WorkspaceItem.class;
    }

    @Override
    public boolean supportsModel(IndexableObject object) {
        return object.getIndexedObject() instanceof WorkspaceItem;
    }
}
