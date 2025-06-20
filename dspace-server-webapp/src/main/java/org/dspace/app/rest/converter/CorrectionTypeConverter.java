/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CorrectionTypeRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.correctiontype.CorrectionType;
import org.springframework.stereotype.Component;

/**
 * This class provides the method to convert a CorrectionType to its REST representation, the
 * CorrectionTypeRest
 * 
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Component
public class CorrectionTypeConverter implements DSpaceConverter<CorrectionType, CorrectionTypeRest> {

    @Override
    public CorrectionTypeRest convert(CorrectionType target, Projection projection) {
        CorrectionTypeRest targetRest = new CorrectionTypeRest();
        targetRest.setProjection(projection);
        targetRest.setId(target.getId());
        targetRest.setTopic(target.getTopic());
        return targetRest;
    }

    @Override
    public Class<CorrectionType> getModelClass() {
        return CorrectionType.class;
    }

}
