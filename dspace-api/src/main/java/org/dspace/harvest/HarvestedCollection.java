package org.dspace.harvest;

/*
 * HarvestedCollection.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2008/01/01 04:11:09 $
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.TableRow;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Alexey Maslov
 */

public class HarvestedCollection 
{
	private Context context;
	private TableRow harvestRow;
	boolean modified;
	
	public static final int TYPE_NONE = 0;
	public static final int TYPE_DMD = 1;
	public static final int TYPE_DMDREF = 2;
	public static final int TYPE_FULL = 3;
	
	public static final int STATUS_READY = 0;
	public static final int STATUS_BUSY = 1;
	public static final int STATUS_QUEUED = 2;
	public static final int STATUS_OAI_ERROR = 3;
	public static final int STATUS_UNKNOWN_ERROR = -1;
	
	/*
	 * 	collection_id      | integer                  | not null
 		harvest_type       | integer                  | 
 		oai_source         | text                     | 
 		oai_set_id         | text                     | 
 		harvest_message    | text                     | 
 		metadata_config_id | text                     | 
 		harvest_status     | integer                  | 
 		harvest_start_time | timestamp with time zone | 
	 */  
	
	// TODO: make sure this guy knows to lock people out if the status is not zero.
	// i.e. someone editing a collection's setting from the admin menu should have
	// to stop an ongoing harvest before they can edit the settings. 
   
    
	HarvestedCollection(Context c, TableRow row)
    {
        context = c;
        harvestRow = row;
        modified = false;
    }
    
    
    public static void exists(Context c) throws SQLException {
    	DatabaseManager.queryTable(c, "harvested_collection", "SELECT COUNT(*) FROM harvested_collection");    	
    }
    
    
    /**
     * Find the harvest settings corresponding to this collection 
     * @return a HarvestInstance object corresponding to this collection's settings, null if not found.
     */
    public static HarvestedCollection find(Context c, int collectionId) throws SQLException 
    {
    	TableRow row = DatabaseManager.findByUnique(c, "harvested_collection", "collection_id", collectionId);
    	
    	if (row == null) {
    		return null;
    	}
    	
    	return new HarvestedCollection(c, row);
    }
    
    /**
     * Create a new harvest instance row for a specified collection.  
     * @return a new HarvestInstance object
     */
    public static HarvestedCollection create(Context c, int collectionId) throws SQLException {
    	TableRow row = DatabaseManager.create(c, "harvested_collection");
    	row.setColumn("collection_id", collectionId);
    	row.setColumn("harvest_type", 0);
    	DatabaseManager.update(c, row);
    	
    	return new HarvestedCollection(c, row);    	
    }
    
    /** Returns whether the specified collection is harvestable, i.e. whether its harvesting 
     * options are set up correctly. This is distinct from "ready", since this collection may
     * be in process of being harvested.
     */
    public static boolean isHarvestable(Context c, int collectionId) throws SQLException 
    {
    	HarvestedCollection hc = HarvestedCollection.find(c, collectionId); 
    	if (hc != null && hc.getHarvestType() > 0 && hc.getOaiSource() != null && hc.getOaiSetId() != null && 
    			hc.getHarvestStatus() != HarvestedCollection.STATUS_UNKNOWN_ERROR) {
    		return true;
    	}
    	return false;   
    }
    
    /** Returns whether this harvest instance is actually harvestable, i.e. whether its settings
     * options are set up correctly. This is distinct from "ready", since this collection may
     * be in process of being harvested.
     */
    public boolean isHarvestable() throws SQLException 
    {
    	if (this.getHarvestType() > 0 && this.getOaiSource() != null && this.getOaiSetId() != null && 
    			this.getHarvestStatus() != HarvestedCollection.STATUS_UNKNOWN_ERROR) {
    		return true;
    	}

    	return false;   
    }
    
    /** Returns whether the specified collection is ready for immediate harvest. 
     */
    public static boolean isReady(Context c, int collectionId) throws SQLException 
    {
    	HarvestedCollection hc = HarvestedCollection.find(c, collectionId);
    	return hc.isReady();
    }
    
