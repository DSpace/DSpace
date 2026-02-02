/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem.service;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service interface class for the RequestItem object.
 * The implementation of this class is responsible for all business logic calls
 * for the RequestItem object and is autowired by Spring.
 *
 * @author kevinvandevelde at atmire.com
 * @author Kim Shepherd
 */
public interface RequestItemService {

    /**
     * Generate a request item representing the request and put it into the DB
     *
     * @param context    The relevant DSpace Context.
     * @param bitstream  The requested bitstream
     * @param item       The requested item
     * @param reqMessage Request message text
     * @param allFiles   true indicates that all bitstreams of this item are requested
     * @param reqEmail   email
     *                   Requester email
     * @param reqName    Requester name
     * @return the token of the request item
     * @throws SQLException if database error
     */
    String createRequest(Context context, Bitstream bitstream, Item item,
            boolean allFiles, String reqEmail, String reqName, String reqMessage)
        throws SQLException;

    /**
     * Fetch all item requests.
     *
     * @param context current DSpace session.
     * @return all item requests.
     * @throws SQLException passed through.
     */
    List<RequestItem> findAll(Context context)
            throws SQLException;

    /**
     * Retrieve a request by its approver token.
     *
     * @param context current DSpace session.
     * @param token the token identifying the request to be approved.
     * @return the matching request, or null if not found.
     */
    RequestItem findByToken(Context context, String token);

    /**
     * Retrieve a request by its access token, for use by the requester
     *
     * @param context current DSpace session.
     * @param token the token identifying the request to be temporarily accessed
     * @return the matching request, or null if not found.
     */
    RequestItem findByAccessToken(Context context, String token);
    /**
     * Retrieve a request based on the item.
     * @param context current DSpace session.
     * @param item the item to find requests for.
     * @return the matching requests, or null if not found.
     */
    Iterator<RequestItem> findByItem(Context context, Item item) throws SQLException;

    /**
     * Save updates to the record. Only accept_request, decision_date, access_period are settable.
     *
     * Note: the "is settable" rules mentioned here are enforced in RequestItemRest with annotations meaning that
     * these JSON properties are considered READ-ONLY by the core DSpaceRestRepository methods
     *
     * @param context     The relevant DSpace Context.
     * @param requestItem requested item
     */
    void update(Context context, RequestItem requestItem);

    /**
     * Remove the record from the database.
     *
     * @param context current DSpace context.
     * @param request record to be removed.
     */
    void delete(Context context, RequestItem request);

    /**
     * Is there at least one valid READ resource policy for this object?
     * @param context current DSpace session.
     * @param o the object.
     * @return true if a READ policy applies.
     * @throws SQLException passed through.
     */
    boolean isRestricted(Context context, DSpaceObject o)
            throws SQLException;

    /**
     * Set the access expiry timestamp for a request item. After this date, the
     * bitstream(s) will no longer be available for download even with a token.
     * @param requestItem the request item
     * @param accessExpiry the expiry timestamp
     */
    void setAccessExpiry(RequestItem requestItem, Instant accessExpiry);

    /**
     * Set the access expiry timestamp for a request item by delta string.
     * After this date, the bitstream(s) will no longer be available for download
     * even with a token.
     * @param requestItem the request item
     * @param delta the delta to calculate the expiry timestamp, from the decision date
     */
    void setAccessExpiry(RequestItem requestItem, String delta);

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
    void authorizeAccessByAccessToken(Context context, RequestItem requestItem, Bitstream bitstream,
                                             String accessToken)
            throws AuthorizeException;

    /**
     * Taking into account 'accepted' flag, bitstream id or allfiles flag, decision date and access period,
     * either return cleanly or throw an AuthorizeException
     *
     * @param context the DSpace context
     * @param bitstream the bitstream to which access is requested
     * @param accessToken the access token supplied by the user (e.g. to REST controller)
     * @throws AuthorizeException
     */
    void authorizeAccessByAccessToken(Context context, Bitstream bitstream, String accessToken)
        throws AuthorizeException;

    /**
     * Generate a link back to DSpace, to act on a request.
     *
     * @param token identifies the request.
     * @return URL to the item request API, with the token as request parameter
     *          "token".
     * @throws URISyntaxException passed through.
     * @throws MalformedURLException passed through.
     */
    String getLinkTokenEmail(String token)
            throws URISyntaxException, MalformedURLException;

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
    void sanitizeRequestItem(Context context, RequestItem requestItem);
}
