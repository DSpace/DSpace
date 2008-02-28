/*
 * BrowseCreateDAO.java
 *
 * Version: $Revision: $
 *
 * Date: $Date:  $
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
package org.dspace.browse;

import java.util.List;
import java.util.Map;

/**
 * Interface for any class wishing to provide a browse storage later.  This particular
 * Data Access Object deals with building and destroying the database, and inserting and
 * removing content from it.  There is an alternative class BrowseDAO which deals with
 * Read-Only operations.
 * 
 * If you implement this class, and you wish it to be loaded via the BrowseDAOFactory
 * you must supply a constructor of the form:
 * 
 * public BrowseCreateDAOImpl(Context context) {}
 * 
 * Where Context is the DSpace Context object
 * 
 * Where tables are referred to in this class, they can be obtained from the BrowseIndex
 * class, which will answer queries given the context of the request on which table
 * is the relevant target.
 * 
 * @author Richard Jones
 *
 */
public interface BrowseCreateDAO
{
	// this must have a constructor which takes a DSpace Context as
	// an argument, thus:
	//
	// public BrowseCreateDAO(Context context)
	
	/**
	 * Delete the record for the given item id from the specified table.
	 * 
	 * Table names can be obtained from the BrowseIndex class
	 * 
	 * @param 	table	the browse table to remove the index from
	 * @param	itemID	the database id of the item to remove the index for
	 * @throws BrowseException
	 */
	public void deleteByItemID(String table, int itemID) throws BrowseException;

    public void deleteCommunityMappings(int itemID) throws BrowseException;
    public void updateCommunityMappings(int itemID) throws BrowseException;

	
	/**
	 * Insert an index record into the given table for the given item id.  The Map should contain
	 * key value pairs representing the sort column integer representation and the normalised
	 * value for that field.
	 * 
	 * For example, the caller might do as follows:
	 * 
	 * <code>
	 * Map map = new HashMap();
	 * map.put(new Integer(1), "the title");
	 * map.put(new Integer(2), "the subject");
	 * 
	 * BrowseCreateDAO dao = BrowseDAOFactory.getCreateInstance();
	 * dao.insertIndex("index_1", 21, map);
	 * </code>
	 * 
	 * @param table		the browse table to insert the index in
	 * @param itemID	the database id of the item being indexed
	 * @param sortCols	an Integer-String map of sort column numbers and values
	 * @throws BrowseException
	 */
    public void insertIndex(String table, int itemID, Map sortCols) throws BrowseException;

    /**
     * Updates an index record into the given table for the given item id.  The Map should contain
     * key value pairs representing the sort column integer representation and the normalised
     * value for that field.
     *
     * For example, the caller might do as follows:
     *
     * <code>
     * Map map = new HashMap();
     * map.put(new Integer(1), "the title");
     * map.put(new Integer(2), "the subject");
     *
     * BrowseCreateDAO dao = BrowseDAOFactory.getCreateInstance();
     * dao.updateIndex("index_1", 21, map);
     * </code>
     *
     * @param table		the browse table to insert the index in
     * @param itemID	the database id of the item being indexed
     * @param sortCols	an Integer-String map of sort column numbers and values
     * @return true if the record is updated, false if not found
     * @throws BrowseException
     */
    public boolean updateIndex(String table, int itemID, Map sortCols) throws BrowseException;

    /**
	 * Get the browse index's internal id for the location of the given string
	 * and sort value in the given table.  This method should always return a 
	 * positive integer, as if no existing ID is available for the given value
	 * then one should be inserted using the data supplied, and the ID returned.
	 * 
	 * Generally this method is used in conjunction with createDistinctMapping thus:
	 * 
	 * <code>
	 * BrowseCreateDAO dao = BrowseDAOFactory.getCreateInstance();
	 * dao.createDistinctMapping("index_1_distinct_map", 21, 
	 *           dao.getDistinctID("index_1_distinct", "Human Readable", "human readable"));
	 * </code>
	 * 
	 * When it creates a distinct record, it would usually do so through insertDistinctRecord
	 * defined below.
	 * 
	 * @param table		the table in which to look for/create the id
	 * @param value		the value on which to search
	 * @param sortValue	the sort value to use in case of the need to create
	 * @return			the database id of the distinct record
	 * @throws BrowseException
	 */
	public int getDistinctID(String table, String value, String sortValue) throws BrowseException;
	
