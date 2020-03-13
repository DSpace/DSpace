/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.versioning.Version;
import org.springframework.stereotype.Component;

/**
 * This is the converter that takes care of the conversion between {@link Version} and {@link VersionRest}
 */
@Component
public class VersionConverter implements DSpaceConverter<Version, VersionRest> {

    @Override
    public VersionRest convert(Version modelObject, Projection projection) {
        VersionRest versionRest = new VersionRest();
        versionRest.setCreated(modelObject.getVersionDate());
        versionRest.setId(modelObject.getID());
        versionRest.setSummary(modelObject.getSummary());
        versionRest.setVersion(modelObject.getVersionNumber());
        versionRest.setProjection(projection);
        return versionRest;
    }

    @Override
    public Class<Version> getModelClass() {
        return Version.class;
    }
}
