/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Collection;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.eperson.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Add an index for all the administrators of the given collection.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class SolrServiceIndexCollectionAdministratorsPlugin implements SolrServiceIndexPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrServiceIndexCollectionAdministratorsPlugin.class);

    @Autowired
    private CommunityService communityService;

    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {
        if (!(indexableObject instanceof IndexableCollection)) {
            return;
        }

        Collection collection = ((IndexableCollection) indexableObject).getIndexedObject();
        if (collection == null) {
            return;
        }

        try {
            addIndexForAdministrators(context, collection, document);
        } catch (SQLException ex) {
            LOGGER.error("An error occurs indexing collection's admins:", ex);
        }

    }

    private void addIndexForAdministrators(Context context, Collection collection, SolrInputDocument document)
        throws SQLException {

        addAdminField(document, collection.getAdministrators());

        communityService.getAllParents(context, collection)
            .forEach(community -> addAdminField(document, community.getAdministrators()));
    }

    private void addAdminField(SolrInputDocument document, Group administrators) {
        if (administrators != null) {
            document.addField("admin", "g" + administrators.getID());
        }
    }

}