    public boolean isReady() throws SQLException 
    {
    	if (this.isHarvestable() &&	(this.getHarvestStatus() == HarvestedCollection.STATUS_READY || this.getHarvestStatus() == HarvestedCollection.STATUS_OAI_ERROR))
    		return true;

    	return false;   
    }
    
    
    /** Find all collections that are set up for harvesting 
     * 
     * return: list of collection id's
     * @throws SQLException 
     */
    public static List<Integer> findAll(Context c) throws SQLException 
    {
    	TableRowIterator tri = DatabaseManager.queryTable(c, "harvested_collection",
        	"SELECT * FROM harvested_collection");
    	
    	List<Integer> collectionIds = new ArrayList<Integer>();
    	while (tri.hasNext())
    	{
    		TableRow row = tri.next();
    		collectionIds.add(row.getIntColumn("collection_id"));
    	}
    	
    	return collectionIds;
    }
    
    /** Find all collections that are ready for harvesting 
     * 
     * return: list of collection id's
     * @throws SQLException 
     */
    public static List<Integer> findReady(Context c) throws SQLException 
    {
    	int harvestInterval = ConfigurationManager.getIntProperty("harvester.harvestFrequency");
    	if (harvestInterval == 0) harvestInterval = 720;
    	
    	int expirationInterval = ConfigurationManager.getIntProperty("harvester.threadTimeout");
    	if (expirationInterval == 0) expirationInterval = 24;

    	Date startTime;
        Date expirationTime;
    	
    	Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.MINUTE, -1 * harvestInterval);
		startTime = calendar.getTime();
		
		calendar.setTime(startTime);
		calendar.add(Calendar.HOUR, -2 * expirationInterval);
		expirationTime = calendar.getTime();
    	
    	/* Select all collections whose last_harvest is before our start time, whose harvest_type *is not* 0 and whose status *is* 0 (available) or 3 (OAI Error). */
    	TableRowIterator tri = DatabaseManager.queryTable(c, "harvested_collection",
        	"SELECT * FROM harvested_collection WHERE (last_harvested < ? or last_harvested is null) and harvest_type > ? and (harvest_status = ? or harvest_status = ? or (harvest_status=? and harvest_start_time < ?)) ORDER BY last_harvested",
        	new java.sql.Timestamp(startTime.getTime()), 0, HarvestedCollection.STATUS_READY, HarvestedCollection.STATUS_OAI_ERROR, HarvestedCollection.STATUS_BUSY, new java.sql.Timestamp(expirationTime.getTime()));
    	
    	List<Integer> collectionIds = new ArrayList<Integer>();

    	while (tri.hasNext())
    	{
    		TableRow row = tri.next();
    		collectionIds.add(row.getIntColumn("collection_id"));
    	}
    	
