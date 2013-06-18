/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.util.HashSet;
import java.util.Set;

import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

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

    private Set<String> toRemoveQueries = null;

    DSpace dspace = new DSpace();

    SolrLogger indexer = dspace.getServiceManager().getServiceByName(SolrLogger.class.getName(),SolrLogger.class);
    
    public void initialize() throws Exception
    {

    }

    // TODO: checkout whether moving of collections, communities and bitstreams works
    // TODO: use async threaded consumer as this might require some processing time
    // TODO: we might be able to improve the performance: changing the collection will trigger 4 update commands
    public void consume(Context ctx, Event event) throws Exception
    {
        if (toRemoveQueries == null)
        {
            toRemoveQueries = new HashSet<String>();
        }

        int dsoId = event.getSubjectID();
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
            // use solr4 join feature no need to have metadata on core statistics
        }

        if (eventType == Event.ADD && dsoType == Constants.COLLECTION
                && event.getObject(ctx) instanceof Item)
        {
            // use solr4 join feature no need to have metadata on core statistics

        }
        else if (eventType == Event.REMOVE && dsoType == Constants.COLLECTION
                && event.getObject(ctx) instanceof Item)
        {
            // use solr4 join feature no need to have metadata on core statistics          
        }
    }


    public void end(Context ctx) throws Exception
    {
        if (toRemoveQueries != null)
        {
            for (String query : toRemoveQueries)
            {
                indexer.removeIndex(query);
            }
        }
        // clean out toRemoveQueries
        toRemoveQueries = null;
    }

    public void finish(Context ctx) throws Exception
    {
    }

}
