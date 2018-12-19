/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package ar.edu.unlp.sedici.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Constants;
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

		        String query = "SELECT col.*, m.text_value as community_name, m2.text_value as collection_name " 
        				+ "FROM collection col, community2collection c2c "
        				+ "LEFT JOIN metadatavalue m ON (m.resource_id = c2c.community_id and m.resource_type_id = ? and m.metadata_field_id = ?) "
        				+ "LEFT JOIN metadatavalue m2 ON (m2.resource_id = c2c.collection_id and m2.resource_type_id = ? and m2.metadata_field_id = ?) "
        				+ "WHERE col.collection_id=c2c.collection_id ORDER BY community_name, collection_name";
		        
		        int dcTitleID = MetadataField.findByElement(context, MetadataSchema.find(context, MetadataSchema.DC_SCHEMA).getSchemaID(), "title", null).getFieldID();
		        
		        tri = DatabaseManager.query(context,query,
		        			Constants.COMMUNITY, dcTitleID,
		        			Constants.COLLECTION, dcTitleID);
		        
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
	
    //TODO: revisar si este método se usa, por lo que pude encontrar de las referencias hasta el momento, no hay ninguna clase que utilice este método
	public static CollectionsWithCommunities findAllWithCommunitiesName(Context context) {
        // Get the bundle table rows

        List<Collection> collections = new ArrayList<Collection>();
        List<String> communitiesName = new ArrayList<String>();

        TableRowIterator tri;
		try {

			String query = "SELECT col.*, m.text_value as community_name, m2.text_value as collection_name " 
    				+ "FROM collection col, community2collection c2c "
    				+ "LEFT JOIN metadatavalue m ON (m.resource_id = c2c.community_id and m.resource_type_id = ? and m.metadata_field_id = ?) "
    				+ "LEFT JOIN metadatavalue m2 ON (m2.resource_id = c2c.collection_id and m2.resource_type_id = ? and m2.metadata_field_id = ?) "
    				+ "WHERE col.collection_id=c2c.collection_id ORDER BY community_name, collection_name";
	        
	        int dcTitleID = MetadataField.findByElement(context, MetadataSchema.find(context, MetadataSchema.DC_SCHEMA).getSchemaID(), "title", null).getFieldID();
	        
	        tri = DatabaseManager.query(context,query,
	        			Constants.COMMUNITY, dcTitleID,
	        			Constants.COLLECTION, dcTitleID);

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
