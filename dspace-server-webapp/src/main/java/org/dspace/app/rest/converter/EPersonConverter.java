/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.eperson.EPerson;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the EPerson in the DSpace API data model and the
 * REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class EPersonConverter extends DSpaceObjectConverter<EPerson, org.dspace.app.rest.model.EPersonRest> {

    @Override
    public EPersonRest convert(EPerson obj, Projection projection) {
        EPersonRest eperson = super.convert(obj, projection);
        eperson.setLastActive(obj.getLastActive());
        eperson.setNetid(obj.getNetid());
        eperson.setCanLogIn(obj.canLogIn());
        eperson.setRequireCertificate(obj.getRequireCertificate());
        eperson.setSelfRegistered(obj.getSelfRegistered());
        eperson.setEmail(obj.getEmail());

        return eperson;
    }

    @Override
    protected EPersonRest newInstance() {
        return new EPersonRest();
    }

    @Override
    public Class<EPerson> getModelClass() {
        return EPerson.class;
    }

}
