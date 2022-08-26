/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueList;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Converter to translate between lists of domain {@link MetadataValue}s and {@link MetadataRest} representations.
 */
@Component
public class MetadataConverter implements DSpaceConverter<MetadataValueList, MetadataRest> {

    @Autowired
    private ContentServiceFactory contentServiceFactory;

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    @Override
    public MetadataRest convert(MetadataValueList metadataValues,
                                Projection projection) {
        // Convert each value to a DTO while retaining place order in a map of key -> SortedSet
        Map<String, SortedSet<MetadataValueRest>> mapOfSortedSets = new HashMap<>();
        for (MetadataValue metadataValue : metadataValues) {
            String key = metadataValue.getMetadataField().toString('.');
            SortedSet<MetadataValueRest> set = mapOfSortedSets.get(key);
            if (set == null) {
                set = new TreeSet<>(Comparator.comparingInt(MetadataValueRest::getPlace));
                mapOfSortedSets.put(key, set);
            }
            set.add(converter.toRest(metadataValue, projection));
        }

        MetadataRest metadataRest = new MetadataRest();

        // Populate MetadataRest's map of key -> List while respecting SortedSet's order
        Map<String, List<MetadataValueRest>> mapOfLists = metadataRest.getMap();
        for (Map.Entry<String, SortedSet<MetadataValueRest>> entry : mapOfSortedSets.entrySet()) {
            mapOfLists.put(entry.getKey(), entry.getValue().stream().collect(Collectors.toList()));
        }

        return metadataRest;
    }

    @Override
    public Class<MetadataValueList> getModelClass() {
        return MetadataValueList.class;
    }

    /**
     * Sets a DSpace object's domain metadata values from a rest representation.
     * Any existing metadata value is deleted or overwritten.
     *
     * @param context the context to use.
     * @param dso the DSpace object.
     * @param metadataRest the rest representation of the new metadata.
     * @throws SQLException if a database error occurs.
     * @throws AuthorizeException if an authorization error occurs.
     */
    public <T extends DSpaceObject> void setMetadata(Context context, T dso, MetadataRest metadataRest)
            throws SQLException, AuthorizeException {
        DSpaceObjectService<T> dsoService = contentServiceFactory.getDSpaceObjectService(dso);
        dsoService.clearMetadata(context, dso, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        persistMetadataRest(context, dso, metadataRest, dsoService);
    }

    /**
     * Add to a DSpace object's domain metadata values from a rest representation.
     * Any existing metadata value is preserved.
     *
     * @param context the context to use.
     * @param dso the DSpace object.
     * @param metadataRest the rest representation of the new metadata.
     * @throws SQLException if a database error occurs.
     * @throws AuthorizeException if an authorization error occurs.
     */
    public <T extends DSpaceObject> void addMetadata(Context context, T dso, MetadataRest metadataRest)
            throws SQLException, AuthorizeException {
        DSpaceObjectService<T> dsoService = contentServiceFactory.getDSpaceObjectService(dso);
        persistMetadataRest(context, dso, metadataRest, dsoService);
    }

    /**
     * Merge into a DSpace object's domain metadata values from a rest representation.
     * Any existing metadata value is preserved or overwritten with the new ones
     *
     * @param context the context to use.
     * @param dso the DSpace object.
     * @param metadataRest the rest representation of the new metadata.
     * @throws SQLException if a database error occurs.
     * @throws AuthorizeException if an authorization error occurs.
     */
    public <T extends DSpaceObject> void mergeMetadata(Context context, T dso, MetadataRest metadataRest)
            throws SQLException, AuthorizeException {
        DSpaceObjectService<T> dsoService = contentServiceFactory.getDSpaceObjectService(dso);
        for (Map.Entry<String, List<MetadataValueRest>> entry: metadataRest.getMap().entrySet()) {
            List<MetadataValue> metadataByMetadataString = dsoService.getMetadataByMetadataString(dso, entry.getKey());
            dsoService.removeMetadataValues(context, dso, metadataByMetadataString);
        }
        persistMetadataRest(context, dso, metadataRest, dsoService);
    }

    private <T extends DSpaceObject> void persistMetadataRest(Context context, T dso, MetadataRest metadataRest,
            DSpaceObjectService<T> dsoService)
                    throws SQLException, AuthorizeException {
        for (Map.Entry<String, List<MetadataValueRest>> entry: metadataRest.getMap().entrySet()) {
            String[] seq = entry.getKey().split("\\.");
            String schema = seq[0];
            String element = seq[1];
            String qualifier = seq.length == 3 ? seq[2] : null;
            for (MetadataValueRest mvr: entry.getValue()) {
                dsoService.addMetadata(context, dso, schema, element, qualifier, mvr.getLanguage(),
                        mvr.getValue(), mvr.getAuthority(), mvr.getConfidence());
            }
        }
        dsoService.update(context, dso);
    }

}
