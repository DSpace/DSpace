/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.ShareSubmissionLinkDTO;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class' purpose is to provide an API for sharing an in-progress submission. It allows the user to generate
 * a share link for a workspace item and to set the owner of the workspace item to the current user.
 *
 * @author Milan Majchrak (dspace at dataquest.sk)
 */
@RestController
@RequestMapping("/api/" + RestAddressableModel.SUBMISSION)
public class SubmissionController {
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(SubmissionController.class);

    @Autowired
    WorkspaceItemService workspaceItemService;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    protected Utils utils;

    @Autowired
    AuthorizeService authorizeService;

    @Lazy
    @Autowired
    protected ConverterService converter;

    @PreAuthorize("hasPermission(#wsoId, 'WORKSPACEITEM', 'WRITE')")
    @RequestMapping(method = RequestMethod.GET, value = "share")
    public ResponseEntity<ShareSubmissionLinkDTO> generateShareLink(@RequestParam(name = "workspaceitemid")
                                                                        Integer wsoId, HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        // Check the context is not null
        this.validateContext(context);

        // Get workspace item from ID
        WorkspaceItem wsi = workspaceItemService.find(context, wsoId);
        // Check the wsi does exist
        validateWorkspaceItem(wsi, wsoId, null);

        // Generate a share link
        String shareToken = generateShareToken();

        // Update workspace item with share link
        wsi.setShareToken(shareToken);
        workspaceItemService.update(context, wsi);
        // Without commit the changes are not persisted into the database
        context.commit();

        // Get submitter email
        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            String errorMessage = "The current user is not valid, it cannot be null.";
            log.error(errorMessage);
            throw new BadRequestException(errorMessage);
        }

        // Send email to submitter with share link
        String shareLink = sendShareLinkEmail(context, wsi, currentUser);
        if (StringUtils.isEmpty(shareLink)) {
            String errorMessage = "The share link is empty.";
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        // Create a DTO with the share link for better processing in the FE
        ShareSubmissionLinkDTO shareSubmissionLinkDTO = new ShareSubmissionLinkDTO();
        shareSubmissionLinkDTO.setShareLink(shareLink);

        // Send share link in response
        return ResponseEntity.ok().body(shareSubmissionLinkDTO);
    }

    @PreAuthorize("hasPermission(#wsoId, 'WORKSPACEITEM', 'WRITE')")
    @RequestMapping(method = RequestMethod.GET, value = "setOwner")
    public WorkspaceItemRest setOwner(@RequestParam(name = "shareToken") String shareToken,
                                      @RequestParam(name = "workspaceitemid") Integer wsoId,
                                      HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        // Check the context is not null
        this.validateContext(context);

        // Get workspace by share token
        List<WorkspaceItem> wsiList = workspaceItemService.findByShareToken(context, shareToken);
        // Check the wsi does exist
        if (CollectionUtils.isEmpty(wsiList)) {
            String errorMessage = "The workspace item with share token:" + shareToken + " does not exist.";
            log.error(errorMessage);
            throw new BadRequestException(errorMessage);
        }

        // Get the first workspace item - the only one
        WorkspaceItem wsi = wsiList.get(0);
        // Check the wsi does exist
        validateWorkspaceItem(wsi, null, shareToken);

        if (!authorizeService.authorizeActionBoolean(context, wsi.getItem(), Constants.READ)) {
            String errorMessage = "The current user does not have rights to view the WorkflowItem";
            log.error(errorMessage);
            throw new AccessDeniedException(errorMessage);
        }

        // Set the owner of the workspace item to the current user
        EPerson currentUser = context.getCurrentUser();
        // If the current user is null, throw an exception
        if (currentUser == null) {
            String errorMessage = "The current user is not valid, it cannot be null.";
            log.error(errorMessage);
            throw new BadRequestException(errorMessage);
        }

        wsi.getItem().setSubmitter(currentUser);
        workspaceItemService.update(context, wsi);
        WorkspaceItemRest wsiRest = converter.toRest(wsi, utils.obtainProjection());

        // Without commit the changes are not persisted into the database
        context.commit();
        return wsiRest;
    }

    private static String generateShareToken() {
        // UUID generates a 36-char string with hyphens, so we can strip them to get a 32-char string
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private String sendShareLinkEmail(Context context, WorkspaceItem wsi, EPerson currentUser) {
        // Get the UI URL from the configuration
        String uiUrl = configurationService.getProperty("dspace.ui.url");
        // Get submitter email
        String email = currentUser.getEmail();
        // Compose the url with the share token. The user will be redirected to the UI.
        String shareTokenUrl = uiUrl + "/share-submission/change-submitter?share_token=" + wsi.getShareToken() +
                "&workspaceitemid=" + wsi.getID();
        try {
            Locale locale = context.getCurrentLocale();
            Email bean = Email.getEmail(I18nUtil.getEmailFilename(locale, "share_submission"));
            bean.addArgument(shareTokenUrl);
            bean.addRecipient(email);
            bean.send();
        } catch (MessagingException | IOException e) {
            String errorMessage = "Unable send the email because: " + e.getMessage();
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        return shareTokenUrl;
    }

    /**
     * Check if the context is valid - not null. If not, throw an exception.
     */
    private void validateContext(Context context) {
        if (context == null) {
            String errorMessage = "The current context is not valid, it cannot be null.";
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * Check if the workspace item is valid - not null. If not, throw an exception. The workspace item can be found by
     * ID or the share token.
     */
    private void validateWorkspaceItem(WorkspaceItem wsi, Integer wsoId, String shareToken) {
        if (wsi == null) {
            String identifier = wsoId != null ? wsoId.toString() : shareToken;
            String identifierName = wsoId != null ? "ID" : "share token";
            String errorMessage = "The workspace item with " + identifierName + ":" + identifier + " does not exist.";
            log.error(errorMessage);
            throw new BadRequestException(errorMessage);
        }
    }
}
