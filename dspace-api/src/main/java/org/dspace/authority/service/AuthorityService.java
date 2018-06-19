/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Service interface class for the Metadata Authority
 * The implementation of this class is responsible for all business logic calls for the Metadata Authority and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface AuthorityService {

    public void indexItem(Context context, Item item) throws SQLException, AuthorizeException;

    public boolean isConfigurationValid();

}
