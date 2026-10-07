/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem.dao;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the RequestItem object.
 * The implementation of this class is responsible for all database calls for
 * the RequestItem object and is autowired by Spring.
 * This class should only be accessed from a single service and should never be
 * exposed outside of the API.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface RequestItemDAO extends GenericDAO<RequestItem> {
    /**
     * Fetch a request named by its unique approval token (passed in emails).
     *
     * @param context the current DSpace context.
     * @param token uniquely identifies the request.
     * @return the found request (or {@code null}?)
     * @throws SQLException passed through.
     */
    public RequestItem findByToken(Context context, String token) throws SQLException;

    /**
     * Fetch a request named by its unique access token (passed in emails).
     * Note this is the token used by the requester to access an approved resource, not the token
     * used by the item submitter or helpdesk to grant the access.
     *
     * @param context the current DSpace context.
     * @param accessToken uniquely identifies the request
     * @return the found request or {@code null}
     * @throws SQLException passed through.
     */
    public RequestItem findByAccessToken(Context context, String accessToken) throws SQLException;

    /**
     * Fetch requests by item
     *
     * @param context current DSpace session.
     * @param item the item to find requests for.
     * @return the matching requests (or empty iterator)
     */
    public Iterator<RequestItem> findByItem(Context context, Item item) throws SQLException;

    /**
     * Retrieve all requests (as iterator) for a given bitstream UUID
     * A UUID parameter is used here rather than Bitstream object, to make it usable
     * in situations even when a bitstream object no longer exists, but orphaned
     * entries need to be found by their (previous) bitstream UUID.
     *
     * @param context current DSpace context
     * @param bitstreamId the bitstream UUID to search for
     * @return the matching requests (or empty iterator)
     */
    public Iterator<RequestItem> findByBitstreamId(Context context, UUID bitstreamId) throws SQLException;

}
