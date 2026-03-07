/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.apache.logging.log4j.Logger;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Static utility class to manage configuration for exposure (hiding) of
 * certain Item metadata fields.
 *
 * This class answers the question, "is the user allowed to see this
 * metadata field?"  Any external interface (UI, OAI-PMH, etc) that
 * disseminates metadata should consult it before disseminating the value
 * of a metadata field.
 *
 * Since the MetadataExposure.isHidden() method gets called in a lot of inner
 * loops, it is important to implement it efficiently, in both time and
 * memory utilization.  It computes an answer without consuming ANY memory
 * (e.g. it does not build any temporary Strings) and in close to constant
 * time by use of hash tables.  Although most sites will only hide a few
 * fields, we can't predict what the usage will be so it's better to make it
 * scalable.
 *
 * Algorithm is as follows:
 * 1. If a Context is provided and it has a user who is Administrator,
 * always grant access (return false).
 * 2. Return true if field is on the hidden list, false otherwise.
 *
 * The internal maps are populated from DSpace Configuration at the first
 * call, in case the properties are not available in the static context.
 *
 * Configuration Properties:
 * ## hide a single metadata field
 * #metadata.hide.SCHEMA.ELEMENT[.QUALIFIER] = true
 * # example: dc.type
 * metadata.hide.dc.type = true
 * # example: dc.description.provenance
 * metadata.hide.dc.description.provenance = true
 *
 * @author Larry Stone
 * @version $Revision: 3734 $
 */
public class MetadataExposureServiceImpl implements MetadataExposureService {
    protected Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataExposureServiceImpl.class);

    protected Map<String, Set<String>> hiddenElementSets = null;
    protected Map<String, Map<String, Set<String>>> hiddenElementMaps = null;

    protected final String CONFIG_PREFIX = "metadata.hide.";

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected GroupService groupService;

    protected MetadataExposureServiceImpl() {

    }

    @Override
    public boolean isHidden(Context context, String schema, String element, String qualifier)
        throws SQLException {
        boolean hidden = false;

        // for schema.element, just check schema->elementSet
        if (!isInitialized()) {
            init();
        }

        if (qualifier == null) {
            Set<String> elts = hiddenElementSets.get(schema);
            hidden = elts != null && elts.contains(element);
        } else { // for schema.element.qualifier, just schema->eltMap->qualSet
            Map<String, Set<String>> elts = hiddenElementMaps.get(schema);
            if (elts == null) {
                return false;
            }
            Set<String> quals = elts.get(element);
            hidden = quals != null && quals.contains(qualifier);
        }

        if (hidden && context != null) {
            // administrator's override: admins can always see hidden fields
            hidden = !authorizeService.isAdmin(context);
        }

        if (hidden && context != null) {
            // Group-based override: resolve which groups list to use.
            // Per-field key (metadata.hide.schema.element[.qualifier].groups) takes
            // precedence over the global key (metadata.hide.groups).
            // If neither is set the field remains hidden for non-admins.
            String perFieldKey = CONFIG_PREFIX + schema + "." + element
                    + (qualifier != null ? "." + qualifier : "") + ".groups";
            String groupsConfig = configurationService.getProperty(perFieldKey);
            if (StringUtils.isBlank(groupsConfig)) {
                groupsConfig = configurationService.getProperty("metadata.hide.groups");
            }
            if (StringUtils.isNotBlank(groupsConfig)) {
                for (String groupName : groupsConfig.split(",")) {
                    String trimmedGroupName = groupName.trim();
                    if (StringUtils.isNotBlank(trimmedGroupName)
                            && groupService.isMember(context, trimmedGroupName)) {
                        hidden = false;
                        break;
                    }
                }
            }
        }

        return hidden;
    }

    /**
     * Returns whether the maps from configuration have already been loaded
     * into the hiddenElementSets property.
     *
     * @return true (initialized) or false (not initialized)
     */
    protected boolean isInitialized() {
        return hiddenElementSets != null;
    }

    /**
     * Loads maps from configuration unless it's already done.
     * The configuration properties are a map starting with the
     * "metadata.hide." prefix followed by schema, element and
     * qualifier separated by dots and the value is true (hidden)
     * or false (exposed).
     */
    protected synchronized void init() {
        if (!isInitialized()) {
            hiddenElementSets = new HashMap<>();
            hiddenElementMaps = new HashMap<>();

            List<String> propertyKeys = configurationService.getPropertyKeys();
            for (String key : propertyKeys) {
                if (key.startsWith(CONFIG_PREFIX)) {
                    // Skip group-access control keys (global and per-field) that share
                    // the prefix but are not field-hiding directives.
                    // e.g. metadata.hide.groups  or  metadata.hide.dc.title.groups
                    if (key.endsWith(".groups")) {
                        continue;
                    }
                    if (configurationService.getBooleanProperty(key, true)) {
                        String mdField = key.substring(CONFIG_PREFIX.length());
                        String[] segment = mdField.split("\\.", 3);

                        // got schema.element.qualifier
                        if (segment.length == 3) {
                            Map<String, Set<String>> eltMap = hiddenElementMaps.get(segment[0]);
                            if (eltMap == null) {
                                eltMap = new HashMap<String, Set<String>>();
                                hiddenElementMaps.put(segment[0], eltMap);
                            }
                            if (!eltMap.containsKey(segment[1])) {
                                eltMap.put(segment[1], new HashSet<String>());
                            }
                            eltMap.get(segment[1]).add(segment[2]);
                        } else if (segment.length == 2) { // got schema.element
                            if (!hiddenElementSets.containsKey(segment[0])) {
                                hiddenElementSets.put(segment[0], new HashSet<String>());
                            }
                            hiddenElementSets.get(segment[0]).add(segment[1]);
                        } else { // oops..
                            log.warn("Bad format in hidden metadata directive, field=\"" + mdField + "\", " +
                                    "config property=" + key);
                        }
                    }
                }
            }
        }
    }
}
