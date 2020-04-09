/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueDTOList;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converter to translate between lists of domain {@link MetadataValue}s and {@link MetadataRest} representations.
 */
@Component
public class MetadataDTOConverter implements DSpaceConverter<MetadataValueDTOList, MetadataRest> {

    @Autowired
    private ContentServiceFactory contentServiceFactory;

    @Autowired
    private ConverterService converter;

    @Override
    public MetadataRest convert(MetadataValueDTOList metadataValues,
                                Projection projection) {
        // Convert each value to a DTO while retaining place order in a map of key -> SortedSet
        Map<String, Set<MetadataValueRest>> mapOfSortedSets = new HashMap<>();
        for (MetadataValueDTO metadataValue : metadataValues) {
            String key = metadataValue.getKey();
            Set<MetadataValueRest> set = mapOfSortedSets.get(key);
            if (set == null) {
                set = new LinkedHashSet<>();
                mapOfSortedSets.put(key, set);
            }
            set.add(converter.toRest(metadataValue, projection));
            mapOfSortedSets.put(key, set);
        }

        MetadataRest metadataRest = new MetadataRest();

        // Populate MetadataRest's map of key -> List while respecting SortedSet's order
        Map<String, List<MetadataValueRest>> mapOfLists = metadataRest.getMap();
        for (Map.Entry<String, Set<MetadataValueRest>> entry : mapOfSortedSets.entrySet()) {
            mapOfLists.put(entry.getKey(), entry.getValue().stream().collect(Collectors.toList()));
        }

        return metadataRest;
    }

    @Override
    public Class<MetadataValueDTOList> getModelClass() {
        return MetadataValueDTOList.class;
    }

    /**
     * Sets a DSpace object's domain metadata values from a rest representation.
     *
     * @param context the context to use.
     * @param dso the DSpace object.
     * @param metadataRest the rest representation of the new metadata.
     * @throws SQLException if a database error occurs.
     * @throws AuthorizeException if an authorization error occurs.
     */
    public void setMetadata(Context context, DSpaceObject dso, MetadataRest metadataRest)
        throws SQLException, AuthorizeException {
        DSpaceObjectService dsoService = contentServiceFactory.getDSpaceObjectService(dso);
        dsoService.clearMetadata(context, dso, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (Map.Entry<String, List<MetadataValueRest>> entry : metadataRest.getMap().entrySet()) {
            String[] seq = entry.getKey().split("\\.");
            String schema = seq[0];
            String element = seq[1];
            String qualifier = seq.length == 3 ? seq[2] : null;
            for (MetadataValueRest mvr : entry.getValue()) {
                dsoService.addMetadata(context, dso, schema, element, qualifier, mvr.getLanguage(),
                                       mvr.getValue(), mvr.getAuthority(), mvr.getConfidence());
            }
        }
        dsoService.update(context, dso);
    }
}
