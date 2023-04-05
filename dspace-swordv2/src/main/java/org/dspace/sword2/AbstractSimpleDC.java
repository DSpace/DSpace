/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.sword2;

import java.util.HashMap;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class AbstractSimpleDC {
    private static final String DC_PREFIX = "swordv2-server.simpledc";

    private static final String ATOM_PREFIX = "swordv2-server.atom";

    protected HashMap<String, String> dcMap = null;

    protected HashMap<String, String> atomMap = null;

    protected ItemService itemService = ContentServiceFactory.getInstance()
                                                             .getItemService();

    protected ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    protected void loadMetadataMaps() {
        if (this.dcMap == null) {
            // we should load our DC map from configuration
            this.dcMap = new HashMap<>();
            List<String> keys = configurationService.getPropertyKeys(DC_PREFIX);
            for (String key : keys) {
                String k = key.substring(DC_PREFIX.length() + 1);
                String v = configurationService.getProperty(key);
                this.dcMap.put(k, v);
            }
        }

        if (this.atomMap == null) {
            this.atomMap = new HashMap<>();
            List<String> keys = configurationService.getPropertyKeys(ATOM_PREFIX);
            for (String key : keys) {
                String k = key.substring(ATOM_PREFIX.length() + 1);
                String v = configurationService.getProperty(key);
                this.atomMap.put(k, v);
            }
        }
    }

    protected SimpleDCMetadata getMetadata(Item item) {
        this.loadMetadataMaps();

        SimpleDCMetadata md = new SimpleDCMetadata();
        List<MetadataValue> all = itemService
            .getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        for (MetadataValue dcv : all) {
            MetadataField field = dcv.getMetadataField();
            String valueMatch = field.getMetadataSchema().getName() + "." +
                field.getElement();
            if (field.getQualifier() != null) {
                valueMatch += "." + field.getQualifier();
            }

            // look for the metadata in the dublin core map
            for (String key : this.dcMap.keySet()) {
                String value = this.dcMap.get(key);
                if (valueMatch.equals(value)) {
                    md.addDublinCore(key, dcv.getValue());
                }
            }

            // look for the metadata in the atom map
            for (String key : this.atomMap.keySet()) {
                String value = this.atomMap.get(key);
                if (valueMatch.equals(value)) {
                    md.addAtom(key, dcv.getValue());
                }
            }
        }

        return md;
    }
}
