/*
 * CommunityGroup.java
 */

package org.dspace.content;

import java.sql.SQLException;

import org.dspace.core.Context;

/**
 * Class representing a community group.
 */
public class CommunityGroup
{
    /**
     * <pre>
     * Revision History
     *
     *   2004/10/21: Ben
     *     - initial version
     * </pre>
     */

    private static final int FACULTY   = 0;
    private static final int LIBRARIES = 1;
    private static final int UM        = 2;

    private Context context;

    private int id;

    /**
     * Construct a group object.
     */
    CommunityGroup(Context context, int id)
    {
	this.context = context;
	this.id = id;
    }


    /**
     * Get a list of all the community groups.
     *
     * @return list of groups
     */

    public static CommunityGroup[] findAll(Context context)
    {
	return new CommunityGroup[]{
	    new CommunityGroup(context, FACULTY),
	    new CommunityGroup(context, LIBRARIES),
	    new CommunityGroup(context, UM)
		};
    }


    /**
     * Get the top level communities in this group.
     *
     * @return  array of Community objects
     */

    public Community[] getCommunities()
	throws SQLException
    {
	return Community.findByCommunityGroupTop(context, this);
    }


    /**
     * Get the name of this community group.
     * @return the name of the group
     */

    public String getName()
    {
	switch (id) {
	case FACULTY:
	    return "UM Faculty: Individual Deposit Collections Organized by Department";
	case LIBRARIES:
	    return "Collections Managed by UM Libraries";
	case UM:
	    return "UM Community-managed Collections";
	default:
	    return "??";
	}
    }
	    

    /**
     * Get the short name of this community group.
     *
     * @param group_id the group id
     *
     * @return the name of the group
     */

    public String getShortName()
    {
	switch (id) {
	case FACULTY:
	    return "UM Faculty";
	case LIBRARIES:
	    return "UM Libraries";
	case UM:
	    return "UM Community";
	default:
	    return "??";
	}
    }
	    

    /**
     * Get the internal ID of this community group
     *
     * @return the internal identifier
     */

    public int getID()
    {
        return id;
    }


}
