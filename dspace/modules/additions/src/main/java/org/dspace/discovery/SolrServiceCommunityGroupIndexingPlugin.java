/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableDSpaceObject;

/**
 * Indexes the id of the CommunityGroup for any Community
 *
 * @author Mohamed Mohideen Abdul Rasheed (mohideen at umd.edu)
 */
public class SolrServiceCommunityGroupIndexingPlugin implements SolrServiceIndexPlugin {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(SolrServiceCommunityGroupIndexingPlugin.class);

    @Override
    public void additionalIndex(Context context, IndexableObject idxObj, SolrInputDocument document) {
        if (idxObj instanceof IndexableDSpaceObject) {
            DSpaceObject dso = ((IndexableDSpaceObject) idxObj).getIndexedObject();
            if (dso instanceof Community) {
                int communityGroupId = ((Community) dso).getGroupID();
                if (communityGroupId >= 0) {
                    log.debug("Adding community group info to solr index for community: " + dso.getHandle());
                    document.addField("community_group", String.valueOf(communityGroupId));
                }
            }
        }
    }
}
