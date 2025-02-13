/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.requestitem.dao.RequestItemDAO;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the RequestItem object.
 * This class is responsible for all business logic calls for the RequestItem
 * object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 * @author Kim Shepherd
 */
public class RequestItemServiceImpl implements RequestItemService {

    private final Logger log = LogManager.getLogger();

    @Autowired(required = true)
    protected RequestItemDAO requestItemDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;

    @Autowired
    protected ConfigurationService configurationService;

    private static final int DEFAULT_MINIMUM_FILE_SIZE = 20;

    protected RequestItemServiceImpl() {

    }

    /**
     * Create a new request-a-copy item request.
     *
     * @param context    The relevant DSpace Context.
     * @param bitstream  The requested bitstream
     * @param item       The requested item
     * @param allFiles   true indicates that all bitstreams of this item are requested
     * @param reqEmail   email
     *                   Requester email
     * @param reqName    Requester name
     * @param reqMessage Request message text
     * @return token to be used to approver for grant/deny
     * @throws SQLException
     */
    @Override
    public String createRequest(Context context, Bitstream bitstream, Item item,
            boolean allFiles, String reqEmail, String reqName, String reqMessage)
            throws SQLException {

        // Create an empty request item
        RequestItem requestItem = requestItemDAO.create(context, new RequestItem());

        // Set values of the request item based on supplied parameters
        requestItem.setToken(Utils.generateHexKey());
        requestItem.setBitstream(bitstream);
        requestItem.setItem(item);
        requestItem.setAllfiles(allFiles);
        requestItem.setReqEmail(reqEmail);
        requestItem.setReqName(reqName);
        requestItem.setReqMessage(reqMessage);
        requestItem.setRequest_date(Instant.now());

        // If the 'link' feature is enabled and the filesize threshold is met, pre-generate access token now
        // so it can be previewed by approver and so Angular and REST services can use the existence of this token
        // as an indication of which delivery method to use.
        // Access period will be created upon actual approval.
        if (configurationService.getBooleanProperty("request.item.grant.link", false)) {
            // The 'send link' feature is enabled, is the file(s) requested over the size threshold (megabytes as int)?
            // Default is 20MB minimum. For inspection purposes we convert to bytes.
            long minimumSize = configurationService.getLongProperty(
                    "request.item.grant.link.filesize", DEFAULT_MINIMUM_FILE_SIZE) * 1024 * 1024;
            // If we have a single bitstream, we will initialise the "minimum threshold reached" correctly
            boolean minimumSizeThresholdReached = (null != bitstream && bitstream.getSizeBytes() >= minimumSize);
            // If all files (and presumably no min reached since bitstream should be null), we look for ANY >= min size
            if (!minimumSizeThresholdReached && allFiles) {
                // Iterate bitstream and inspect file sizes. At each loop iteration we will break out if the min
                // was already reached.
                String[] bundleNames = configurationService.getArrayProperty("request.item.grant.link.bundles",
                        new String[]{"ORIGINAL"});
                for (String bundleName : bundleNames) {
                    if (!minimumSizeThresholdReached) {
                        for (Bundle bundle : item.getBundles(bundleName)) {
                            if (null != bundle && !minimumSizeThresholdReached) {
                                for (Bitstream bitstreamToCheck : bundle.getBitstreams()) {
                                    if (bitstreamToCheck.getSizeBytes() >= minimumSize) {
                                        minimumSizeThresholdReached = true;
                                        break;
                                    }
                                }

                            }
                        }
                    }
                }
            }

            // Now, only generate and set an access token if the minimum file size threshold was reached.
            // Otherwise, an email attachment will still be used.
            // From now on, the existence of an access token in the RequestItem indicates that a web link should be
            // sent instead of attaching file(s) as an attachment.
            if (minimumSizeThresholdReached) {
                requestItem.setAccess_token(Utils.generateHexKey());
            }
        }

        // Save the request item
        requestItemDAO.save(context, requestItem);

        log.debug("Created RequestItem with ID {}, approval token {}, access token {}, access period {}",
                requestItem::getID, requestItem::getToken, requestItem::getAccess_token, requestItem::getAccess_period);

        // Return the approver token
        return requestItem.getToken();
    }

    @Override
    public List<RequestItem> findAll(Context context)
            throws SQLException {
        return requestItemDAO.findAll(context, RequestItem.class);
    }

