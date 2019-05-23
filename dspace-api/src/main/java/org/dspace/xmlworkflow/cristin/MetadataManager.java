package org.dspace.xmlworkflow.cristin;

import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.sql.SQLException;

/**
 * Class for providing utilities for Metadata management in the Duo module
 */
public class MetadataManager {

    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();


    /**
     * Remove all of the metadata from the item based on the configuration in the specified module
     *
     * @param context
     * @param item
     * @param module
     * @param config
     */
    public void removeAuthorityMetadata(Context context, Item item, String module, String config) throws CristinException, SQLException {
        String raw = configService.getProperty(module, config);
        if (raw == null || "".equals(raw)) {
            return;
        }
        String[] parts = raw.split(",");
        for (String part : parts) {
            DCValue dcv = this.makeMetadatumValue(part.trim(), null);
            itemService.clearMetadata(context, item, dcv.schema, dcv.element, dcv.qualifier, Item.ANY);
        }
    }

    /**
     * Make a MetadatumValue object out of the string representation (e.g. dc.title.alternative)
     *
     * @param field
     * @param value
     * @return
     */
    public DCValue makeMetadatumValue(String field, String value) throws CristinException {
        DCValue dcv = new DCValue();
        String[] bits = field.split("\\.");
        if (bits.length < 2 || bits.length > 3) {
            throw new CristinException("invalid DC value: " + field);
        }
        dcv.schema = bits[0];
        dcv.element = bits[1];
        if (bits.length == 3) {
            dcv.qualifier = bits[2];
        }
        dcv.value = value;
        return dcv;
    }

    private class DCValue {

        public String schema;
        public String element;
        public String qualifier;
        public String value;
    }
}
