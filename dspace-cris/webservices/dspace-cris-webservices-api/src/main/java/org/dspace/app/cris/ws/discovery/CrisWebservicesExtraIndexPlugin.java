/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws.discovery;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.configuration.DiscoverySearchFilter;


/**
 * Plugin to add extra information in the index needed to the webservices
 * infrastructure
 * 
 * @author pascarelli
 * @author bollini
 */
public class CrisWebservicesExtraIndexPlugin implements SolrServiceIndexPlugin
{
    private static final Logger log = Logger
            .getLogger(CrisWebservicesExtraIndexPlugin.class);

    public static final String FIELDNAME_COMMUNITIES_HANDLE = "customlocation.communitiesHandle";

    public static final String FIELDNAME_COMMUNITIES_NAME = "customlocation.communitiesName";

    public static final String FIELDNAME_COMMUNITIES = "customlocation.communities";

    public static final String FIELDNAME_COLLECTIONS_HANDLE = "customlocation.collectionsHandle";

    public static final String FIELDNAME_COLLECTIONS_NAME = "customlocation.collectionsName";

    public static final String FIELDNAME_COLLECTIONS = "customlocation.collections";

    public static final String[] OTHERS_TO_INDEX = { FIELDNAME_COMMUNITIES,
            FIELDNAME_COMMUNITIES_NAME, FIELDNAME_COMMUNITIES_HANDLE,
            FIELDNAME_COLLECTIONS, FIELDNAME_COLLECTIONS_NAME,
            FIELDNAME_COLLECTIONS_HANDLE };

       
    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        if (!(dso instanceof Item))
        {
            return;
        }
        try
        {
            Item myitem = (Item) dso;
            List<String> collectionsList = new Vector<String>();
            List<String> collectionsName = new Vector<String>();
            List<String> collectionsHandle = new Vector<String>();
            List<String> communitiesList = new Vector<String>();
            List<String> communitiesName = new Vector<String>();
            List<String> communitiesHandle = new Vector<String>();
    
            // build list of community ids
            Community[] communities = myitem.getCommunities();
    
            // build list of collection ids
            Collection[] collections = myitem.getCollections();
    
            // now put those into strings
            int i = 0;
    
            for (i = 0; i < communities.length; i++)
            {
                communitiesList.add(new String("" + communities[i].getID()));
                communitiesName.add(communities[i].getName());
                communitiesHandle.add(communities[i].getHandle());
            }
    
            for (i = 0; i < collections.length; i++)
            {
                collectionsList.add(new String("" + collections[i].getID()));
                collectionsName.add(collections[i].getName());
                collectionsHandle.add(collections[i].getHandle());
            }
    
            document.addField(FIELDNAME_COLLECTIONS, collectionsList);
            document.addField(FIELDNAME_COLLECTIONS_NAME, collectionsName);
            document.addField(FIELDNAME_COLLECTIONS_HANDLE, collectionsHandle);
            document.addField(FIELDNAME_COMMUNITIES, communitiesList);
            document.addField(FIELDNAME_COMMUNITIES_NAME, communitiesName);
            document.addField(FIELDNAME_COMMUNITIES_HANDLE, communitiesHandle);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }
}