    @Override
    public RequestItem findByToken(Context context, String token) {
        try {
            return requestItemDAO.findByToken(context, token);
        } catch (SQLException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public Iterator<RequestItem> findByItem(Context context, Item item) throws SQLException {
        return requestItemDAO.findByItem(context, item);
    }

    @Override
    public void update(Context context, RequestItem requestItem) {
        try {
            requestItemDAO.save(context, requestItem);
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void delete(Context context, RequestItem requestItem) {
        log.debug(LogHelper.getHeader(context, "delete_itemrequest", "request_id={}"),
                requestItem.getID());
        try {
            requestItemDAO.delete(context, requestItem);
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public boolean isRestricted(Context context, DSpaceObject o)
            throws SQLException {
        List<ResourcePolicy> policies = authorizeService
                .getPoliciesActionFilter(context, o, Constants.READ);
        for (ResourcePolicy rp : policies) {
            if (resourcePolicyService.isDateValid(rp)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Find a request item by its access token. This is the token that a requester would use
     * to authenticate themselves as a granted requester.
     * It is up to the RequestItemRepository to check validity of the item, access granted, data sanitization, etc.
     *
     * @param context current DSpace session.
     * @param accessToken the token identifying the request to be temporarily accessed
     * @return request item data
     */
    @Override
    public RequestItem findByAccessToken(Context context, String accessToken) {
        try {
            return requestItemDAO.findByAccessToken(context, accessToken);
        } catch (SQLException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Taking into account 'accepted' flag, bitstream id or allfiles flag, decision date and access period,
     * either return cleanly or throw an AuthorizeException
     *
     * @param context the DSpace context
     * @param requestItem the request item containing request and approval data
     * @param bitstream the bitstream to which access is requested
     * @param accessToken the access token supplied by the user (e.g. to REST controller)
     * @throws AuthorizeException
     */
    @Override
    public void authorizeAccessByAccessToken(Context context, RequestItem requestItem, Bitstream bitstream,
                                             String accessToken) throws AuthorizeException {
        if (requestItem == null || bitstream == null || context == null || accessToken == null) {
            throw new AuthorizeException("Null resources provided, not authorized");
        }
        // 1. Request is accepted
        if (requestItem.isAccept_request()
                // 2. Request access token is not null and matches supplied string
                && (requestItem.getAccess_token() != null && requestItem.getAccess_token().equals(accessToken))
                // 3. Request is 'allfiles' or for this bitstream ID
                && (requestItem.isAllfiles() || bitstream.equals(requestItem.getBitstream()))
                // 4. access period is 0 (forever), or the elapsed seconds since decision date is less than the
                // access period granted
                && requestItem.accessPeriodCurrent()
        ) {
            log.info("Authorizing access to bitstream {} by access token", bitstream.getID());
            return;
        }
        // Default, throw authorize exception
        throw new AuthorizeException("Unauthorized access to bitstream by access token for bitstream ID "
                + bitstream.getID());
    }

    /**
     * Taking into account 'accepted' flag, bitstream id or allfiles flag, decision date and access period,
     * either return cleanly or throw an AuthorizeException
     *
     * @param context the DSpace context
     * @param bitstream the bitstream to which access is requested
     * @param accessToken the access token supplied by the user (e.g. to REST controller)
     * @throws AuthorizeException
     */
    @Override
    public void authorizeAccessByAccessToken(Context context, Bitstream bitstream, String accessToken)
            throws AuthorizeException {
        if (bitstream == null || context == null || accessToken == null) {
            throw new AuthorizeException("Null resources provided, not authorized");
        }
        // get request item from access token
        RequestItem requestItem = findByAccessToken(context, accessToken);
        if (requestItem == null) {
            throw new AuthorizeException("Null item request provided, not authorized");
        }
        // Continue with authorization check
        authorizeAccessByAccessToken(context, requestItem, bitstream, accessToken);
    }

    /**
     * Generate a link back to DSpace, to act on a request.
     *
     * @param token identifies the request.
     * @return URL to the item request API, with the token as request parameter
     *          "token".
     * @throws URISyntaxException passed through.
     * @throws MalformedURLException passed through.
     */
    @Override
    public String getLinkTokenEmail(String token)
            throws URISyntaxException, MalformedURLException {
        final String base = configurationService.getProperty("dspace.ui.url");
        URIBuilder uriBuilder = new URIBuilder(base);
        String currentPath = uriBuilder.getPath();
        String newPath = (currentPath == null || currentPath.isEmpty() || currentPath.equals("/"))
                ? "/request-a-copy/" + token
                : currentPath + "/request-a-copy/" + token;
        URI uri = uriBuilder.setPath(newPath).build();
        return uri.toURL().toExternalForm();
    }

    /**
     * Sanitize a RequestItem depending on the current session user. If the current user is not
     * the approver, an administrator or other privileged group, the following values in the return object
     * are nullified:
     * - approver token (aka token)
     * - requester name
     * - requester email
     * - requester message
     *
     * These properties contain personal information, or can be used to access personal information
     * and are not needed except for sending the original request and grant/deny emails
     *
     * @param requestItem
     */
    @Override
    public void sanitizeRequestItem(Context context, RequestItem requestItem) {
        if (null == requestItem) {
            log.error("Null request item passed for sanitization, skipping");
            return;
        }
        if (null != context) {
            // Get current user, if any
            EPerson currentUser = context.getCurrentUser();
            // Get item
            Item item = requestItem.getItem();
            if (null != currentUser) {
                try {
                    if (currentUser == requestItem.getItem().getSubmitter()
                            && authorizeService.isAdmin(context, requestItem.getItem())) {
                        // Return original object, this person technically had full access to the request item data via
                        // the original approval link
                        log.debug("User is authorized to receive all request item data: {}", currentUser.getEmail());
                    }
                } catch (SQLException e) {
                    log.error("Could not determine isAdmin for item {}: {}",item.getID(), e.getMessage());
                }
            }
        }

        // By default, sanitize (strips requester name, email, message, and the approver token)
        // This is the case if we have a non-admin, non-submitter or a null user/session
        requestItem.sanitizePersonalData();

    }
}
