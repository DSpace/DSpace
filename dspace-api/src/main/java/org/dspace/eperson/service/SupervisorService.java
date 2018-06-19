/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

import java.sql.SQLException;

/**
 * Class to represent the supervisor, primarily for use in applying supervisor
 * activities to the database, such as setting and unsetting supervision
 * orders and so forth.
 *
 * @author  Richard Jones
 * @version $Revision$
 */
public interface SupervisorService {

    /** value to use for no policy set */
    public static final int POLICY_NONE = 0;

    /** value to use for editor policies */
    public static final int POLICY_EDITOR = 1;

    /** value to use for observer policies */
    public static final int POLICY_OBSERVER = 2;

    /**
     * finds out if there is a supervision order that matches this set
     * of values
     *
     * @param context   the context this object exists in
     * @param workspaceItem  the workspace item to be supervised
     * @param group   the group to be doing the supervising
     *
     * @return boolean  true if there is an order that matches, false if not
     */
    public boolean isOrder(Context context, WorkspaceItem workspaceItem, Group group)
        throws SQLException;

    /**
     * removes the requested group from the requested workspace item in terms
     * of supervision.  This also removes all the policies that group has
     * associated with the item
     *
     * @param context   the context this object exists in
     * @param workspaceItem  the ID of the workspace item
     * @param group   the ID of the group to be removed from the item
     */
    public void remove(Context context, WorkspaceItem workspaceItem, Group group)
        throws SQLException, AuthorizeException;

    /**
     * adds a supervision order to the database
     *
     * @param context   the context this object exists in
     * @param group   the ID of the group which will supervise
     * @param workspaceItem  the ID of the workspace item to be supervised
     * @param policy    String containing the policy type to be used
     */
    public void add(Context context, Group group, WorkspaceItem workspaceItem, int policy)
        throws SQLException, AuthorizeException;
}
