/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package ar.edu.unlp.sedici.util;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

public class AuthorizeUtil {

	/**
	 * Check if the eperson is system admin
	 * @param c Context
	 * @param eperson EPerson to check for
	 * @return
	 * @throws SQLException 
	 */
	public static boolean isAdmin(Context c, EPerson eperson) throws SQLException {
		if(eperson == null)
			return false;
		return epersonInGroup(c, 1, eperson);
	}
	
	
    /**
     * copied from the AuthorizeMqanager.isAdmin(Context, DSpaceObject), to check for any eperson (not just the logged user)
     * 
     * Check to see if the user is an Administrator of a given object
     * within DSpace. Always return <code>true</code> if the user is a System
     * Admin
     * 
     * @param c
     *            current context
     * @param person
     *            EPerson
     * @param dspaceObject
     *            current DSpace Object, if <code>null</code> the call will be
     *            equivalent to a call to the <code>isAdmin(Context c)</code>
     *            method
     * 
     * @return <code>true</code> if user has administrative privileges on the
     *         given DSpace object
     */
    public static boolean isAdmin(Context c, EPerson eperson, DSpaceObject dspaceObject) throws SQLException {

    	if(eperson == null)
    		return false;
    	
		// return true if user is an Administrator
		if (isAdmin(c, eperson))
		    return true;
	
		if (dspaceObject == null)
		    return false;
		
        //
        // First, check all Resource Policies directly on this object
        //
		List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(c, dspaceObject, Constants.ADMIN);
        
        for (ResourcePolicy rp : policies) {
            // check policies for date validity
            if (rp.isDateValid()) {
                if ((rp.getEPersonID() != -1) && (rp.getEPersonID() == eperson.getID())) {
                    return true; // match
                }

                if ((rp.getGroupID() != -1) && (epersonInGroup(c, rp.getGroupID(), eperson)) ) {
                    // group was set, and eperson is a member of that group
                    return true;
                }
            }
        }

        // If user doesn't have specific Admin permissions on this object,
        // check the *parent* objects of this object.  This allows Admin
        // permissions to be inherited automatically (e.g. Admin on Community
        // is also an Admin of all Collections/Items in that Community)
        DSpaceObject parent = dspaceObject.getParentObject();
        if (parent != null) {
            return isAdmin(c, eperson, parent);
        }
	
		return false;
    }
    

    /**
     * copied from the private version in Group.epersonInGroup method
     */
    public static boolean epersonInGroup(Context c, int groupID, EPerson e) throws SQLException {
        Set<Integer> groupIDs = Group.allMemberGroupIDs(c, e);
        return groupIDs.contains(Integer.valueOf(groupID));
    }

}
