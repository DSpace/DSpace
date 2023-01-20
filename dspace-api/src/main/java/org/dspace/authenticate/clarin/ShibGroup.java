/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate.clarin;
/* Created for LINDAT/CLARIN */

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Try to refactor the Shibboleth mess.
 *
 * Get groups a user should be put into according to several Shibboleth headers
 * and default configuration values.
 *
 * Class is copied from UFAL/CLARIN-DSPACE (https://github.com/ufal/clarin-dspace) and modified by
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ShibGroup {
    // variables
    //
    private static final Logger log = LogManager.getLogger(ShibGroup.class);
    private ShibHeaders shib_headers_ = null;
    private Context context_ = null;

    private static String defaultRoles;
    private static String roleHeader;
    private static boolean ignoreScope;
    private static boolean ignoreValue;

    ConfigurationService configurationService;
    GroupService groupService;
    // ctor
    //

    public ShibGroup(ShibHeaders shib_headers, Context context) {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        groupService = EPersonServiceFactory.getInstance().getGroupService();

        defaultRoles = configurationService.getProperty("authentication-shibboleth.default-roles");
        roleHeader = configurationService.getProperty("authentication-shibboleth.role-header");
        ignoreScope = configurationService
                .getBooleanProperty("authentication-shibboleth.role-header.ignore-scope", true);
        ignoreValue = configurationService
                .getBooleanProperty("authentication-shibboleth.role-header.ignore-value", false);

        shib_headers_ = shib_headers;
        context_ = context;

        if (ignoreScope && ignoreValue) {
            throw new IllegalStateException(
                    "Both config parameters for ignoring attributes scope and value are turned on, " +
                    "this is not a permissable configuration. (Note: ignore-scope defaults to true) " +
                    "The configuration parameters are: 'authentication.shib.role-header.ignore-scope' " +
                    "and 'authentication.shib.role-header.ignore-value'");
        }
    }

    /**
     * This is again a bit messy but the purpose is to find out into which groups an EPerson belongs; hence,
     * authorisation part from AAI.
     *
     *
     */

    public List<UUID> get() {
        try {
            log.debug("Starting to determine special groups");

            // Get afill from `authentication-shibboleth.header.entitlement` and from EmAIL HEADER
            /* <UFAL>
             * lets be evil and hack the email to the entitlement field
             */
            List<String> affiliations = new ArrayList<String>();

            affiliations.addAll(
                    get_affilations_from_roles(roleHeader));
            affiliations.addAll(
                    get_affilations_from_shib_mappings());

            /* </UFAL> */


            // If none affiliation was loaded
            if (affiliations.isEmpty()) {
                if (defaultRoles != null) {
                    affiliations = Arrays.asList(defaultRoles.split(","));
                }
                log.debug("Failed to find Shibboleth role header, '" + roleHeader + "', " +
                        "falling back to the default roles: '" + defaultRoles + "'");
            } else {
                log.debug("Found Shibboleth role header: '" + roleHeader + "' = '" + affiliations + "'");
            }

            // Loop through each affiliation
            //
            Set<UUID> groups = new HashSet<UUID>();
            if (affiliations != null) {
                for ( String affiliation : affiliations) {
                    // populate the organisation name
                    affiliation = populate_affiliation(affiliation, ignoreScope, ignoreValue);
                    // try to get the group names from authentication-shibboleth.cfg
                    String groupNames = get_group_names_from_affiliation(affiliation);

                    if (groupNames == null) {
                        log.debug("Unable to find role mapping for the value, '" + affiliation + "', " +
                                "there should be a mapping in the dspace.cfg:  authentication.shib.role."
                                + affiliation + " = <some group name>");
                        continue;
                    } else {
                        log.debug("Mapping role affiliation to DSpace group: '" + groupNames + "'");
                    }

                    // get the group ids
                    groups.addAll(string2groups(groupNames));

                } // foreach affiliations
            } // if affiliations

            //attribute -> group mapping
            //check shibboleth attribute ATTR and put users having value ATTR_VALUE1 and ATTR_VALUE2 to GROUP1
            //users having ATTR_VALUE3 to GROUP2
            //groups must exist
            //header.ATTR=ATTR_VALUE1=>GROUP1,ATTR_VALUE2=>GROUP1,ATTR_VALUE3=>GROUP2
            final String lookFor = "authentication-shibboleth.header.";
            ConfigurationService configurationService = new DSpace().getConfigurationService();
            Properties allShibbolethProperties = configurationService.getProperties();
            for (String propertyName : allShibbolethProperties.stringPropertyNames()) {
                //look for properties in authentication shibboleth that start with "header."
                if (propertyName.startsWith(lookFor)) {
                    String headerName = propertyName.substring(lookFor.length());
                    List<String> presentHeaderValues = shib_headers_.get(headerName);
                    if (!CollectionUtils.isEmpty(presentHeaderValues)) {
                        //if shibboleth sent any attributes under the headerName
                        String[] values2groups = configurationService.getPropertyAsType(
                                propertyName, String[].class);
                        for (String value2group : values2groups) {
                            String[] value2groupParts = value2group.split("=>", 2);
                            String headerValue = value2groupParts[0].trim();
                            String assignedGroup = value2groupParts[1].trim();
                            if (presentHeaderValues.contains(headerValue)) {
                                //our configured header value is present so add a group
                                groups.addAll(string2groups(assignedGroup));
                            }
                        }
                    }
                }
            }

            /* <UFAL>
             * Default group for shib authenticated users
             */
            Group default_group = get_default_group();
            if ( null != default_group ) {
                groups.add(default_group.getID());
            }
            /* </UFAL> */

            log.info("Added current EPerson to special groups: " + groups);
            // Convert from a Java Set to primitive ArrayList array
            return new ArrayList<>(groups);
        } catch (Throwable t) {
            log.error(
                    "Unable to validate any special groups this user may belong too because of an exception.",t);
            return new ArrayList<>();
        }
    }

    //
    //
    private List<String> get_affilations_from_roles(String roleHeader) {
        List<String> roleHeaderValues = shib_headers_.get(roleHeader);
        List<String> affiliations = new ArrayList<String>();

        // Get the Shib supplied affiliation or use the default affiliation
        // e.g., we can use 'entitlement' shibboleth header
        if (roleHeaderValues != null) {
            for (String roleHeaderValue : roleHeaderValues) {
                affiliations.addAll(string2values(roleHeaderValue));
            }
        }
        return affiliations;
    }

    private List<String> get_affilations_from_shib_mappings() {
        List<String> ret = new ArrayList<String>();
        String organization = shib_headers_.get_idp();
        // Try to get email based on utilities mapping database table
        //
        if (organization != null) {
            String email_header = configurationService.getProperty("authentication-shibboleth.email-header");
            if (email_header != null) {
                String email = shib_headers_.get_single(email_header);
                if (email != null) {
                    ret = string2values(email);
                }
            }
        }
        if ( ret == null ) {
            return new ArrayList<String>();
        }

        return ret;
    }

    private String populate_affiliation(String affiliation, boolean ignoreScope, boolean ignoreValue) {
        // If we ignore the affilation's scope then strip the scope if it exists.
        if (ignoreScope) {
            int index = affiliation.indexOf('@');
            if (index != -1) {
                affiliation = affiliation.substring(0, index);
            }
        }
        // If we ignore the value, then strip it out so only the scope remains.
        if (ignoreValue) {
            int index = affiliation.indexOf('@');
            if (index != -1) {
                affiliation = affiliation.substring(index + 1, affiliation.length());
            }
        }

        return affiliation;
    }

    private String get_group_names_from_affiliation(String affiliation) {
        String groupNames = configurationService.getProperty(
                "authentication-shibboleth.role." + affiliation);
        if (groupNames == null || groupNames.trim().length() == 0) {
            groupNames = configurationService.getProperty(
                "authentication-shibboleth.role." + affiliation.toLowerCase());
        }
        return groupNames;
    }

    private List<UUID> string2groups(String groupNames) {
        List<UUID> groups = new ArrayList<UUID>();
        // Add each group to the list.
        String[] names = groupNames.split(",");
        for (int i = 0; i < names.length; i++) {
            try {

                Group group = groupService.findByName(context_, names[i].trim());
                if (group != null) {
                    groups.add(group.getID());
                } else {
                    log.debug("Unable to find group: '" + names[i].trim() + "'");
                }
            } catch (SQLException sqle) {
                log.error(
                    "Exception thrown while trying to lookup affiliation role for group name: '"
                            + names[i].trim() + "'", sqle);
            }
        } // for each groupNames
        return groups;
    }

    private Group get_default_group() {
        String defaultAuthGroup = configurationService.getProperty(
                "authentication-shibboleth.default.auth.group");
        if (defaultAuthGroup != null && defaultAuthGroup.trim().length() != 0) {
            try {
                Group group = groupService.findByName(context_,defaultAuthGroup.trim());
                if (group != null) {
                    return group;
                } else {
                    log.debug("Unable to find default group: '" + defaultAuthGroup.trim() + "'");
                }
            } catch (SQLException sqle) {
                log.error("Exception thrown while trying to lookup shibboleth " +
                        "default authentication group with name: '" + defaultAuthGroup.trim() + "'",sqle);
            }
        }

        return null;
    }

    // helpers
    //

    private static List<String> string2values(String string) {
        if ( string == null ) {
            return null;
        }
        return Arrays.asList(string.split(",|;"));
    }
}