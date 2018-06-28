/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.converter.BitstreamFormatConverter;
import org.dspace.app.rest.converter.ResourcePolicyConverter;
import org.dspace.app.rest.converter.WorkspaceItemConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CheckSumRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.model.step.UploadBitstreamRest;
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
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.WorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service to manipulate in-progress submissions.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class SubmissionService {

    private static final Logger log = Logger.getLogger(SubmissionService.class);

    @Autowired
    protected ConfigurationService configurationService;
    @Autowired
    protected CollectionService collectionService;
    @Autowired
    protected ItemService itemService;
    @Autowired
    protected WorkspaceItemService workspaceItemService;
    @Autowired
    protected WorkflowItemService workflowItemService;
    @Autowired
    protected WorkflowService<XmlWorkflowItem> workflowService;
    @Autowired
    private RequestService requestService;
    @Autowired(required = true)
    BitstreamFormatConverter bfConverter;
    @Autowired
    WorkspaceItemConverter workspaceItemConverter;
    @Autowired(required = true)
    ResourcePolicyConverter aCConverter;

    public WorkspaceItem createWorkspaceItem(Context context, Request request) throws SQLException, AuthorizeException {
        WorkspaceItem wsi = null;
        String collectionUUID = request.getHttpServletRequest().getParameter("collection");
        if (StringUtils.isBlank(collectionUUID)) {
            collectionUUID = configurationService.getProperty("submission.default.collection");
        }

        Collection collection = null;
        if (StringUtils.isNotBlank(collectionUUID)) {
            collection = collectionService.find(context, UUID.fromString(collectionUUID));
        } else {
            collection = collectionService.findAll(context, 1, 0).get(0);
        }
        wsi = workspaceItemService.create(context, collection, true);

        context.addEvent(new Event(Event.MODIFY, Constants.ITEM, wsi.getItem().getID(), null,
            itemService.getIdentifiers(context, wsi.getItem())));
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
        data.setFormat(bfConverter.convert(source.getFormat(ContextUtil.obtainContext(request))));

        for (ResourcePolicy rp : source.getResourcePolicies()) {
            if (ResourcePolicy.TYPE_CUSTOM.equals(rp.getRpType())) {
                ResourcePolicyRest resourcePolicyRest = aCConverter.convert(rp);
                data.getAccessConditions().add(resourcePolicyRest);
            }
        }

        data.setUuid(source.getID());
        CheckSumRest checksum = new CheckSumRest();
        checksum.setCheckSumAlgorithm(source.getChecksumAlgorithm());
        checksum.setValue(source.getChecksum());
        data.setCheckSum(checksum);
        data.setSizeBytes(source.getSize());
        data.setUrl(configurationService.getProperty("dspace.url") + "/api/" + BitstreamRest.CATEGORY + "/" + English
            .plural(BitstreamRest.NAME) + "/" + source.getID() + "/content");
        return data;
    }

    public XmlWorkflowItem createWorkflowItem(Context context, Request currentRequest)
            throws SQLException, AuthorizeException, WorkflowException {
        Reader reader = null;
        XmlWorkflowItem wi = null;
        try {
            reader = currentRequest.getHttpServletRequest().getReader();
            char[] arr = new char[1024];
            StringBuilder buffer = new StringBuilder();
            int numCharsRead = reader.read(arr, 0, arr.length);
            buffer.append(arr, 0, numCharsRead);
            if (numCharsRead == arr.length) {
                throw new RuntimeException("Malformed body... too long");
            }
            String regex = "\\/api\\/" + WorkspaceItemRest.CATEGORY + "\\/" + English.plural(WorkspaceItemRest.NAME)
                    + "\\/";
            String[] split = buffer.toString().split(regex, 2);
            if (split.length != 2) {
                throw new RuntimeException("Malformed body..." + buffer);
            }
            WorkspaceItem wsi = workspaceItemService.find(context, Integer.parseInt(split[1]));

            if (!workspaceItemConverter.convert(wsi).getErrors().isEmpty()) {
                throw new UnprocessableEntityException(
                        "Start workflow failed due to validation error on workspaceitem");
            }

            wi = workflowService.start(context, wsi);

            context.addEvent(new Event(Event.MODIFY, Constants.ITEM, wi.getItem().getID(), null,
                    itemService.getIdentifiers(context, wi.getItem())));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        return wi;
    }

    public void saveWorkflowItem(Context context, XmlWorkflowItem source) throws SQLException, AuthorizeException {
        workflowItemService.update(context, source);
    }
}
