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

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightingConfiguration;
import org.dspace.discovery.indexobject.factory.CommunityIndexFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation for indexing/retrieving communities in the search core
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class CommunityIndexFactoryImpl extends DSpaceObjectIndexFactoryImpl<IndexableCommunity, Community>
        implements CommunityIndexFactory {

    @Autowired(required = true)
    protected CommunityService communityService;

    @Override
    public Iterator<IndexableCommunity> findAll(Context context) throws SQLException {
        Iterator<Community> communities = communityService.findAll(context).iterator();

        return new Iterator<IndexableCommunity>() {
            @Override
            public boolean hasNext() {
                return communities.hasNext();
            }

            @Override
            public IndexableCommunity next() {
                return new IndexableCommunity(communities.next());
            }
        };
    }

    @Override
    public String getType() {
        return IndexableCommunity.TYPE;
    }


    @Override
    public SolrInputDocument buildDocument(Context context, IndexableCommunity indexableObject)
            throws SQLException, IOException {
        // Add the ID's, types and call the SolrServiceIndexPlugins
        SolrInputDocument doc = super.buildDocument(context, indexableObject);
        final Community community = indexableObject.getIndexedObject();

        // Retrieve configuration
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(community);
        DiscoveryHitHighlightingConfiguration highlightingConfiguration = discoveryConfiguration
            .getHitHighlightingConfiguration();
        List<String> highlightedMetadataFields = new ArrayList<String>();
        if (highlightingConfiguration != null) {
            for (DiscoveryHitHighlightFieldConfiguration configuration : highlightingConfiguration
                .getMetadataFields()) {
                highlightedMetadataFields.add(configuration.getField());
            }
        }

        // Add community metadata
        String description = communityService.getMetadata(community, "introductory_text");
        String description_abstract = communityService.getMetadata(community, "short_description");
        String description_table = communityService.getMetadata(community, "side_bar_text");
        String rights = communityService.getMetadata(community, "copyright_text");
        String title = communityService.getMetadata(community, "name");

        List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(community.getType());
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.description",
                                  description);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.description.abstract",
                                  description_abstract);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields,
                                  "dc.description.tableofcontents", description_table);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.rights", rights);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.title", title);
        return doc;
    }

    @Override
    public boolean supports(Object object) {
        return object instanceof Community;
    }

    @Override
    public List getIndexableObjects(Context context, Community object) {
        return Arrays.asList(new IndexableCommunity(object));
    }

    @Override
    public Optional<IndexableCommunity> findIndexableObject(Context context, String id) throws SQLException {
        final Community community = communityService.find(context, UUID.fromString(id));
        return community == null ? Optional.empty() : Optional.of(new IndexableCommunity(community));
    }

    @Override
    public List<String> getLocations(Context context, IndexableCommunity indexableDSpaceObject) throws SQLException {
        final Community target = indexableDSpaceObject.getIndexedObject();
        List<String> locations = new ArrayList<>();
        // build list of community ids
        List<Community> communities = target.getParentCommunities();

        // now put those into strings
        for (Community community : communities) {
            locations.add("m" + community.getID());
        }

        return locations;
    }
}