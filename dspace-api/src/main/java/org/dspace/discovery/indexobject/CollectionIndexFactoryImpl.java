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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightingConfiguration;
import org.dspace.discovery.indexobject.factory.CollectionIndexFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Factory implementation for indexing/retrieving collections in the search core
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class CollectionIndexFactoryImpl extends DSpaceObjectIndexFactoryImpl<IndexableCollection, Collection>
        implements CollectionIndexFactory {

    @Autowired
    protected CollectionService collectionService;

    @Autowired
    protected CommunityService communityService;

    @Override
    public Iterator<IndexableCollection> findAll(Context context) throws SQLException {
        Iterator<Collection> collections = collectionService.findAll(context).iterator();

        return new Iterator<IndexableCollection>() {
            @Override
            public boolean hasNext() {
                return collections.hasNext();
            }

            @Override
            public IndexableCollection next() {
                return new IndexableCollection(collections.next());
            }
        };
    }

    @Override
    public String getType() {
        return IndexableCollection.TYPE;
    }

    /**
     * Build a solr document for a DSpace Collection.
     *
     * @param context    The relevant DSpace Context.
     * @param indexableCollection indexableCollection to be indexed
     * @throws SQLException sql exception
     * @throws IOException  IO exception
     */
    @Override
    public SolrInputDocument buildDocument(Context context, IndexableCollection indexableCollection)
            throws IOException, SQLException {
        // Create Lucene Document and add the ID's, types and call the SolrServiceIndexPlugins
        SolrInputDocument doc = super.buildDocument(context, indexableCollection);

        final Collection collection = indexableCollection.getIndexedObject();

        // Retrieve configuration
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(context, collection);
        DiscoveryHitHighlightingConfiguration highlightingConfiguration = discoveryConfiguration
            .getHitHighlightingConfiguration();
        List<String> highlightedMetadataFields = new ArrayList<>();
        if (highlightingConfiguration != null) {
            for (DiscoveryHitHighlightFieldConfiguration configuration : highlightingConfiguration
                .getMetadataFields()) {
                highlightedMetadataFields.add(configuration.getField());
            }
        }


        // Add collection metadata
        String description = collectionService.getMetadataFirstValue(collection,
                CollectionService.MD_INTRODUCTORY_TEXT, Item.ANY);
        String description_abstract = collectionService.getMetadataFirstValue(collection,
                CollectionService.MD_SHORT_DESCRIPTION, Item.ANY);
        String description_table = collectionService.getMetadataFirstValue(collection,
                CollectionService.MD_SIDEBAR_TEXT, Item.ANY);
        String provenance = collectionService.getMetadataFirstValue(collection,
                CollectionService.MD_PROVENANCE_DESCRIPTION, Item.ANY);
        String rights = collectionService.getMetadataFirstValue(collection,
                CollectionService.MD_COPYRIGHT_TEXT, Item.ANY);
        String rights_license = collectionService.getMetadataFirstValue(collection,
                CollectionService.MD_LICENSE, Item.ANY);
        String title = collectionService.getMetadataFirstValue(collection,
                CollectionService.MD_NAME, Item.ANY);
        String entityType = collectionService.getMetadataFirstValue(collection, "dspace", "entity", "type", Item.ANY);

        List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(collection.getType());
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.description",
                                  description);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.description.abstract",
                                  description_abstract);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields,
                                  "dc.description.tableofcontents", description_table);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.provenance", provenance);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.rights", rights);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.rights.license",
                                  rights_license);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.title", title);
        doc.addField("dc.title_sort", title);

        if (StringUtils.isBlank(entityType)) {
            entityType = Constants.ENTITY_TYPE_NONE;
        }
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields,
                                  "dspace.entity.type", entityType);

        return doc;
    }

    @Override
    public boolean supports(Object object) {
        return object instanceof Collection;
    }

    @Override
    public List getIndexableObjects(Context context, Collection object) {
        return Arrays.asList(new IndexableCollection(object));
    }

    @Override
    public Optional<IndexableCollection> findIndexableObject(Context context, String id) throws SQLException {
        final Collection collection = (collectionService.find(context, UUID.fromString(id)));
        return collection == null ? Optional.empty() : Optional.of(new IndexableCollection(collection));
    }

    @Override
    public List<String> getLocations(Context context, IndexableCollection indexableCollection) throws SQLException {
        return getCollectionLocations(context, indexableCollection.getIndexedObject());
    }

    @Override
    public List<String> getCollectionLocations(Context context, Collection collection) throws SQLException {
        List<String> locations = new ArrayList<>();
        // build list of community ids
        List<Community> communities = ContentServiceFactory.getInstance().getCommunityService().
                getAllParents(context, collection);

        // now put those into strings
        for (Community community : communities) {
            locations.add("m" + community.getID());
        }

        return locations;
    }
}
