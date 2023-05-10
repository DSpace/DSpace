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
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableInProgressSubmission;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;
import org.dspace.supervision.SupervisionOrder;
import org.dspace.supervision.service.SupervisionOrderService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A Solr Indexing plugin responsible adding a `supervised` field.
 * When item being indexed is a workspace or workflow item,
 * and at least one supervision order is defined
 * the 'supervised' field with value 'true' will be added to the solr document,
 * if no supervision orders are defined field will be set to 'false'
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public class SolrServiceSupervisionOrderIndexingPlugin implements SolrServiceIndexPlugin {

    @Autowired(required = true)
    private SupervisionOrderService supervisionOrderService;

    @Override
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {
        try {

            if (!(indexableObject instanceof IndexableWorkspaceItem) &&
                !(indexableObject instanceof IndexableWorkflowItem)) {
                return;
            }

            Item item =
                (((IndexableInProgressSubmission) indexableObject).getIndexedObject()).getItem();

            if (Objects.isNull(item)) {
                return;
            }
            addSupervisedField(context, item, document);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void addSupervisedField(Context context, Item item, SolrInputDocument document) throws SQLException {
        List<SupervisionOrder> supervisionOrders = supervisionOrderService.findByItem(context, item);
        if (CollectionUtils.isNotEmpty(supervisionOrders)) {
            document.addField("supervised", true);
        } else {
            document.addField("supervised", false);
        }
    }
}
