/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CheckSumRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.model.step.UploadBitstreamRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.WorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.init.UncategorizedScriptException;
import org.springframework.stereotype.Component;

/**
 * Service to manipulate in-progress submissions.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class SubmissionService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SubmissionService.class);

    @Autowired
    protected ConfigurationService configurationService;
    @Autowired
    protected CollectionService collectionService;
    @Autowired
    protected ItemService itemService;
    @Autowired
    protected WorkspaceItemService workspaceItemService;
    @Autowired
    protected WorkflowItemService<XmlWorkflowItem> workflowItemService;
    @Autowired
    protected WorkflowService<XmlWorkflowItem> workflowService;
    @Autowired
    private RequestService requestService;
    @Autowired
    private ConverterService converter;

    /**
     * Create a workspaceitem using the information in the reqest
     *
     * @param context
     *            the dspace context
     * @param request
     *            the request containing the details about the workspace to create
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     */
    public WorkspaceItem createWorkspaceItem(Context context, Request request) throws SQLException, AuthorizeException {
        WorkspaceItem wsi = null;
        Collection collection = null;
        String collectionUUID = request.getHttpServletRequest().getParameter("owningCollection");

        if (StringUtils.isBlank(collectionUUID)) {
            collectionUUID = configurationService.getProperty("submission.default.collection");
        }

        try {
            if (StringUtils.isNotBlank(collectionUUID)) {
                collection = collectionService.find(context, UUID.fromString(collectionUUID));
            } else {
                final List<Collection> findAuthorizedOptimized = collectionService.findAuthorizedOptimized(context,
                        Constants.ADD);
                if (findAuthorizedOptimized != null && findAuthorizedOptimized.size() > 0) {
                    collection = findAuthorizedOptimized.get(0);
                } else {
                    throw new RESTAuthorizationException("No collection suitable for submission for the current user");
                }
            }

            if (collection == null) {
                throw new RESTAuthorizationException("collectionUUID=" + collectionUUID + " not found");
            }
            wsi = workspaceItemService.create(context, collection, true);
        } catch (SQLException e) {
            // wrap in a runtime exception as we cannot change the method signature
            throw new UncategorizedScriptException(e.getMessage(), e);
        } catch (AuthorizeException ae) {
            throw new RESTAuthorizationException(ae);
        }

        return wsi;
    }

    public void saveWorkspaceItem(Context context, WorkspaceItem wsi) {
        try {
            workspaceItemService.update(context, wsi);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    public UploadBitstreamRest buildUploadBitstream(ConfigurationService configurationService, Bitstream source)
        throws SQLException {
        UploadBitstreamRest data = new UploadBitstreamRest();

        for (MetadataValue md : source.getMetadata()) {

            MetadataValueRest dto = new MetadataValueRest();
            dto.setAuthority(md.getAuthority());
            dto.setConfidence(md.getConfidence());
            dto.setLanguage(md.getLanguage());
            dto.setPlace(md.getPlace());
            dto.setValue(md.getValue());

            String[] metadataToCheck = Utils.tokenize(md.getMetadataField().toString());
            if (data.getMetadata()
                    .containsKey(Utils.standardize(metadataToCheck[0], metadataToCheck[1], metadataToCheck[2], "."))) {
                data.getMetadata().get(Utils.standardize(md.getMetadataField().getMetadataSchema().getName(),
                                                         md.getMetadataField().getElement(),
                                                         md.getMetadataField().getQualifier(), ".")).add(dto);
            } else {
                List<MetadataValueRest> listDto = new ArrayList<>();
                listDto.add(dto);
                data.getMetadata().put(Utils.standardize(md.getMetadataField().getMetadataSchema().getName(),
                                                         md.getMetadataField().getElement(),
                                                         md.getMetadataField().getQualifier(), "."), listDto);
            }

        }

        HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
        data.setFormat(converter.toRest(source.getFormat(ContextUtil.obtainContext(request)), Projection.DEFAULT));

        for (ResourcePolicy rp : source.getResourcePolicies()) {
            if (ResourcePolicy.TYPE_CUSTOM.equals(rp.getRpType())) {
                ResourcePolicyRest resourcePolicyRest = converter.toRest(rp, Projection.DEFAULT);
                data.getAccessConditions().add(resourcePolicyRest);
            }
        }

        data.setUuid(source.getID());
        CheckSumRest checksum = new CheckSumRest();
        checksum.setCheckSumAlgorithm(source.getChecksumAlgorithm());
        checksum.setValue(source.getChecksum());
        data.setCheckSum(checksum);
        data.setSizeBytes(source.getSizeBytes());
        data.setUrl(configurationService.getProperty("dspace.server.url") + "/api/" + BitstreamRest.CATEGORY + "/" +
                        English.plural(BitstreamRest.NAME) + "/" + source.getID() + "/content");
        return data;
    }

    /**
     * Create a workflowitem using the information in the reqest
     *
     * @param context
     *            the dspace context
     * @param requestUriListString
     *            the id of the workspaceItem
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     * @throws WorkflowException
     */
    public XmlWorkflowItem createWorkflowItem(Context context, String requestUriListString)
            throws SQLException, AuthorizeException, WorkflowException {
        XmlWorkflowItem wi = null;
        if (StringUtils.isBlank(requestUriListString)) {
            throw new UnprocessableEntityException("Malformed body..." + requestUriListString);
        }
        String regex = "\\/api\\/" + WorkspaceItemRest.CATEGORY + "\\/" + English.plural(WorkspaceItemRest.NAME)
                + "\\/";
        String[] split = requestUriListString.split(regex, 2);
        if (split.length != 2) {
            throw new UnprocessableEntityException("Malformed body..." + requestUriListString);
        }
        WorkspaceItem wsi = null;
        int id = 0;
        try {
            id = Integer.parseInt(split[1]);
            wsi = workspaceItemService.find(context, id);
        } catch (NumberFormatException e) {
            throw new UnprocessableEntityException("The provided workspaceitem URI is not valid");
        }
        if (wsi == null) {
            throw new UnprocessableEntityException("Workspace item is not found");
        }
        WorkspaceItemRest wsiRest = converter.toRest(wsi, Projection.DEFAULT);
        if (!wsiRest.getErrors().isEmpty()) {
            throw new UnprocessableEntityException(
                    "Start workflow failed due to validation error on workspaceitem");
        }

        try {
            wi = workflowService.start(context, wsi);
        } catch (IOException e) {
            throw new RuntimeException("The workflow could not be started for workspaceItem with" +
                                           "id:  " + id);
        }

        return wi;
    }

    public void saveWorkflowItem(Context context, XmlWorkflowItem source) throws SQLException, AuthorizeException {
        workflowItemService.update(context, source);
    }
}
