/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.factory.DSpaceObjectIndexFactory;

/**
 * Factory implementation for indexing/retrieving DSpaceObjects in the search core
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public abstract class DSpaceObjectIndexFactoryImpl<T extends IndexableDSpaceObject, S extends DSpaceObject>
        extends IndexFactoryImpl<T, S> implements DSpaceObjectIndexFactory<T, S> {

    @Override
    public SolrInputDocument buildDocument(Context context, T indexableObject) throws SQLException, IOException {
        // Add the ID's, types and call the SolrServiceIndexPlugins
        SolrInputDocument doc = super.buildDocument(context, indexableObject);
        final DSpaceObject dso = indexableObject.getIndexedObject();

        // want to be able to search for handle, so use keyword
        // (not tokenized, but it is indexed)
        if (dso.getHandle() != null) {
            // want to be able to search for handle, so use keyword
            // (not tokenized, but it is indexed)
            doc.addField("handle", dso.getHandle());
        }

        final List<String> locations = getLocations(context, indexableObject);
        storeCommunityCollectionLocations(doc, locations);
        return doc;
    }

    /**
     * Add the metadata value of the community/collection to the solr document
     * IF needed highlighting is added !
     *
     * @param doc                       the solr document
     * @param highlightedMetadataFields the list of metadata fields that CAN be highlighted
     * @param toIgnoreMetadataFields    the list of metadata fields to skip adding to Solr
     * @param metadataField             the metadata field added
     * @param value                     the value (can be NULL !)
     */
    protected void addContainerMetadataField(SolrInputDocument doc, List<String> highlightedMetadataFields,
                                   List<String> toIgnoreMetadataFields, String metadataField, String value) {
        if ((toIgnoreMetadataFields == null || !toIgnoreMetadataFields.contains(metadataField))
                && StringUtils.isNotBlank(value)) {
            doc.addField(metadataField, value);
            if (highlightedMetadataFields.contains(metadataField)) {
                doc.addField(metadataField + "_hl", value);
            }
        }
    }

    @Override
    public void storeCommunityCollectionLocations(SolrInputDocument doc, List<String> locations) {
        if (locations != null) {
            for (String location : locations) {
                doc.addField("location", location);
                if (location.startsWith("m")) {
                    doc.addField("location.comm", location.substring(1));
                } else {
                    doc.addField("location.coll", location.substring(1));
                }
            }
        }
    }
}
