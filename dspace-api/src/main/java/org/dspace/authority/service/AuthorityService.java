/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service interface class for the Metadata Authority.
 * The implementation of this class is responsible for all business logic calls
 * for the Metadata Authority and is autowired by Spring.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface AuthorityService {

    /**
     * Add an {@link Item} to the authority index.
     * @param context current DSpace session.
     * @param item the Item to be added.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    public void indexItem(Context context, Item item) throws SQLException, AuthorizeException;

    public boolean isConfigurationValid();

}
