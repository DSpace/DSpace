/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package sk.dtq.dspace.app.util;

import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Class that represents single Access Control Entry
 *
 * @author Michal Josífko
 * @author milanmajchrak
 */

public class ACE {

    /** Logger */
    private static final Logger log = Logger.getLogger(ACE.class);

    private static final String POLICY_KEYWORD = "policy";

    private static final String POLICY_DENY_KEYWORD = "deny";

    private static final String POLICY_ALLOW_KEYWORD = "allow";

    private static final String ACTION_KEYWORD = "action";

    private static final String ACTION_READ_KEYWORD = "read";

    private static final String ACTION_WRITE_KEYWORD = "write";

    private static final String GRANTEE_TYPE_KEYWORD = "grantee-type";

    private static final String GRANTEE_TYPE_USER_KEYWORD = "user";

    private static final String GRANTEE_TYPE_GROUP_KEYWORD = "group";

    private static final String GRANTEE_ID_KEYWORD = "grantee-id";

    private static final String ANY_KEYWORD = "*";

    public static final int ACTION_READ = 1;

    public static final int ACTION_WRITE = 2;

    private static final int POLICY_DENY = 1;

    private static final int POLICY_ALLOW = 2;

    private static final int GRANTEE_TYPE_USER = 1;

    private static final int GRANTEE_TYPE_GROUP = 2;

    private static final String GRANTEE_ID_ANY = "-1";

    private int policy;

    private int action;

    private int granteeType;

    private String granteeID;

    /**
     * Creates new ACE object from given String
     *
     * @param s
     * @return ACE object or null
     */

    public static ACE fromString(String s) {
        ACE ace = null;
        String[] aceParts = s.split(",");

        int errors = 0;

        int policy = 0;
        int action = 0;
        int granteeType = 0;
        String granteeID = "";

        for (int i = 0; i < aceParts.length; i++) {
            String acePart = aceParts[i];
            String keyValue[] = acePart.split("=");

            if (keyValue.length != 2) {
                log.error("Invalid ACE format: " + acePart);
                errors++;
                continue;
            }

            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            if (key.equals(POLICY_KEYWORD)) {
                if (value.equals(POLICY_DENY_KEYWORD)) {
                    policy = POLICY_DENY;
                } else if (value.equals(POLICY_ALLOW_KEYWORD)) {
                    policy = POLICY_ALLOW;
                } else {
                    log.error("Invalid ACE policy value: " + value);
                    errors++;
                }
            } else if (key.equals(ACTION_KEYWORD)) {
                if (value.equals(ACTION_READ_KEYWORD)) {
                    action = ACTION_READ;
                } else if (value.equals(ACTION_WRITE_KEYWORD)) {
                    action = ACTION_WRITE;
                } else {
                    log.error("Invalid ACE action value: " + value);
                    errors++;
                }
            } else if (key.equals(GRANTEE_TYPE_KEYWORD)) {
                if (value.equals(GRANTEE_TYPE_USER_KEYWORD)) {
                    granteeType = GRANTEE_TYPE_USER;
                } else if (value.equals(GRANTEE_TYPE_GROUP_KEYWORD)) {
                    granteeType = GRANTEE_TYPE_GROUP;
                } else {
                    log.error("Invalid ACE grantee type value: " + value);
                    errors++;
                }
            } else if (key.equals(GRANTEE_ID_KEYWORD)) {
                if (value.equals(ANY_KEYWORD)) {
                    granteeID = GRANTEE_ID_ANY;
                } else {
                    granteeID = value;
                }
            } else {
                log.error("Invalid ACE keyword: " + key);
                errors++;
            }
        }
        if (errors == 0) {
            ace = new ACE(policy, action, granteeType, granteeID);
        }
        return ace;
    }

    /**
     * Constructor for creating new Access Control Entry
     *
     * @param policy
     * @param action
     * @param granteeType
     * @param granteeID
     */

    private ACE(int policy, int action, int granteeType, String granteeID) {
        this.policy = policy;
        this.action = action;
        this.granteeType = granteeType;
        this.granteeID = granteeID;
    }

    /**
     * Method that checks whether the given inputs match this Access Control
     * Entry
     *
     * @param userID
     * @param groupIDs
     * @param action
     * @return
     */

    public boolean matches(String userID, Set<String> groupIDs, int action) {
        if (this.action == action) {
            if (granteeType == ACE.GRANTEE_TYPE_USER) {
                if (granteeID.equals(GRANTEE_ID_ANY) || userID.equals(granteeID)) {
                    return true;
                }
            } else if (granteeType == ACE.GRANTEE_TYPE_GROUP) {
                if (granteeID.equals(GRANTEE_ID_ANY) || groupIDs.contains(granteeID)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Convenience method to verify if this entry is allowing the action;
     *
     * @return
     */

    public boolean isAllowed() {
        return policy == ACE.POLICY_ALLOW;
    }

}
