/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security.service;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.content.security.AccessItemMode;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Service to verify if a given item can be access.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface CrisSecurityService {

    /**
     * Check if the given user can access the given item with the provided access
     * item mode.
     *
     * @param  context      the DSpace context
     * @param  item         the item
     * @param  user         the user
     * @param  accessMode   the configured access item mode
     * @return              true if the given user can access the item, false
     *                      otherwise
     * @throws SQLException is an SQL error occurs
     */
    boolean hasAccess(Context context, Item item, EPerson user, AccessItemMode accessMode) throws SQLException;

}
