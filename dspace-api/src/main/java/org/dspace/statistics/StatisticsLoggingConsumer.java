/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.sql.SQLException;
import java.util.*;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;

/**
 * StatisticsLogging Consumer for SolrLogger which captures Create, Update
 * and Delete Events on DSpace Objects.
 *
 * All usage-events will be updated to capture changes to e.g.
 * the owning collection
 *
 * @author kevinvandevelde at atmire.com
 * @author ben at atmrie.com
 */
public class StatisticsLoggingConsumer implements Consumer
{

    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
    private Set<String> toRemoveQueries = null;

    @Override
    public void initialize() throws Exception
    {

    }

    // TODO: checkout whether moving of collections, communities and bitstreams works
    // TODO: use async threaded consumer as this might require some processing time
    // TODO: we might be able to improve the performance: changing the collection will trigger 4 update commands
    @Override
    public void consume(Context ctx, Event event) throws Exception
    {
        if (toRemoveQueries == null)
        {
            toRemoveQueries = new HashSet<String>();
        }

        UUID dsoId = event.getSubjectID();
        int dsoType = event.getSubjectType();
        int eventType = event.getEventType();

        // Check if we are deleting something
        if (eventType == Event.DELETE)
        {
            // First make sure we delete everything for this dso
            String query = "id:" + dsoId + " AND type:" + dsoType;
            toRemoveQueries.add(query);
        }
        else if (eventType == Event.MODIFY && dsoType == Constants.ITEM)
        {
            // We have a modified item check for a withdraw/reinstate
        }
        else if (eventType == Event.MODIFY_METADATA
                && event.getSubjectType() == Constants.ITEM)
        {
            Item item = itemService.find(ctx, event.getSubjectID());

            String updateQuery = "id:" + item.getID() + " AND type:"
                    + item.getType();
            Map<String, List<String>> indexedValues = solrLoggerService.queryField(
                    updateQuery, null, null);

            // Get all the metadata
            List<String> storageFieldList = new ArrayList<String>();
            List<List<Object>> storageValuesList = new ArrayList<List<Object>>();

            solrLoggerService.update(updateQuery, "replace", storageFieldList,
                    storageValuesList);

        }

        if (eventType == Event.ADD && dsoType == Constants.COLLECTION
                && event.getObject(ctx) instanceof Item)
        {
            // We are mapping a new item make sure that the owning collection is
            // updated
            Item newItem = (Item) event.getObject(ctx);
            String updateQuery = "id: " + newItem.getID() + " AND type:"
                    + newItem.getType();

            List<String> fieldNames = new ArrayList<String>();
            List<List<Object>> valuesList = new ArrayList<List<Object>>();
            fieldNames.add("owningColl");
            fieldNames.add("owningComm");

            List<Object> valsList = new ArrayList<Object>();
            valsList.add(dsoId);
            valuesList.add(valsList);

            valsList = new ArrayList<Object>();
            valsList.addAll(findOwningCommunities(ctx, dsoId));
            valuesList.add(valsList);

            // Now make sure we also update the communities
            solrLoggerService.update(updateQuery, "addOne", fieldNames, valuesList);

        }
        else if (eventType == Event.REMOVE && dsoType == Constants.COLLECTION
                && event.getObject(ctx) instanceof Item)
        {
            // Unmapping items
            Item newItem = (Item) event.getObject(ctx);
            String updateQuery = "id: " + newItem.getID() + " AND type:"
                    + newItem.getType();

            List<String> fieldNames = new ArrayList<String>();
            List<List<Object>> valuesList = new ArrayList<List<Object>>();
            fieldNames.add("owningColl");
            fieldNames.add("owningComm");

            List<Object> valsList = new ArrayList<Object>();
            valsList.add(dsoId);
            valuesList.add(valsList);

            valsList = new ArrayList<Object>();
            valsList.addAll(findOwningCommunities(ctx, dsoId));
            valuesList.add(valsList);

            solrLoggerService.update(updateQuery, "remOne", fieldNames, valuesList);
        }
    }

    private List<Object> findOwningCommunities(Context context, UUID collId)
            throws SQLException
    {
        Collection coll = collectionService.find(context, collId);

        List<Object> owningComms = new ArrayList<Object>();
        for (int i = 0; i < coll.getCommunities().size(); i++)
        {
            Community community = coll.getCommunities().get(i);
            findComms(community, owningComms);
        }

        return owningComms;
    }

    private void findComms(Community comm, List<Object> parentComms)
            throws SQLException
    {
        if (comm == null)
        {
            return;
        }
        if (!parentComms.contains(comm.getID()))
        {
            parentComms.add(comm.getID());
        }
        List<Community> parentCommunities = comm.getParentCommunities();
        Community parent = parentCommunities.size() == 0 ? null : parentCommunities.get(0);
        findComms(parent, parentComms);
    }

    @Override
    public void end(Context ctx) throws Exception
    {
        if (toRemoveQueries != null)
        {
            for (String query : toRemoveQueries)
            {
                solrLoggerService.removeIndex(query);
            }
        }
        // clean out toRemoveQueries
        toRemoveQueries = null;
    }

    @Override
    public void finish(Context ctx) throws Exception
    {
    }

}
