/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.springframework.stereotype.Component;

/**
 * Class that represents Access Control List
 *
 * @author Michal Josífko
 * Class is copied from the LINDAT/CLARIAH-CZ (https://github.com/ufal/clarin-dspace) and modified by
 * @author Milan Majchrak (milan.majchrak at dataquest dot sk)
 */

@Component
public class ACL {

    /** Logger */
    private static final Logger log = Logger.getLogger(ACL.class);
    public static final int ACTION_READ = ACE.ACTION_READ;
    public static final int ACTION_WRITE = ACE.ACTION_WRITE;
    /**
     * List of single Access Control Entry
     */
    private List<ACE> acl;
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    /**
     * Creates new ACL object from given String
     *
     * @param aclDefinition of the field from the form definition file
     * @return ACL object
     */
    public static ACL fromString(String aclDefinition) {
        List<ACE> acl = new ArrayList<ACE>();
        if (aclDefinition != null) {
            String[] aclEntries = aclDefinition.split(";");
            for (int i = 0; i < aclEntries.length; i++) {
                String aclEntry = aclEntries[i];
                ACE ace = ACE.fromString(aclEntry);
                if (ace != null) {
                    acl.add(ace);
                }
            }
        }
        return new ACL(acl);
    }

    /**
     * Constructor for creating new Access Control List
     *
     * @param acl List of ACE
     */
    ACL(List<ACE> acl) {
        this.acl = acl;
    }

    /**
     * Method to verify whether the the given user ID and set of group IDs is
     * allowed to perform the given action
     *
     * @param userID current user
     * @param groupIDs where is assigned the current user
     * @param action read/write
     * @return if user will see the input field
     */
    private boolean isAllowedAction(String userID, Set<String> groupIDs, int action) {
        for (ACE ace : acl) {
            if (ace.matches(userID, groupIDs, action)) {
                return ace.isAllowed();
            }
        }
        return false;
    }

    /**
     * Convenience method to verify whether the current user is allowed to
     * perform given action based on current context
     *
     * @param c Current context, the user information are loaded from the context
     * @param action read/write
     * @return if user will see the input field
     * @throws SQLException
     */
    public boolean isAllowedAction(Context c, int action) {
        boolean res = false;
        if (acl.isEmpty()) {
            // To maintain backwards compatibility allow everything if the ACL
            // is empty
            return true;
        }
        try {
            if (authorizeService.isAdmin(c)) {
                // Admin is always allowed
                return true;
            } else {
                EPerson e = c.getCurrentUser();
                if (e != null) {
                    UUID userID = e.getID();
                    List<Group> groups = groupService.allMemberGroups(c, c.getCurrentUser());

                    Set<String> groupIDs = groups.stream().flatMap(group -> Stream.of(group.getID().toString()))
                            .collect(Collectors.toSet());

                    return isAllowedAction(userID.toString(), groupIDs, action);
                }
            }
        } catch (SQLException e) {
            log.error(e);
        }
        return res;
    }

    /**
     * Returns true is the ACL is empty set of rules
     *
     * @return contains some ACE elements
     */
    public boolean isEmpty() {
        return acl.isEmpty();
    }

}
