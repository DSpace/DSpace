/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem.service;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.dspace.app.requestitem.RequestItem;
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
    public String createRequest(Context context, Bitstream bitstream, Item item,
            boolean allFiles, String reqEmail, String reqName, String reqMessage)
        throws SQLException;

    /**
     * Fetch all item requests.
     *
     * @param context current DSpace session.
     * @return all item requests.
     * @throws java.sql.SQLException passed through.
     */
    public List<RequestItem> findAll(Context context)
            throws SQLException;

    /**
     * Retrieve a request by its token.
     *
     * @param context current DSpace session.
     * @param token the token identifying the request.
     * @return the matching request, or null if not found.
     */
    public RequestItem findByToken(Context context, String token);

    /**
     * Retrieve a request based on the item.
     * @param context current DSpace session.
     * @param item the item to find requests for.
     * @return the matching requests, or null if not found.
     */
    public Iterator<RequestItem> findByItem(Context context, Item item) throws SQLException;

    /**
     * Save updates to the record. Only accept_request, and decision_date are set-able.
     *
     * @param context     The relevant DSpace Context.
     * @param requestItem requested item
     */
    public void update(Context context, RequestItem requestItem);

    /**
     * Remove the record from the database.
     *
     * @param context current DSpace context.
     * @param request record to be removed.
     */
    public void delete(Context context, RequestItem request);

    /**
     * Is there at least one valid READ resource policy for this object?
     * @param context current DSpace session.
     * @param o the object.
     * @return true if a READ policy applies.
     * @throws SQLException passed through.
     */
    public boolean isRestricted(Context context, DSpaceObject o)
            throws SQLException;
}
