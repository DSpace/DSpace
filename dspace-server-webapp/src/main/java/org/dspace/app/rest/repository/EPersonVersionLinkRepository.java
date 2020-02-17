/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is the Repository that will take care of retrieving the EPerson for a given Version
 */
@Component(VersionRest.CATEGORY + "." + VersionRest.NAME + "." + VersionRest.EPERSON)
public class EPersonVersionLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private ConfigurationService configurationService;


    public EPersonRest getEPersonForVersion(@Nullable HttpServletRequest request,
                                            Integer versionId,
                                            @Nullable Pageable optionalPageable,
                                            Projection projection) throws SQLException, AuthorizeException {

        Context context = obtainContext();
        Version version = versioningService.getVersion(context, versionId);
        EPerson ePerson = version.getEPerson();
        if (!configurationService.getBooleanProperty("versioning.item.history.include.submitter")) {
            throw new ResourceNotFoundException("The EPerson for this Version couldn't be displayed");
        }
        if (ePerson == null) {
            throw new ResourceNotFoundException("The EPerson for version with id: " + versionId + " couldn't be found");
        }
        return converter.toRest(ePerson, projection);
    }

    @Override
    public boolean isEmbeddableRelation(Object data, String name) {
        return false;
    }


}