	/**
	 * Insert the given value and sort value into the distinct index table.  This
	 * returns an integer which represents the database id of the created record, so
	 * that it can be used, for example in createDistinctMapping thus:
	 * 
	 * <code>
	 * BrowseCreateDAO dao = BrowseDAOFactory.getCreateInstance();
	 * dao.createDistinctMapping("index_1_distinct_map", 21,
	 * 			dao.insertDistinctRecord("index_1_distinct", "Human Readable", "human readable"));
	 * </code>
	 * 
	 * This is less good than using getDistinctID defined above, as if there is
	 * already a distinct value in the table it may throw an exception
	 * 
	 * @param table		the table into which to insert the record
	 * @param value		the value to insert
	 * @param sortValue	the sort value to insert
	 * @return			the database id of the created record
	 * @throws BrowseException
	 */
	public int insertDistinctRecord(String table, String value, String sortValue) throws BrowseException;

    /**
     * Update a mapping between an item id and a distinct metadata field such as an author,
     * who can appear in multiple items.  To get the id of the distinct record you should
     * use either getDistinctID or insertDistinctRecord as defined above.
     *
     * @param table		 	the mapping table
     * @param itemID		the item id
     * @param distinctIDs	the id of the distinct record
     * @throws BrowseException
     */
    public boolean updateDistinctMappings(String table, int itemID, int[] distinctIDs) throws BrowseException;

	/**
	 * Find out of a given table exists.
	 * 
	 * @param table		the table to test
	 * @return			true if exists, false if not
	 * @throws BrowseException
	 */
	public boolean testTableExistance(String table) throws BrowseException;
	
	/**
	 * Drop the given table name, and all other resources that are attached to it.  In normal
	 * relational database land this will include constraints and views.  If the boolean execute
	 * is true this operation should be carried out, and if it is false it should not.  The returned
	 * string should contain the SQL (if relevant) that the caller can do with what they like
	 * (for example, output to the screen).
	 * 
	 * @param table		The table to drop
	 * @param execute	Whether to action the removal or not
	 * @return			The instructions (SQL) that effect the removal
	 * @throws BrowseException
	 */
	public String dropIndexAndRelated(String table, boolean execute) throws BrowseException;
	
	/**
	 * Drop the given sequence name.  This is relevant to most forms of database, but not all.
	 * If the boolean execute is true this operation should be carried out, and if it is false
	 * it should not.  The returned string should contain the SQL (if relevant) that the caller
	 * can do with what they like (for example, output to the screen)
	 * 
	 * @param sequence		the sequence to drop
	 * @param execute		whether to action the removal or not
	 * @return				The instructions (SQL) that effect the removal
	 * @throws BrowseException
	 */
	public String dropSequence(String sequence, boolean execute) throws BrowseException;
	
    /**
     * Drop the given view name.  This is relevant to most forms of database, but not all.
     * If the boolean execute is true this operation should be carried out, and if it is false
     * it should not.  The returned string should contain the SQL (if relevant) that the caller
     * can do with what they like (for example, output to the screen)
     * 
     * @param view          the view to drop
     * @param execute       whether to action the removal or not
     * @return              The instructions (SQL) that effect the removal
     * @throws BrowseException
     */
    public String dropView(String view, boolean execute) throws BrowseException;
    
	/**
	 * Create the sequence with the given name.  This is relevant to most forms of database, but not all.
	 * If the boolean execute is true this operation should be carried out, and if it is false
	 * it should not.  The returned string should contain the SQL (if relevant) that the caller
	 * can do with what they like (for example, output to the screen)
	 * 
	 * @param sequence		the sequence to create
	 * @param execute		whether to action the create or not
	 * @return				the instructions (SQL) that effect the creation
	 * @throws BrowseException
	 */
	public String createSequence(String sequence, boolean execute) throws BrowseException;

    /**
     * Create the main index table.  This is the one which will contain a single row per
     * item.  If the boolean execute is true this operation should be carried out, and if it is false
     * it should not.  The returned string should contain the SQL (if relevant) that the caller
     * can do with what they like (for example, output to the screen)
     * 
     * This form is used for the primary item browse tables
     * 
     * This should be used, for example, like this:
     * 
     * <code>
     * List list = new ArrayList();
     * list.add(new Integer(1));
     * list.add(new Integer(2));
     * 
     * BrowseCreateDAO dao = BrowseDAOFactory.getCreateInstance();
     * dao.createPrimaryTable("index_1", list, true);
     * </code>
     * 
     * @param table     the raw table to create
     * @param sortCols  a List of Integers numbering the sort columns required
     * @param execute   whether to action the create or not
     * @return          the instructions (SQL) that effect the creation
     * @throws BrowseException
     */
    public String createPrimaryTable(String table, List sortCols, boolean execute) throws BrowseException;
	
