/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;
import java.util.List;

/**
 * Class to handle WorkspaceItems which are being supervised.
 *
 * @author Richard Jones
 * @version  $Revision$
 */
public interface SupervisedItemService
{
    /**
     * Get all workspace items which are being supervised
     *
     * @param context the context this object exists in
     *
     * @return array of SupervisedItems
     * @throws SQLException if database error
     */
    public List<WorkspaceItem> getAll(Context context) throws SQLException;


    /**
     * Get items being supervised by given EPerson
     *
     * @param   ep          the eperson who's items to supervise we want
     * @param   context     the dspace context
     *
     * @return the items eperson is supervising in an array
     * @throws SQLException if database error
     */
    public List<WorkspaceItem> findbyEPerson(Context context, EPerson ep)
        throws SQLException;
}
