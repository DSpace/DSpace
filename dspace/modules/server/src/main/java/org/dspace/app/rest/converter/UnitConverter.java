package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.UnitRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.eperson.Unit;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Unit in the DSpace API data model
 * and the REST data model
 */
@Component
public class UnitConverter extends DSpaceObjectConverter<Unit, UnitRest> {

    @Override
    public UnitRest convert(Unit obj, Projection projection) {
        UnitRest unitRest = super.convert(obj, projection);
        unitRest.setFacultyOnly(obj.getFacultyOnly());
        return unitRest;
    }

    @Override
    protected UnitRest newInstance() {
        return new UnitRest();
    }

    @Override
    public Class<Unit> getModelClass() {
        return Unit.class;
    }

}
