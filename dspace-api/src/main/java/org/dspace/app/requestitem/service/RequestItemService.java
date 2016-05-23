/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem.service;

import java.sql.SQLException;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service interface class for the RequestItem object.
 * The implementation of this class is responsible for all business logic calls for the RequestItem object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface RequestItemService {

    /**
     * Generate a request item representing the request and put it into the DB
     * @param context context
     * @param bitstream bitstream
     * @param item item
     * @param reqMessage message
     * @param allFiles all files flag
     * @param reqEmail email
     * @param reqName name
     * @return the token of the request item
     * @throws SQLException if database error
     */
    public String createRequest(Context context, Bitstream bitstream, Item item, boolean allFiles, String reqEmail, String reqName, String reqMessage)
            throws SQLException;

    public RequestItem findByToken(Context context, String token);

    /**
     * Save updates to the record. Only accept_request, and decision_date are set-able.
     * @param context
     * @param requestItem
     */
    public void update(Context context, RequestItem requestItem);



}
