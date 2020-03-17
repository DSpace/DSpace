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
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableDSpaceObject;
/**
 * Indexes the UUID of the parent object for any Community, Collection and Item
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SolrServiceParentObjectIndexingPlugin implements SolrServiceIndexPlugin {

    @Override
    public void additionalIndex(Context context, IndexableObject idxObj, SolrInputDocument document) {
        try {
            if (idxObj instanceof IndexableDSpaceObject) {
                DSpaceObject dso = ((IndexableDSpaceObject) idxObj).getIndexedObject();
                if (dso instanceof Community || dso instanceof Collection || dso instanceof Item) {
                    DSpaceObject parent = ContentServiceFactory.getInstance().getDSpaceObjectService(dso)
                                                               .getParentObject(context, dso);
                    if (parent != null) {
                        document.addField("location.parent", parent.getID().toString());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
