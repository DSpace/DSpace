/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.virtual.AuthorityVirtualMetadataConfiguration;
import org.dspace.content.virtual.AuthorityVirtualMetadataPopulator;
import org.dspace.content.virtual.VirtualMetadataPopulator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple service to handle authority virtual metadata operations.
 * Separated from item service for better separation of concerns and because the field lookups
 * required by this service require a context which is not provided in
 * {@link org.dspace.content.service.ItemService#getMetadata}
 *
 * @author Kim Shepherd
 */
public class AuthorityVirtualMetadataServiceImpl implements AuthorityVirtualMetadataService {

    @Autowired
    protected VirtualMetadataPopulator virtualMetadataPopulator;
    @Autowired
    protected AuthorityVirtualMetadataPopulator authorityVirtualMetadataPopulator;
    @Autowired
    protected AuthorityValueService authorityValueService;
    @Autowired
    protected MetadataFieldService metadataFieldService;
    @Autowired
    protected MetadataAuthorityService metadataAuthorityService;

    /**
     * Map of virtual field names that are configured for authority control and exist in metadata registry
     */
    private HashMap<String, MetadataField> validVirtualFieldNames;
    /**
     * Map of authority virtual metadata configurations, see virtual-metadata.xml
     */
    private Map<String, HashMap<String, AuthorityVirtualMetadataConfiguration>> authorityVirtualMaps;

    private static final Logger log = LogManager.getLogger();

    /**
     * Initialize hashmaps for field / configuration lookups. This is only executed the first time a lookup
     * is attempted, to keep database load down for data retrieval that typically will not change between
     * service restarts.
     */
    public void initMaps() throws SQLException {
        if (validVirtualFieldNames == null && authorityVirtualMaps == null) {
            validVirtualFieldNames = new HashMap<>();
            authorityVirtualMaps = new HashMap<>();
            Context context = new Context(Context.Mode.READ_ONLY);
            // Get field maps configured in virtual metadata spring configuration
            authorityVirtualMaps = authorityVirtualMetadataPopulator.getMap();
            // Iterate map of maps, just to check each virtual field name (key of 2nd-level map) exists
            // and populate the virtual field names list so we can look it up later without further database calls
            for (String configuredAuthorityField : authorityVirtualMaps.keySet()) {
                for (String virtualFieldName : authorityVirtualMaps.get(configuredAuthorityField).keySet()) {
                    MetadataField virtualField = metadataFieldService.findByString(context, virtualFieldName, '.');
                    if (virtualField != null) {
                        validVirtualFieldNames.put(virtualFieldName, virtualField);
                    }
                }
            }
        }
    }

    /**
     * This method retrieves a list of authority virtual metadata values for a given item.
     * For each metadata value that is authority controlled and configured in the spring virtual metadata config,
     * the authority document values will be populated (via collection, concatenation, etc) and returned to
     * add to an existing metadata list for the item.
     *
     * @param item The item for which to retrieve the authority virtual metadata values.
     * @param dbMetadataValues The list of regular metadata values associated with the item.
     * @return The list of authority virtual metadata values for the item.
     */
    public List<MetadataValue> getAuthorityVirtualMetadata(Item item, List<MetadataValue> dbMetadataValues) {
        List<MetadataValue> authorityMetadataValues = new LinkedList<>();
        // If maps are not initialized, do it now
        if (authorityVirtualMaps == null || validVirtualFieldNames == null) {
            try {
                initMaps();
            } catch (SQLException e) {
                log.error("Error initializing authority virtual metadata configuration", e);
                return authorityMetadataValues;
            }
        }

        // Make a map of counts - we want to calc a real 'place'
        Map<String, Integer> fieldCounts = new HashMap<>();
        for (MetadataValue mv : dbMetadataValues) {
            String mvfn = mv.getMetadataField().toString('.');
            if (!fieldCounts.containsKey(mvfn)) {
                fieldCounts.put(mvfn, 1);
            } else {
                fieldCounts.put(mvfn, fieldCounts.get(mvfn) + 1);
            }
        }
        for (MetadataValue dbValue : dbMetadataValues) {
            if (null != dbValue.getAuthority()) {
                // Get field
                MetadataField dbField = dbValue.getMetadataField();
                // In the virtual authority metadata spring XML, the map keys are field names of
                // authority-controlled fields
                String dbFieldName = dbField.toString('.');
                // Check if this field is mapped *and* is configured for authority control
                if (authorityVirtualMaps.containsKey(dbFieldName)
                        && metadataAuthorityService.isAuthorityControlled(
                        dbFieldName.replace('.', '_')) ) {
                    AuthorityValue authorityValue = authorityValueService.findByUID(dbValue.getAuthority());
                    if (authorityValue == null) {
                        continue;
                    }
                    // Get map from config
                    HashMap<String, AuthorityVirtualMetadataConfiguration> authorityVirtualFieldMap =
                            authorityVirtualMaps.get(dbField.toString('.'));
                    // Iterate mapped fields
                    for (String virtualFieldName : authorityVirtualFieldMap.keySet()) {
                        // The field must exist
                        if (!validVirtualFieldNames.containsKey(virtualFieldName)) {
                            log.error("Could not find configured virtual metadata field: " + virtualFieldName);
                            continue;
                        }
                        // Don't process fields which match the db field
                        if (virtualFieldName.equals(dbFieldName)) {
                            log.debug("Skipping virtual metadata field which matches current db field: "
                                    + virtualFieldName);
                            continue;
                        }
                        MetadataField virtualField = validVirtualFieldNames.get(virtualFieldName);
                        AuthorityVirtualMetadataConfiguration authorityVirtualMetadataConfiguration =
                                authorityVirtualFieldMap.get(virtualFieldName);
                        List<String> virtualAuthorityValues =
                                authorityVirtualMetadataConfiguration.getValues(authorityValue);
                        // Iterate virtual authority values and insert them as authority virtual metadata
                        for (String virtualAuthorityValue : virtualAuthorityValues) {
                            AuthorityVirtualMetadataValue metadataValue = new AuthorityVirtualMetadataValue();
                            metadataValue.setMetadataField(virtualField);
                            // There is no information in the source authority object to help determine the
                            // place so instead we use the running count to set an appropriate place at the
                            // end of the existing metadata values for this field
                            int place = (fieldCounts.getOrDefault(virtualFieldName, 0));
                            metadataValue.setPlace(place);
                            fieldCounts.put(virtualFieldName, ++place);
                            // In RelationVirtualMetadata, the authority uses the prefix and the relationship
                            // ID, but we will instead use the authority ID as the second component
                            metadataValue.setAuthority(Constants.VIRTUAL_AUTHORITY_PREFIX
                                    + authorityValue.getId());
                            metadataValue.setValue(virtualAuthorityValue);
                            metadataValue.setDSpaceObject(item);
                            authorityMetadataValues.add(metadataValue);
                        }
                    }
                }
            }
        }
        return authorityMetadataValues;
    }

}
