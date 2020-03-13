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

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
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
public class VersionEPersonLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(VersionEPersonLinkRepository.class);

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizeService authorizeService;


    /**
     * This method will return the EPersonRest object from the Version retrieved by the given versionId
     * @param request           The current request
     * @param versionId         The id for the Version from which we'll retrieve the EPerson
     * @param optionalPageable  Pageable if present
     * @param projection        Current Projection
     * @return                  The EPerson that is attached to the Version object that is associated with the given
     *                          versionId Integer
     * @throws SQLException     If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    public EPersonRest getEPersonForVersion(@Nullable HttpServletRequest request,
                                            Integer versionId,
                                            @Nullable Pageable optionalPageable,
                                            Projection projection) throws SQLException, AuthorizeException {

        Context context = obtainContext();
        Version version = versioningService.getVersion(context, versionId);
        if (version == null) {
            throw new ResourceNotFoundException("The Version with id: " + versionId + " couldn't be found");
        }
        EPerson ePerson = version.getEPerson();
        if (!authorizeService.isAdmin(context) &&
            !configurationService.getBooleanProperty("versioning.item.history.include.submitter")) {
            throw new ResourceNotFoundException("The EPerson for this Version couldn't be displayed");
        }
        if (ePerson == null) {
            throw new ResourceNotFoundException("The EPerson for version with id: " + versionId + " couldn't be found");
        }
        return converter.toRest(ePerson, projection);
    }

    public boolean isEmbeddableRelation(Object data, String name) {
        return false;
    }

    @Override
    public boolean isLinkableRelation(Object data, String name) {
        return isAdmin() || DSpaceServicesFactory.getInstance().getConfigurationService()
                                                 .getBooleanProperty("versioning.item.history.include.submitter");
    }

    private boolean isAdmin() {
        try {
            return AuthorizeServiceFactory.getInstance().getAuthorizeService().isAdmin(
                ContextUtil.obtainContext(
                    DSpaceServicesFactory.getInstance().getRequestService().getCurrentRequest()
                                         .getHttpServletRequest()));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
