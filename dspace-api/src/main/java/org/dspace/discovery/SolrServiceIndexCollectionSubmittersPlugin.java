/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The purpose of this plugin is to index all ADD type resource policies related to collections.
 * 
 * @author Mykhaylo Boychuk (at 4science.it)
 */
public class SolrServiceIndexCollectionSubmittersPlugin implements SolrServiceIndexPlugin {

    private static final Logger log = org.apache.logging.log4j.LogManager
                                                .getLogger(SolrServiceIndexCollectionSubmittersPlugin.class);

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Override
    public void additionalIndex(Context context, IndexableObject idxObj, SolrInputDocument document) {
        if (idxObj instanceof IndexableCollection) {
            Collection col = ((IndexableCollection) idxObj).getIndexedObject();
            if (col != null) {
                try {
                    // Index groups with ADMIN rights on the Collection, on
                    // Communities containing those Collections, and recursively on any Community containing such a
                    // Community.
                    // TODO: Strictly speaking we should also check for epersons who received admin rights directly,
                    //       without being part of the admin group. Finding them may be a lot slower though.
                    IndexingUtils.findTransitiveAdminGroupIds(context, col)
                        .forEach(unprefixedId -> document.addField("submit", "g" + unprefixedId));

                    // Index groups and epersons with ADD rights on the Collection.
                    IndexingUtils.findDirectAuthorizedGroupsAndEPersonsPrefixedIds(
                        authorizeService, context, col, new int[] {Constants.ADD}
                    ).forEach(prefixedId -> document.addField("submit", prefixedId));
                } catch (SQLException e) {
                    log.error(LogHelper.getHeader(context, "Error while indexing resource policies",
                             "Collection: (id " + col.getID() + " type " + col.getName() + ")" ));
                }
            }
        }
    }

}
