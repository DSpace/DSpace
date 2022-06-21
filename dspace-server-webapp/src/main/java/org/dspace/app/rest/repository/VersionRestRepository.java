/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.handler.service.UriListHandlerService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the Repository that takes care of the operations on the {@link VersionRest} objects
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(VersionRest.CATEGORY + "." + VersionRest.NAME)
public class VersionRestRepository extends DSpaceRestRepository<VersionRest, Integer>
                                    implements ReloadableEntityObjectRepository<Version, Integer> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(VersionRestRepository.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private VersionHistoryService versionHistoryService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private UriListHandlerService uriListHandlerService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @SuppressWarnings("rawtypes")
    @Autowired(required = true)
    protected WorkflowItemService workflowItemService;

    @Override
    @PreAuthorize("@versioningSecurity.isEnableVersioning() && hasPermission(#id, 'VERSION', 'READ')")
    public VersionRest findOne(Context context, Integer id) {
        try {
            Version version = versioningService.getVersion(context, id);
            if (Objects.isNull(version) || Objects.isNull(version.getItem())) {
                throw new ResourceNotFoundException("Couldn't find version for id: " + id);
            }
            return converterService.toRest(version, utils.obtainProjection());
        } catch (SQLException e) {
            log.error("Something with wrong getting version with id:" + id, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("@versioningSecurity.isEnableVersioning() && hasAuthority('AUTHENTICATED')")
    protected VersionRest createAndReturn(Context context, List<String> stringList)
        throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        String summary = req.getParameter("summary");

        Item item = uriListHandlerService.handle(context, req, stringList, Item.class);
        if (Objects.isNull(item)) {
            throw new UnprocessableEntityException("The given URI list could not be properly parsed to one result");
        }

        EPerson submitter = item.getSubmitter();
        boolean isAdmin = authorizeService.isAdmin(context);
        boolean canCreateVersion = configurationService.getBooleanProperty("versioning.submitterCanCreateNewVersion");

        if (!isAdmin && !(canCreateVersion && Objects.equals(submitter, context.getCurrentUser()))) {
            throw new AuthorizeException("The logged user doesn't have the rights to create a new version.");
        }

        WorkflowItem workflowItem = null;
        WorkspaceItem workspaceItem = null;
        VersionHistory versionHistory = versionHistoryService.findByItem(context, item);
        if (Objects.nonNull(versionHistory)) {
            Version lastVersion = versionHistoryService.getLatestVersion(context, versionHistory);
            if (Objects.nonNull(lastVersion)) {
                workflowItem = workflowItemService.findByItem(context, lastVersion.getItem());
                workspaceItem = workspaceItemService.findByItem(context, lastVersion.getItem());
            }
        } else {
            workflowItem = workflowItemService.findByItem(context, item);
            workspaceItem = workspaceItemService.findByItem(context, item);
        }

        if (Objects.nonNull(workflowItem) || Objects.nonNull(workspaceItem)) {
            throw new UnprocessableEntityException("It is not possible to create a new version"
                                                         + " if the latest one in submisssion!");
        }

        Version version = StringUtils.isNotBlank(summary) ?
                          versioningService.createNewVersion(context, item, summary) :
                          versioningService.createNewVersion(context, item);
        return converter.toRest(version, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("@versioningSecurity.isEnableVersioning() && hasPermission(#versionId, 'version', 'ADMIN')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model,
                         Integer versionId, Patch patch) throws AuthorizeException, SQLException {
        for (Operation operation : patch.getOperations()) {
            if (!operation.getPath().equals("/summary")) {
                throw new UnprocessableEntityException("The provided property:"
                          + operation.getPath() + " is not supported!");
            }
            Version version = versioningService.getVersion(context, versionId);
            if (Objects.isNull(version)) {
                throw new ResourceNotFoundException("This given id:" + versionId + " does not resolve to a Version");
            }
            switch (operation.getOp()) {
                case "remove":
                    version.setSummary(StringUtils.EMPTY);
                    break;
                case "add":
                    if (StringUtils.isNotBlank(version.getSummary())) {
                        throw new DSpaceBadRequestException("The 'summary' already contains the value: "
                                     + version.getSummary() + ", it is not possible to add a new value.");
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

    @Override
    public Version findDomainObjectByPk(Context context, Integer id) throws SQLException {
        return versioningService.getVersion(context, id);
    }

    @Override
    public Class<Integer> getPKClass() {
        return Integer.class;
    }

}
