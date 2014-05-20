
package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class CollectionSearchSedici {

    public static CollectionsWithCommunities findAuthorizedWithCommunitiesName(Context context, Community comm,
            int actionID) throws java.sql.SQLException
    {

		try {
			if (comm!=null){
				return CollectionSearchSedici.findAuthorizedWithCommunitiesNameRecursive(context, comm, actionID);
			} else {
		        List<Collection> collections = new ArrayList<Collection>();
		        List<String> communitiesName = new ArrayList<String>();
		        TableRowIterator tri;		        

				tri = DatabaseManager.queryTable(context,null,
				                "SELECT collection.*, community.name as community_name FROM collection, community, community2collection WHERE " +
				                "community.community_id=community2collection.community_id "+
				                " AND collection.collection_id=community2collection.collection_id" +
				                " ORDER BY community_name, collection.name"
				                );
	
		        // Build a list of Community objects
		        Collection fromCache;
		        String communityName;
		        try
		        {
		            while (tri.hasNext())
		            {
		                TableRow row = tri.next();
		
		                // First check the cache
		                fromCache = (Collection) context.fromCache(
		                        Collection.class, row.getIntColumn("collection_id"));
		                communityName=row.getStringColumn("community_name");
		
		                if (fromCache == null)
		                {
		                	fromCache=new Collection(context, row);
		                }
		            	if (AuthorizeManager.authorizeActionBoolean(context, fromCache, actionID)){	    					
		                	collections.add(fromCache);
		                    communitiesName.add(communityName);
		            	}                
		                
		            }
		        }
		        finally
		        {
		            // close the TableRowIterator to free up resources
		            if (tri != null)
		            {
		                tri.close();
		            }
		        }
		        return new CollectionsWithCommunities(collections, communitiesName);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}


	}
    
    public static CollectionsWithCommunities findAuthorizedWithCommunitiesNameRecursive(Context context, Community comm,
            int actionID) throws java.sql.SQLException
    {
        List<Collection> collections = new ArrayList<Collection>();
        List<String> communitiesName = new ArrayList<String>();
        Collection[] colecciones=comm.getCollections();
        Community[] subcomunidades=comm.getSubcommunities();
        String communityName=comm.getName();
        CollectionsWithCommunities recursividad;
        for (Collection coleccion : colecciones) {
        	if (AuthorizeManager.authorizeActionBoolean(context, coleccion, actionID)){
        		collections.add(coleccion);
        		communitiesName.add(communityName);
        	}    
		}
        for (Community community : subcomunidades) {
			recursividad=CollectionSearchSedici.findAuthorizedWithCommunitiesNameRecursive(context, community, actionID);
			collections.addAll(recursividad.getCollections());
			communitiesName.addAll(recursividad.getCommunitiesName());
		}
        return new CollectionsWithCommunities(collections, communitiesName);
	}
	
	public static CollectionsWithCommunities findAllWithCommunitiesName(Context context) {
        // Get the bundle table rows

        List<Collection> collections = new ArrayList<Collection>();
        List<String> communitiesName = new ArrayList<String>();

        TableRowIterator tri;
		try {

				tri = DatabaseManager.queryTable(context,null,
				                "SELECT collection.*, community.name as community_name FROM collection, community, community2collection WHERE " +
				                "community.community_id=community2collection.community_id "+
				                " AND collection.collection_id=community2collection.collection_id" +
				                " ORDER BY community_name, collection.name"
				                );

        // Build a list of Community objects
        Collection fromCache;
        String communityName;
        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                fromCache = (Collection) context.fromCache(
                        Collection.class, row.getIntColumn("collection_id"));
                communityName=row.getStringColumn("community_name");

                if (fromCache == null)
                {
                	fromCache=new Collection(context, row);
                }
            	collections.add(fromCache);
                communitiesName.add(communityName);
                
                
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return new CollectionsWithCommunities(collections, communitiesName);
	}
	
}
