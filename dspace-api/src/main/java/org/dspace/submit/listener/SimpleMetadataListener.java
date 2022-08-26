/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.listener;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;

/**
 * This is the basic implementation for the MetadataListener interface.
 * 
 * It got the a map of metadata and related External Data Provider that can be
 * used to retrieve further information using the updated metadata in the item
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class SimpleMetadataListener implements MetadataListener {
    /**
     * A map to link a specific metadata with an ExternalDataProvider
     */
    private Map<String, List<ExternalDataProvider>> externalDataProvidersMap;

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    public Map<String, List<ExternalDataProvider>> getExternalDataProvidersMap() {
        return externalDataProvidersMap;
    }

    public void setExternalDataProvidersMap(Map<String, List<ExternalDataProvider>> externalDataProvidersMap) {
        this.externalDataProvidersMap = externalDataProvidersMap;
    }

    @Override
    public Set<String> getMetadataToListen() {
        return externalDataProvidersMap.keySet();
    }

    @Override
    public ExternalDataObject getExternalDataObject(Context context, Item item, Set<String> changedMetadata) {
        // we loop over the available provider and return the first found object
        for (String m : changedMetadata) {
            List<ExternalDataProvider> providers = externalDataProvidersMap.get(m);
            for (ExternalDataProvider prov : providers) {
                String id = generateExternalId(context, prov, item, changedMetadata, m);
                if (StringUtils.isNotBlank(id)) {
                    Optional<ExternalDataObject> result = prov.getExternalDataObject(id);
                    if (result.isPresent()) {
                        return result.get();
                    }
                }
            }
        }
        return null;
    }


    /**
     * This is the simpler implementation, it assumes that the value of the metadata
     * listened by the DataProvider can be used directly as identifier. Subclass may
     * extend it to add support for identifier normalization or combine multiple
     * information to build the identifier
     * 
     * @param context         the DSpace Context Object
     * @param prov            the ExternalDataProvider that need to received an Id
     * @param item            the item
     * @param changedMetadata the metadata that are recently changed
     * @param m               the changed metadata that lead to the selected
     *                        ExternalDataProvider
     * @return an Id if any that can be used to query the {@link ExternalDataProvider}
     */
    protected String generateExternalId(Context context, ExternalDataProvider prov, Item item,
            Set<String> changedMetadata, String m) {
        List<MetadataValue> metadataByMetadataString = itemService.getMetadataByMetadataString(item, m);
        // only suggest an identifier if there is exactly one value for the metadata. If
        // there are more values it is highly probable that a lookup was already
        // performed when the first value was added
        if (metadataByMetadataString != null && metadataByMetadataString.size() == 1) {
            return metadataByMetadataString.get(0).getValue();
        }
        return null;
    }

}
