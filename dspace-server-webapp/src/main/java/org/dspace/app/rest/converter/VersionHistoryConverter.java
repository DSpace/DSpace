/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.VersionHistoryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.versioning.VersionHistory;
import org.springframework.stereotype.Component;

/**
 * This is the Converter that takes care of the conversion between {@link VersionHistory} and {@link VersionHistoryRest}
 */
@Component
public class VersionHistoryConverter implements DSpaceConverter<VersionHistory, VersionHistoryRest> {

    @Override
    public VersionHistoryRest convert(VersionHistory modelObject, Projection projection) {
        VersionHistoryRest versionHistoryRest = new VersionHistoryRest();
        versionHistoryRest.setId(modelObject.getID());
        return versionHistoryRest;
    }

    @Override
    public Class<VersionHistory> getModelClass() {
        return VersionHistory.class;
    }
}
