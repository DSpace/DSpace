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
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

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
import org.dspace.services.ConfigurationService;
import org.dspace.util.DateMathParser;
import org.dspace.util.MultiFormatDateParser;
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

    /**
     * Always set UTC for dateMathParser for consistent database date handling
     */
    static DateMathParser dateMathParser = new DateMathParser(TimeZone.getTimeZone("UTC"));

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

        log.debug("Created RequestItem with ID {}, approval token {}, access token {}, access expiry {}",
                requestItem::getID, requestItem::getToken, requestItem::getAccess_token, requestItem::getAccess_expiry);

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
     * Set the access expiry date for the request item.
     * @param requestItem the request item to update
     * @param accessExpiry the expiry date to set
     */
    @Override
    public void setAccessExpiry(RequestItem requestItem, Instant accessExpiry) {
        requestItem.setAccess_expiry(accessExpiry);
    }

    /**
     * Take a string either as a formatted date, or in the "math" format expected by
     * the DateMathParser, e.g. +7DAYS or +10MONTHS, and set the access expiry date accordingly.
     * There are no special checks here to check that the date is in the future, or after the
     * 'decision date', as there may be legitimate reasons to set past dates.
     * If past dates are not allowed by some interface, then the caller should check this.
     *
     * @param requestItem the request item to update
     * @param dateOrDelta the delta as a string in format expected by the DateMathParser
     */
    @Override
    public void setAccessExpiry(RequestItem requestItem, String dateOrDelta) {
        try {
            setAccessExpiry(requestItem, parseDateOrDelta(dateOrDelta, requestItem.getDecision_date()));
        } catch (ParseException e) {
            log.error("Error parsing access expiry or duration: {}", e.getMessage());
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
                // 4. access expiry timestamp is null (forever), or is *after* the current time
                && (requestItem.getAccess_expiry() == null || requestItem.getAccess_expiry().isAfter(Instant.now()))
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
     * Sanitize a RequestItem. The following values in the referenced RequestItem
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

        // Sanitized referenced data (strips requester name, email, message, and the approver token)
        requestItem.sanitizePersonalData();
    }

    /**
     * Parse a date or delta string into an Instant. Kept here as a static method for use in unit tests
     * and other areas that might not have access to the full spring service
     *
     * @param dateOrDelta
     * @param decisionDate
     * @return parsed date as instant
     * @throws ParseException
     */
    public static Instant parseDateOrDelta(String dateOrDelta, Instant decisionDate)
            throws ParseException, DateTimeException {
        // First, if dateOrDelta is a null string or "FOREVER", we will set the expiry
        // date to a very distant date in the future.
        if (dateOrDelta == null || dateOrDelta.equals("FOREVER")) {
            return Utils.getMaxTimestamp();
        }
        // Next, try parsing as a straight date using the multiple format parser
        ZonedDateTime parsedExpiryDate = MultiFormatDateParser.parse(dateOrDelta);

        if (parsedExpiryDate == null) {
            // That did not work, so try parsing as a delta
            // Set the 'now' date to the decision date of the request item
            dateMathParser.setNow(LocalDateTime.ofInstant(decisionDate, ZoneOffset.UTC));
            // Parse the delta (e.g. +7DAYS) and set the new access expiry date
            return dateMathParser.parseMath(dateOrDelta).toInstant(ZoneOffset.UTC);
        } else {
            // The expiry date was a valid formatted date string, so set the access expiry date
            return parsedExpiryDate.toInstant();
        }
    }
}