    	return collectionIds;
    }
    
    /**
     * Find all collections with the specified status flag 
     * @param c
     * @param status, see HarvestInstance.STATUS_...
     * @return
     * @throws SQLException
     */
    public static List<Integer> findByStatus(Context c, int status) throws SQLException {
    	TableRowIterator tri = DatabaseManager.queryTable(c, "harvested_collection",	
    			"SELECT * FROM harvested_collection WHERE harvest_status = ?", status);
	
		List<Integer> collectionIds = new ArrayList<Integer>();
		while (tri.hasNext())
		{
			TableRow row = tri.next();
			collectionIds.add(row.getIntColumn("collection_id"));
		}
		
		return collectionIds;
    }
    
    
    /** Find the collection that was harvested the longest time ago. 
     * @throws SQLException 
     */
    public static Integer findOldestHarvest (Context c) throws SQLException {
    	String query = "select collection_id from harvested_collection where harvest_type > ? and harvest_status = ? order by last_harvested asc limit 1"; 
        
    	if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
    	    query = "select collection_id from harvested_collection where harvest_type > ? and harvest_status = ? and rownum <= 1  order by last_harvested asc"; 
    	    
        TableRowIterator tri = DatabaseManager.queryTable(c, "harvested_collection", 
    			query, 0, 0);
    	TableRow row = tri.next();
    	
    	if (row != null)
    		return row.getIntColumn("collection_id");
    	else
    		return -1;
    }
    
    /** Find the collection that was harvested most recently. 
     * @throws SQLException 
     */
    public static Integer findNewestHarvest (Context c) throws SQLException {
        String query = "select collection_id from harvested_collection where harvest_type > ? and harvest_status = ? order by last_harvested desc limit 1"; 
        
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
            query = "select collection_id from harvested_collection where harvest_type > ? and harvest_status = ? and rownum <= 1 order by last_harvested desc";
        
    	TableRowIterator tri = DatabaseManager.queryTable(c, "harvested_collection", 
    			query , 0, 0);
    	TableRow row = tri.next();
		
    	if (row != null)
    		return row.getIntColumn("collection_id");
    	else
    		return -1;
    }
    
    
    /** 
     * A function to set all harvesting-related parameters at once 
     */
    public void setHarvestParams(int type, String oaiSource, String oaiSetId, String mdConfigId) {
   		setHarvestType(type);
    	setOaiSource(oaiSource);
    	setOaiSetId(oaiSetId); 
    	setHarvestMetadataConfig(mdConfigId);
    }     

    /* Setters for the appropriate harverting-related columns */
    public void setHarvestType(int type) {
    	harvestRow.setColumn("harvest_type",type);
    	modified = true;
    }
    
    /** 
     * Sets the current status of the collection.
     *    
     * @param	status	a HarvestInstance.STATUS_... constant
     */
    public void setHarvestStatus(int status) {
    	harvestRow.setColumn("harvest_status",status);
    	modified = true;
    }

    public void setOaiSource(String oaiSource) {
    	if (oaiSource == null || oaiSource.length() == 0) {
    		harvestRow.setColumnNull("oai_source");
    	}
    	else {
    		harvestRow.setColumn("oai_source",oaiSource);
    	}
    	modified = true;
    }

    public void setOaiSetId(String oaiSetId) {
    	if (oaiSetId == null || oaiSetId.length() == 0) {
    		harvestRow.setColumnNull("oai_set_id");
    	}
    	else {
    		harvestRow.setColumn("oai_set_id",oaiSetId);
    	}
    	modified = true;
    }

    public void setHarvestMetadataConfig(String mdConfigId) {
    	if (mdConfigId == null || mdConfigId.length() == 0) {
    		harvestRow.setColumnNull("metadata_config_id");
    	}
    	else {
    		harvestRow.setColumn("metadata_config_id",mdConfigId);
    	}
    	modified = true;    	 
    }

    public void setHarvestResult(Date date, String message) {
    	if (date == null) {
    		harvestRow.setColumnNull("last_harvested");
    	} else {
    		harvestRow.setColumn("last_harvested", date);
    	}

    	if (message == null || message.length() == 0) {
    		harvestRow.setColumnNull("harvest_message");
    	} else {
    		harvestRow.setColumn("harvest_message", message);
    	}
    	modified = true;
    }

    public void setHarvestMessage(String message) {
    	if (message == null || message.length() == 0) {
    		harvestRow.setColumnNull("harvest_message");
    	} else {
    		harvestRow.setColumn("harvest_message", message);
    	}
    	modified = true;
    }
    
    public void setHarvestStartTime(Date date) {
    	if (date == null) {
    		harvestRow.setColumnNull("harvest_start_time");
    	} else {
    		harvestRow.setColumn("harvest_start_time", date);
    	}
    	modified = true;
    }
    

    /* Getting for the appropriate harverting-related columns */
    public int getCollectionId() {
    	return harvestRow.getIntColumn("collection_id");
    }
    
    public int getHarvestType() {
    	return harvestRow.getIntColumn("harvest_type");
    }
    
    public int getHarvestStatus() {
    	return harvestRow.getIntColumn("harvest_status");
    }

    public String getOaiSource() {
    	return harvestRow.getStringColumn("oai_source");
    }

    public String getOaiSetId() {
    	return harvestRow.getStringColumn("oai_set_id");
    }

    public String getHarvestMetadataConfig() {
    	return harvestRow.getStringColumn("metadata_config_id");    	 
    }
    
    public String getHarvestMessage() {
    	return harvestRow.getStringColumn("harvest_message");
    }

    public Date getHarvestDate() {
    	return harvestRow.getDateColumn("last_harvested");
    }
    
    public Date getHarvestStartTime() {
    	return harvestRow.getDateColumn("harvest_start_time");
    }
    
    
    
    public void delete() throws SQLException {
    	DatabaseManager.delete(context, harvestRow);
    }
    
    public void update() throws SQLException, IOException, AuthorizeException
    {
        DatabaseManager.update(context, harvestRow);

    }

    
    
    
}