    /**
	 * Create any indices that the implementing DAO sees fit to maximise performance.
	 * If the boolean execute is true this operation should be carried out, and if it is false
	 * it should not.  The returned string array should contain the SQL (if relevant) that the caller
	 * can do with what they like (for example, output to the screen).  It's an array so that
	 * you can return each bit of SQL as an element if you want.
	 * 
	 * @param table		the table upon which to create indices
	 * @param sortCols TODO
	 * @param execute	whether to action the create or not
	 * @return			the instructions (SQL) that effect the indices
	 * @throws BrowseException
	 */
	public String[] createDatabaseIndices(String table, List<Integer> sortCols, boolean value, boolean execute) throws BrowseException;

    /**
     * Create any indices that the implementing DAO sees fit to maximise performance.
     * If the boolean execute is true this operation should be carried out, and if it is false
     * it should not.  The returned string array should contain the SQL (if relevant) that the caller
     * can do with what they like (for example, output to the screen).  It's an array so that
     * you can return each bit of SQL as an element if you want.
     * 
     * @param disTable    the distinct table upon which to create indices
     * @param mapTable    the mapping table upon which to create indices
     * @param execute   whether to action the create or not
     * @return          the instructions (SQL) that effect the indices
     * @throws BrowseException
     */
    public String[] createMapIndices(String disTable, String mapTable, boolean execute) throws BrowseException;
	
	/**
	 * Create the View of the full item index as seen from a collection.
	 * If the boolean execute is true this operation should be carried out, and if it is false
	 * it should not.  The returned string array should contain the SQL (if relevant) that the caller
	 * can do with what they like (for example, output to the screen).
	 * 
	 * @param table		the table to create the view on
	 * @param view		the name of the view to create
	 * @param execute	whether to action the create or not
	 * @return			the instructions (SQL) that effects the create
	 * @throws BrowseException
	 */
	public String createCollectionView(String table, String view, boolean execute) throws BrowseException;
	
	/**
	 * Create the View of the full item index as seen from a community
	 * If the boolean execute is true this operation should be carried out, and if it is false
	 * it should not.  The returned string array should contain the SQL (if relevant) that the caller
	 * can do with what they like (for example, output to the screen).
	 * 
	 * @param table		the table to create the view on
	 * @param view		the name of the view to create
	 * @param execute	whether to action the create or not
	 * @return			the instructions (SQL) that effects the create
	 * @throws BrowseException
	 */
	public String createCommunityView(String table, String view, boolean execute) throws BrowseException;
	
	/**
	 * Create the table which will hold the distinct metadata values that appear in multiple
	 * items.  For example, this table may hold a list of unique authors, each name in the
	 * metadata for the entire system appearing only once.  Or for subject classifications.
	 * If the boolean execute is true this operation should be carried out, and if it is false
	 * it should not.  The returned string array should contain the SQL (if relevant) that the caller
	 * can do with what they like (for example, output to the screen).
	 * 
	 * @param table		the table to create
	 * @param execute	whether to action the create or not
	 * @return			the instructions (SQL) that effects the create
	 * @throws BrowseException
	 */
	public String createDistinctTable(String table, boolean execute) throws BrowseException;
	
	/**
	 * Create a table to hold a mapping between an item and a distinct metadata value that can appear
	 * across multiple items (for example, author names).  If the boolean execute is true this 
	 * operation should be carried out, and if it is false it should not.
	 * 
	 * @param table		the name of the distinct table which holds the target of the mapping
	 * @param map		the name of the mapping table itself
	 * @param execute	whether to execute the query or not
	 * @return
	 * @throws BrowseException
	 */
	public String createDistinctMap(String table, String map, boolean execute) throws BrowseException;
	
	/**
	 * So that any left over indices for items which have been deleted can be assured to have
	 * been removed, this method checks for indicies for items which are not in the item table.
	 * If it finds an index which does not have an associated item it removes it.
	 * 
	 * @param table		the index table to check
	 * @param map		the name of the associated distinct mapping table
	 * @param withdrawn TODO
	 * @throws BrowseException
	 */
	public void pruneExcess(String table, String map, boolean withdrawn) throws BrowseException;
	
	/**
	 * So that there are no distinct values indexed which are no longer referenced from the
	 * map table, this method checks for values which are not referenced from the map,
	 * and removes them.
	 * 
	 * @param table		the name of the distinct index table
	 * @param map		the name of the associated distinct mapping table.
	 * @throws BrowseException
	 */
	public void pruneDistinct(String table, String map) throws BrowseException;
}
