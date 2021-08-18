/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.handler.service.UriListHandlerService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the Repository that takes care of the operations on the {@link VersionRest} objects
 */
@Component(VersionRest.CATEGORY + "." + VersionRest.NAME)
public class VersionRestRepository extends DSpaceRestRepository<VersionRest, Integer> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(VersionRestRepository.class);

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private UriListHandlerService uriListHandlerService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private VersionHistoryService versionHistoryService;

    @Override
    @PreAuthorize("hasPermission(#id, 'VERSION', 'READ')")
    public VersionRest findOne(Context context, Integer id) {
        try {
            Version version = versioningService.getVersion(context, id);
            if (version == null) {
                throw new ResourceNotFoundException("Couldn't find version for id: " + id);
            }
            return converterService.toRest(version, utils.obtainProjection());
        } catch (SQLException e) {
            log.error("Something with wrong getting version with id:" + id, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, Integer id) throws AuthorizeException {
        Version version = null;
        try {
            version = versioningService.getVersion(context, id);
            if (Objects.isNull(version)) {
                throw new ResourceNotFoundException(
                          VersionRest.CATEGORY + "." + VersionRest.NAME + " with id: " + id + " not found");
            }
            versioningService.delete(context, version);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete Version with id = " + id, e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected VersionRest createAndReturn(Context context, List<String> stringList)
        throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        String summary = req.getParameter("summary");
        Item item = uriListHandlerService.handle(context, req, stringList, Item.class);
        if (Objects.isNull(item)) {
            throw new UnprocessableEntityException("The given URI list could not be properly parsed to one result");
        }
        Version version = null;
        if (StringUtils.isNotBlank(summary)) {
            version = versioningService.createNewVersion(context, item, summary);
        } else {
            version = versioningService.createNewVersion(context, item);
        }
        return converter.toRest(version, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "findByItem")
    public VersionRest findByItem(@Parameter(value = "itemUuid", required = true) UUID itemUuid) {
        Context context = obtainContext();
        Version version = null;
        try {
            Item item = itemService.find(context, itemUuid);
            if (Objects.nonNull(item)) {
                version = versioningService.getVersion(context, item);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return Objects.nonNull(version) ? converter.toRest(version, utils.obtainProjection()) : null;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "findByHistory")
    public Page<VersionRest> findByHistory(@Parameter(value = "historyId", required = true) Integer historyId,
                                           Pageable pageable) {
        Context context = obtainContext();
        List<Version> versions = new LinkedList<Version>();;
        int total = 0;
        try {
            VersionHistory versionHistory = versionHistoryService.find(context, historyId);
            if (Objects.isNull(versionHistory)) {
                throw new DSpaceBadRequestException(
                      "This given id:" + historyId + " does not resolve to a VersionHistory");
            }
            versions = versioningService.getVersionsByHistory(context, versionHistory,
                                         Math.toIntExact(pageable.getOffset()),
                                         Math.toIntExact(pageable.getPageSize()));
            total = versioningService.countVersionsByHistory(context, versionHistory);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(versions, pageable, total, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model,
                         Integer versionId, Patch patch) throws AuthorizeException, SQLException {
        for (Operation operation : patch.getOperations()) {
            if (!operation.getPath().equals("/summary")) {
                throw new UnprocessableEntityException("The provided property:"
                          + operation.getPath() + " is not supported!");
            }
            Version version = versioningService.getVersion(context, versionId);
            if (Objects.isNull(version)) {
                throw new NotFoundException("This given id:" + versionId + " does not resolve to a Version");
            }
            switch (operation.getOp()) {
                case "remove":
                    version.setSummary(StringUtils.EMPTY);
                    break;
                case "add":
                    if (StringUtils.isNotBlank(version.getSummary())) {
                        throw new DSpaceBadRequestException("The 'summary' property is setted with value:"
                                          + version.getSummary() + ", it is not possible to add new value");
                    }
                    version.setSummary(operation.getValue().toString());
                    break;
                case "replace":
                    version.setSummary(operation.getValue().toString());
                    break;
                default: throw new UnprocessableEntityException("Provided operation:"
                                   + operation.getOp() + " is not supported");
            }
        }
    }

    @Override
    public Page<VersionRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    public Class<VersionRest> getDomainClass() {
        return VersionRest.class;
    }
}
