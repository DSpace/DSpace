/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.discovery.IndexableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Collection in the DSpace API data model and
 * the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class CollectionConverter
    extends DSpaceObjectConverter<org.dspace.content.Collection, org.dspace.app.rest.model.CollectionRest>
    implements IndexableObjectConverter<Collection, CollectionRest> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CollectionConverter.class);

    @Autowired
    private ConverterService converter;

    @Override
    public CollectionRest convert(org.dspace.content.Collection obj, Projection projection) {
        CollectionRest col = super.convert(obj, projection);
        Bitstream logo = obj.getLogo();
        if (logo != null) {
            col.setLogo(converter.toRest(logo, projection));
        }
        return col;
    }

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
