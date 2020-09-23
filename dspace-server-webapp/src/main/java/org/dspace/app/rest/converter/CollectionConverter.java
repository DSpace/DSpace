/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CollectionRest;
import org.dspace.content.Collection;
import org.dspace.discovery.IndexableObject;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Collection in the DSpace API data model and
 * the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class CollectionConverter extends DSpaceObjectConverter<Collection, CollectionRest>
        implements IndexableObjectConverter<Collection, CollectionRest> {

    @Override
    protected CollectionRest newInstance() {
        return new CollectionRest();
    }

    @Override
    public Class<org.dspace.content.Collection> getModelClass() {
        return org.dspace.content.Collection.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof Collection;
    }
}
