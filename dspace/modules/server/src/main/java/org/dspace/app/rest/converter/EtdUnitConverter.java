package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.EtdUnitRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.EtdUnit;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the EtdUnit in the DSpace API data model
 * and the REST data model
 */
@Component
public class EtdUnitConverter extends DSpaceObjectConverter<EtdUnit, EtdUnitRest> {
    @Override
    public EtdUnitRest convert(EtdUnit obj, Projection projection) {
        EtdUnitRest etdunitRest = super.convert(obj, projection);
        return etdunitRest;
    }

    @Override
    protected EtdUnitRest newInstance() {
        return new EtdUnitRest();
    }

    @Override
    public Class<EtdUnit> getModelClass() {
        return EtdUnit.class;
    }
}
