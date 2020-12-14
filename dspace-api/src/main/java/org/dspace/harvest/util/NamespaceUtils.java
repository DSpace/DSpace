/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom.Namespace;

public final class NamespaceUtils {

    public static final String METADATA_FORMATS_KEY = "oai.harvester.metadataformats";

    private static final Namespace ATOM_NS = Namespace.getNamespace("http://www.w3.org/2005/Atom");

    private NamespaceUtils() {

    }

    /**
     * Search the configuration for metadata formats and return the namespace.
     *
     * @param  metadataKey the metadata key
     * @return             Namespace of the designated metadata format. Returns null
     *                     of not found.
     */
    public static Namespace getMetadataFormatNamespace(String metadataKey) {
        String key = METADATA_FORMATS_KEY + "." + metadataKey;
        String value = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(key);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return Namespace.getNamespace(value.indexOf(',') != -1 ? value.substring(0, value.indexOf(',')) : value);
    }

    /**
     * Search the configuration options and find the ORE serialization string
     *
     * @return Namespace of the supported ORE format.
     */
    public static Namespace getORENamespace() {
        String ORESerializationString = null;
        String ORESeialKey = null;
        String oreString = "oai.harvester.oreSerializationFormat";

        List<String> keys = DSpaceServicesFactory.getInstance().getConfigurationService().getPropertyKeys(oreString);

        for (String key : keys) {
            ORESeialKey = key.substring(oreString.length() + 1);
            ORESerializationString = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(key);

            return Namespace.getNamespace(ORESeialKey, ORESerializationString);
        }

        // Fallback if the configuration option is not present
        return Namespace.getNamespace("ore", ATOM_NS.getURI());
    }

}
