/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.util;

import java.util.List;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom.Namespace;

public final class NamespaceUtils {

    private static final Namespace ATOM_NS = Namespace.getNamespace("http://www.w3.org/2005/Atom");

    private NamespaceUtils() {

    }

    /**
     * Cycle through the options and find the metadata namespace matching the
     * provided key.
     *
     * @param  metadataKey the metadata key
     * @return             Namespace of the designated metadata format. Returns null
     *                     of not found.
     */
    public static Namespace getDMDNamespace(String metadataKey) {
        String metadataString = null;
        String metaString = "oai.harvester.metadataformats";

        List<String> keys = DSpaceServicesFactory.getInstance().getConfigurationService().getPropertyKeys(metaString);

        for (String key : keys) {
            if (key.substring(metaString.length() + 1).equals((metadataKey))) {
                metadataString = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(key);
                String namespacePiece;
                if (metadataString.indexOf(',') != -1) {
                    namespacePiece = metadataString.substring(0, metadataString.indexOf(','));
                } else {
                    namespacePiece = metadataString;
                }

                return Namespace.getNamespace(namespacePiece);
            }
        }
        return null;
    }

    /**
     * Search the configuration options and find the ORE serialization string
     *
     * @return Namespace of the supported ORE format. Returns null if not found.
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
