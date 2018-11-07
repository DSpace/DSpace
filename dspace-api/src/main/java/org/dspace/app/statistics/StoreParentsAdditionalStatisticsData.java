/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.statistics.StatisticsMetadataGenerator;


public class StoreParentsAdditionalStatisticsData implements
        StatisticsMetadataGenerator
{

    @Override
    public void addMetadata(SolrInputDocument document, HttpServletRequest request,
            DSpaceObject dso)
    {
        try
        {
            storeParents(document, dso);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    
    /**
     * Method just used to log the parents.
     * <ul>
     *  <li>Community log: owning comms.</li>
     *  <li>Collection log: owning comms & their comms.</li>
     *  <li>Item log: owning colls/comms.</li>
     *  <li>Bitstream log: owning item/colls/comms.</li>
     * </ul>
     * 
     * @param doc1
     *            the current SolrInputDocument
     * @param dso
     *            the current dspace object we want to log
     * @throws java.sql.SQLException
     *             ignore it
     */
    public void storeParents(SolrInputDocument doc1, DSpaceObject dso)
            throws SQLException
    {
        if (dso instanceof Community)
        {
            Community comm = (Community) dso;
            while (comm != null && comm.getParentCommunity() != null)
            {
                comm = comm.getParentCommunity();
                doc1.addField("owningComm", comm.getID());
            }
        }
        else if (dso instanceof Collection)
        {
            Collection coll = (Collection) dso;
            Community[] communities = coll.getCommunities();
            for (int i = 0; i < communities.length; i++)
            {
                Community community = communities[i];
                doc1.addField("owningComm", community.getID());
                storeParents(doc1, community);
            }
        }
        else if (dso instanceof Item)
        {
            Item item = (Item) dso;
            Collection[] collections = item.getCollections();
            for (int i = 0; i < collections.length; i++)
            {
                Collection collection = collections[i];
                doc1.addField("owningColl", collection.getID());
                storeParents(doc1, collection);
            }
        }
        else if (dso instanceof Bitstream)
        {
            Bitstream bitstream = (Bitstream) dso;
            Bundle[] bundles = bitstream.getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                Bundle bundle = bundles[i];
                Item[] items = bundle.getItems();
                for (int j = 0; j < items.length; j++)
                {
                    Item item = items[j];
                    doc1.addField("owningItem", item.getID());
                    storeParents(doc1, item);
                }
            }
        }
    }

}
