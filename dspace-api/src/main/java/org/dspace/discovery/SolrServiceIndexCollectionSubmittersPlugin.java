/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
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
                    String fieldValue = null;
                    Community parent = (Community) ContentServiceFactory.getInstance().getDSpaceObjectService(col)
                                                                        .getParentObject(context, col);
                    while (parent != null) {
                        if (parent.getAdministrators() != null) {
                            fieldValue = "g" + parent.getAdministrators().getID();
                            document.addField("submit", fieldValue);
                        }
                        parent = (Community) ContentServiceFactory.getInstance().getDSpaceObjectService(parent)
                                                                                .getParentObject(context, parent);
                    }
                    List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context,col,Constants.ADD);
                    policies.addAll(authorizeService.getPoliciesActionFilter(context, col, Constants.ADMIN));

                    for (ResourcePolicy resourcePolicy : policies) {
                        if (resourcePolicy.getGroup() != null) {
                            fieldValue = "g" + resourcePolicy.getGroup().getID();
                        } else {
                            fieldValue = "e" + resourcePolicy.getEPerson().getID();

                        }
                        document.addField("submit", fieldValue);
                        context.uncacheEntity(resourcePolicy);
                    }
                } catch (SQLException e) {
                    log.error(LogHelper.getHeader(context, "Error while indexing resource policies",
                             "Collection: (id " + col.getID() + " type " + col.getName() + ")" ));
                }
            }
        }
    }

}