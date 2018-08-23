/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.content.DSpaceObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to an unknown DSpaceObject in the DSpace API data model and the
 * REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class GenericDSpaceObjectConverter
        extends DSpaceObjectConverter<org.dspace.content.DSpaceObject, org.dspace.app.rest.model.DSpaceObjectRest> {

    @Autowired
    private List<DSpaceObjectConverter> converters;

    private static final Logger log = Logger.getLogger(GenericDSpaceObjectConverter.class);

    /**
     * Convert a DSpaceObject in its REST representation using a suitable converter
     */
    @Override
    public DSpaceObjectRest fromModel(org.dspace.content.DSpaceObject dspaceObject) {
        for (DSpaceObjectConverter converter : converters) {
            if (converter.supportsModel(dspaceObject)) {
                return converter.fromModel(dspaceObject);
            }
        }
        return null;
    }

    @Override
    public org.dspace.content.DSpaceObject toModel(DSpaceObjectRest obj) {
        return null;
    }

    @Override
    protected DSpaceObjectRest newInstance() {
        return null;
    }

    @Override
    protected Class<DSpaceObject> getModelClass() {
        return DSpaceObject.class;
    }

}
