/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.HashMap;
import java.util.Map;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class to represent the configuration of the cross-linking between browse
 * pages (for example, between the author name in one full listing to the
 * author's list of publications).
 *
 * @author Richard Jones
 */
public class CrossLinks {
    /**
     * a map of the desired links
     */
    private Map<String, String> links = new HashMap<>();

    /**
     * Construct a new object which will obtain the configuration for itself.
     *
     * @throws BrowseException if browse error
     */
    public CrossLinks()
        throws BrowseException {
        int i = 1;
        while (true) {
            String field = "webui.browse.link." + i;
            ConfigurationService configurationService
                    = DSpaceServicesFactory.getInstance().getConfigurationService();
            String config = configurationService.getProperty(field);
            if (config == null) {
                break;
            }

            String[] parts = config.split(":");
            if (parts.length != 2) {
                throw new BrowseException("Invalid configuration for " + field + ": " + config);
            }
            links.put(parts[1], parts[0]);
            i++;
        }
    }

    /**
     * Is there a link for the given canonical form of metadata (i.e. schema.element.qualifier)?
     *
     * @param metadata the metadata to check for a link on
     * @return true/false
     */
    public boolean hasLink(String metadata) {
        return findLinkType(metadata) != null;
    }

    /**
     * Is there a link for the given browse name (eg 'author')
     * @param browseIndexName
     * @return true/false
     */
    public boolean hasBrowseName(String browseIndexName) {
        return links.containsValue(browseIndexName);
    }

    /**
     * Get the type of link that the bit of metadata has.
     *
     * @param metadata the metadata to get the link type for
     * @return type
     */
    public String getLinkType(String metadata) {
        return findLinkType(metadata);
    }

    /**
     * Get full map of field->indexname link configurations
     * @return
     */
    public Map<String, String> getLinks() {
        return links;
    }

    /**
     * Find and return the browse name for a given metadata field.
     * If the link key contains a wildcard eg dc.subject.*, it should
     * match dc.subject.other, etc.
     * @param metadata
     * @return
     */
    public String findLinkType(String metadata) {
        // Resolve wildcards properly, eg. dc.subject.other matches a configuration for dc.subject.*
        for (String key : links.keySet()) {
            if (null != key && key.endsWith(".*")) {
                // A substring of length-1, also substracting the wildcard should work as a "startsWith"
                // check for the field eg. dc.subject.* -> dc.subject is the start of dc.subject.other
                if (null != metadata && metadata.startsWith(key.substring(0, key.length() - 1 - ".*".length()))) {
                    return links.get(key);
                }
            } else {
                // Exact match, if the key field has no .* wildcard
                if (links.containsKey(metadata)) {
                    return links.get(metadata);
                }
            }
        }
        // No match
        return null;
    }
}
