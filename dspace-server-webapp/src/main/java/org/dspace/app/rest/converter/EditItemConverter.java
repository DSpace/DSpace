/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.EditItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.content.edit.EditItem;
import org.dspace.discovery.IndexableObject;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class EditItemConverter
    extends AInprogressItemConverter<EditItem, EditItemRest> {

    public EditItemConverter() throws SubmissionConfigReaderException {
        super();
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.IndexableObjectConverter#supportsModel(org.dspace.discovery.IndexableObject)
     */
    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof EditItem;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#
     * convert(java.lang.Object, org.dspace.app.rest.projection.Projection)
     */
    @Override
    public EditItemRest convert(EditItem modelObject, Projection projection) {
        EditItemRest eitem = new EditItemRest();
        eitem.setProjection(projection);
        fillFromModel(modelObject, eitem, projection);
        return eitem;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#getModelClass()
     */
    @Override
    public Class<EditItem> getModelClass() {
        return EditItem.class;
    }
}
