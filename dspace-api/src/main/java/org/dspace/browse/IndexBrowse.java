/*
 * IndexBrowse.java
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Context;
import org.dspace.sort.SortOption;
import org.dspace.sort.SortException;
import org.dspace.sort.OrderFormat;

/**
 * Tool to create Browse indexes.  This class is used from the command line to
 * create and destroy the browse indices from configuration, and also from within
 * the application to add and remove content from those tables.
 * 
 * To see a full definition of the usage of this class just run it without any
 * arguments, and you will get the help message.
 * 
 * @author Richard Jones
 */
public class IndexBrowse
{
	/** logger */
	private static Logger log = Logger.getLogger(IndexBrowse.class);
	
	/** DSpace context */
	private Context context;
	
	/** whether to destroy and rebuild the database */
	private boolean rebuild = false;
	
	/** whether to destroy the database */
	private boolean delete = false;
	
	/** the index number to start working from (for debug only) */
	private int start = 1;
	
	/** whether to execute the commands generated against the database */
	private boolean execute = false;
	
	/** whether there is an output file into which to write SQL */
	private boolean fileOut = false;
	
	/** whether the output should be written to the standadr out */
	private boolean stdOut = false;
	
	/** the name of the output file */
	private String outFile = null;
	
	/** should the operations be verbose */
	private boolean verbose = false;
	
	/** the configured browse indices */
	private BrowseIndex[] bis;
	
	/** the DAO for write operations on the database */
	private BrowseCreateDAO dao;
	
	/** the outputter class */
	private BrowseOutput output;
	
    /**
     * Construct a new index browse.  If done this way, an internal
     * DSpace context will be created.  Better instead to call
     * 
     * <code>
     * new IndexBrowse(context);
     * </code>
     * 
     * with your desired context (when using with the application)
     * 
     * @throws SQLException
     * @throws BrowseException
     */
    public IndexBrowse()
    	throws SQLException, BrowseException
    {
    	this(new Context());
    }
    
    /**
     * Create a new IndexBrowse object.  This will ignore any authorisations
     * applied to the Context
     * 
     * @param context
     * @throws SQLException
     * @throws BrowseException
     */
    public IndexBrowse(Context context)
    	throws BrowseException
    {
    	this.context = context;
    	this.context.setIgnoreAuthorization(true);
    	
    	// get the browse indices, and ensure that
    	// we have all the relevant tables prepped
        this.bis = BrowseIndex.getBrowseIndices();
        checkConfig();
        
        // get the DAO for the create operations
        dao = BrowseDAOFactory.getCreateInstance(context);
        
        // set the outputter
        output = new BrowseOutput();
        
        // then generate all the metadata bits that we
        // are going to use
        for (int k = 0; k < bis.length; k++)
    	{
    		bis[k].generateMdBits();
    	}
    }
    
    /**
	 * @return Returns the verbose.
	 */
	public boolean isVerbose()
	{
		return verbose;
	}

	/**
	 * @param verbose The verbose to set.
	 */
	public void setVerbose(boolean verbose)
	{
		this.verbose = verbose;
		output.setVerbose(verbose);
	}

	/**
	 * @return	true if to rebuild the database, false if not
	 */
	public boolean rebuild()
    {
    	return rebuild;
    }
    
	/**
	 * @param bool		whether to rebuild the database or not
	 */
    public void setRebuild(boolean bool)
    {
    	this.rebuild = bool;
    }
    
    /**
     * @return		true if to delete the database, false if not
     */
    public boolean delete()
    {
    	return delete;
    }
    
    /**
     * @param bool	whetehr to delete the database or not
     */
    public void setDelete(boolean bool)
    {
    	this.delete = bool;
    }
    
    /**
     * @param start		the index to start working up from
     */
    public void setStart(int start)
    {
    	this.start = start;
    }
    
    /**
     * @return		the index to start working up from
     */
    public int getStart()
    {
    	return this.start;
    }
    
    /**
     * @param bool		whether to execute the database commands or not
     */
    public void setExecute(boolean bool)
    {
    	this.execute = bool;
    }
    
    /**
     * @return		true if to execute database commands, false if not
     */
    public boolean execute()
    {
    	return this.execute;
    }
    
    /**
     * @param bool	whether to use an output file
     */
    public void setFileOut(boolean bool)
    {
    	this.fileOut = bool;
    	output.setFile(bool);
    }
    
    /**
     * @return		true if using an output file, false if not
     */
    public boolean isFileOut()
    {
    	return this.fileOut;
    }
    
    /**
     * @param bool		whether to write to standard out
     */
    public void setStdOut(boolean bool)
    {
    	this.stdOut = bool;
    	output.setPrint(bool);
    }
    
    /**
     * @return	true if to write to standard out, false if not
     */
    public boolean toStdOut()
    {
    	return this.stdOut;
    }
    
    /**
     * @param file		the name of the output file
     */
    public void setOutFile(String file)
    {
    	this.outFile = file;
    	output.setFileName(file);
    }
    
    /**
     * @return	the name of the output file
     */
    public String getOutFile()
    {
    	return this.outFile;
    }
        
    private void removeIndex(int itemID, String table)
        throws BrowseException
    {
        dao.deleteByItemID(table, itemID);
    }

    /**
     * Prune indexes - called from the public interfaces or at the end of a batch indexing process
     */
    private void pruneIndexes() throws BrowseException
    {
        // go over the indices and prune
        for (int i = 0; i < bis.length; i++)
        {
            if (bis[i].isMetadataIndex())
            {
                log.debug("Pruning metadata index: " + bis[i].getTableName());
                dao.pruneExcess(bis[i].getTableName(), bis[i].getMapTableName(), false);
                dao.pruneDistinct(bis[i].getDistinctTableName(), bis[i].getMapTableName());
            }
        }

        dao.pruneExcess(BrowseIndex.getItemBrowseIndex().getTableName(), null, false);
        dao.pruneExcess(BrowseIndex.getWithdrawnBrowseIndex().getTableName(), null, true);
    }

    /**
     * Index the given item
     * 
     * @param item	the item to index
     * @throws BrowseException
     */
    public void indexItem(Item item)
    	throws BrowseException
    {
        // If the item is not archived AND has not been withdrawn
        // we can assume that it has *never* been archived - in that case,
        // there won't be anything in the browse index, so we can just skip processing.
        // If it is either archived or withdrawn, then there may be something in the browse
        // tables, so we *must* process it.
        // Caveat: an Item.update() that changes isArchived() from TRUE to FALSE, whilst leaving
        // isWithdrawn() as FALSE, may result in stale data in the browse tables.
        // Such an update should never occur though, and if it does, probably indicates a major
        // problem with the code updating the Item.
        if (item.isArchived() || item.isWithdrawn())
        {
            indexItem(new ItemMetadataProxy(item));

            // Ensure that we remove any invalid entries
            pruneIndexes();
        }
    }
    
       /**
         * Index the given item
         * 
         * @param item  the item to index
         * @throws BrowseException
         */
    private void indexItem(ItemMetadataProxy item)
        throws BrowseException
    {
        // Map to store the metadata from the Item
        // so that we don't grab it multiple times
        Map<String, String> itemMDMap = new HashMap<String, String>();
        
        try
        {
            // Delete community mappings - we'll add them again if necessary
            dao.deleteCommunityMappings(item.getID());

            Map<Integer, String> sortMap = getSortValues(item, itemMDMap);
            if (item.isArchived() && !item.isWithdrawn())
            {
                // Try to update an existing record in the item index
                if (!dao.updateIndex(BrowseIndex.getItemBrowseIndex().getTableName(), item.getID(), sortMap))
                {
                    // Record doesn't exist - ensure that it doesn't exist in the withdrawn index,
                    // and add it to the archived item index
                    removeIndex(item.getID(), BrowseIndex.getWithdrawnBrowseIndex().getTableName());
                    dao.insertIndex(BrowseIndex.getItemBrowseIndex().getTableName(), item.getID(), sortMap);
                }
                dao.insertCommunityMappings(item.getID());
            }
            else if (item.isWithdrawn())
            {
                // Try to update an existing record in the withdrawn index
                if (!dao.updateIndex(BrowseIndex.getWithdrawnBrowseIndex().getTableName(), item.getID(), sortMap))
                {
                    // Record doesn't exist - ensure that it doesn't exist in the item index,
                    // and add it to the withdrawn item index
                    removeIndex(item.getID(), BrowseIndex.getItemBrowseIndex().getTableName());
                    dao.insertIndex(BrowseIndex.getWithdrawnBrowseIndex().getTableName(), item.getID(), sortMap);
                }
            }
            else
            {
                // This item shouldn't exist in either index - ensure that it is removed
                removeIndex(item.getID(), BrowseIndex.getItemBrowseIndex().getTableName());
                removeIndex(item.getID(), BrowseIndex.getWithdrawnBrowseIndex().getTableName());
            }

            // Now update the metadata indexes
            for (int i = 0; i < bis.length; i++)
            {
                log.debug("Indexing for item " + item.getID() + ", for index: " + bis[i].getTableName());
                
                if (bis[i].isMetadataIndex())
                {
                    boolean itemMapped = false;

                    // now index the new details - but only if it's archived and not withdrawn
                    if (item.isArchived() && !item.isWithdrawn())
                    {
                        // get the metadata from the item
                        for (int mdIdx = 0; mdIdx < bis[i].getMetadataCount(); mdIdx++)
                        {
                            String[] md = bis[i].getMdBits(mdIdx);
                            DCValue[] values = item.getMetadata(md[0], md[1], md[2], Item.ANY);

                            // if we have values to index on, then do so
                            if (values != null)
                            {
                                for (int x = 0; x < values.length; x++)
                                {
                                    // Ensure that there is a value to index before inserting it
                                    if (StringUtils.isEmpty(values[x].value))
                                    {
                                        log.error("Null metadata value for item " + item.getID() + ", field: " +
                                                values[x].schema + "." +
                                                values[x].element +
                                                (values[x].qualifier == null ? "" : "." + values[x].qualifier));
                                    }
                                    else
                                    {
                                        // get the normalised version of the value
                                        String nVal = OrderFormat.makeSortString(values[x].value, values[x].language, bis[i].getDataType());
                                        int distinctID = dao.getDistinctID(bis[i].getDistinctTableName(), values[x].value, nVal);

                                        // Update the existing mapping, or create a new one if it doesn't exist
                                        if (!dao.updateDistinctMapping(bis[i].getMapTableName(), item.getID(), distinctID))
                                            dao.createDistinctMapping(bis[i].getMapTableName(), item.getID(), distinctID);

                                        itemMapped = true;
                                    }
                                }
                            }
                        }
                    }

                    // remove any old mappings
                    if (!itemMapped)
                        removeIndex(item.getID(), bis[i].getMapTableName());
                }
            }
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /**
     * Get the normalised values for each of the sort columns
     * @param item
     * @param itemMDMap
     * @return
     * @throws BrowseException
     * @throws SQLException
     */
    private Map<Integer, String> getSortValues(ItemMetadataProxy item, Map itemMDMap)
            throws BrowseException, SQLException
    {
        try
        {
            // now obtain the sort order values that we will use
            Map<Integer, String> sortMap = new HashMap<Integer, String>();
            for (SortOption so : SortOption.getSortOptions())
            {
                Integer key = new Integer(so.getNumber());
                String metadata = so.getMetadata();

                // If we've already used the metadata for this Item
                // it will be cached in the map
                DCValue value = null;

                if (itemMDMap != null)
                    value = (DCValue) itemMDMap.get(metadata);

                // We haven't used this metadata before, so grab it from the item
                if (value == null)
                {
                    String[] somd = so.getMdBits();
                    DCValue[] dcv = item.getMetadata(somd[0], somd[1], somd[2], Item.ANY);

                    if (dcv == null)
                    {
                        continue;
                    }

                    // we only use the first dc value
                    if (dcv.length > 0)
                    {
                        // Set it as the current metadata value to use
                        // and add it to the map
                        value = dcv[0];

                        if (itemMDMap != null)
                            itemMDMap.put(metadata, dcv[0]);
                    }
                }

                // normalise the values as we insert into the sort map
                if (value != null && value.value != null)
                {
                    String nValue = OrderFormat.makeSortString(value.value, value.language, so.getType());
                    sortMap.put(key, nValue);
                }
            }

            return sortMap;
        }
        catch (SortException se)
        {
            throw new BrowseException("Error in SortOptions", se);
        }
    }
    
    /**
     * @deprecated
     * @param item
     * @return
     * @throws BrowseException
     */
    public boolean itemAdded(Item item)
		throws BrowseException
	{
		indexItem(item);
	    return true;
	}

    /**
     * @deprecated
     * @param item
     * @return
     * @throws BrowseException
     */
	public boolean itemChanged(Item item)
		throws BrowseException
	{
		indexItem(item);
	    return true;
	}

    /**
	 * remove all the indices for the given item
	 * 
	 * @param item		the item to be removed
	 * @return
	 * @throws BrowseException
	 */
	public boolean itemRemoved(Item item)
		throws BrowseException
    {
        return itemRemoved(item.getID());
    }

    public boolean itemRemoved(int itemID)
            throws BrowseException
    {
		// go over the indices and index the item
		for (int i = 0; i < bis.length; i++)
		{
		    if (bis[i].isMetadataIndex())
		    {
                log.debug("Removing indexing for removed item " + itemID + ", for index: " + bis[i].getTableName());
                removeIndex(itemID, bis[i].getMapTableName());
            }
	    }

        // Remove from the item indexes (archive and withdrawn)
        removeIndex(itemID, BrowseIndex.getItemBrowseIndex().getTableName());
        removeIndex(itemID, BrowseIndex.getWithdrawnBrowseIndex().getTableName());
        dao.deleteCommunityMappings(itemID);

        // Ensure that we remove any invalid entries
        pruneIndexes();

        return true;
	}

	/**
	 * Creates Browse indexes, destroying the old ones.
	 * 
	 * @param argv
	 *            Command-line arguments
	 */
	public static void main(String[] argv)
		throws SQLException, BrowseException, ParseException
	{
        Context context = new Context();
        IndexBrowse indexer = new IndexBrowse(context);
	    
	    // create an options object and populate it
	    CommandLineParser parser = new PosixParser();
	    Options options = new Options();
	   
	    // these are mutually exclusive, and represent the primary actions
	    options.addOption("t", "tables", false, "create the tables only, do not attempt to index.  Mutually exclusive with -f and -i");
	    options.addOption("i", "index", false, "actually do the indexing.  Mutually exclusive with -t and -f");
	    options.addOption("f", "full", false, "make the tables, and do the indexing.  This forces -x.  Mutually exclusive with -t and -i");
	    
	    // these options can be specified only with the -f option
	    options.addOption("r", "rebuild", false, "should we rebuild all the indices, which removes old index tables and creates new ones.  For use with -f. Mutually exclusive with -d");
	    options.addOption("d", "delete", false, "delete all the indices, but don't create new ones.  For use with -f. This is mutually exclusive with -r");
	    
	    // these options can be specified only with the -t and -f options
	    options.addOption("o", "out", true, "[-o <filename>] write the remove and create SQL to the given file. For use with -t and -f");  // FIXME: not currently working
	    options.addOption("p", "print", false, "write the remove and create SQL to the stdout. For use with -t and -f");
	    options.addOption("x", "execute", false, "execute all the remove and create SQL against the database. For use with -t and -f");
	    options.addOption("s", "start", true, "[-s <int>] start from this index number and work upward (mostly only useful for debugging). For use with -t and -f");
	    
	    // this option can be used with any argument
	    options.addOption("v", "verbose", false, "print extra information to the stdout.  If used in conjunction with -p, you cannot use the stdout to generate your database structure");
	    
	    // display the help.  If this is spefified, it trumps all other arguments
	    options.addOption("h", "help", false, "show this help documentation.  Overrides all other arguments");
	    
	    CommandLine line = parser.parse(options, argv);
	    
	    // display the help
	    if (line.hasOption("h"))
	    {
	    	indexer.usage(options);
	    	return;
	    }
	    
	    if (line.hasOption("v"))
	    {
	    	indexer.setVerbose(true);
	    }
	    
	    if (line.hasOption("i"))
	    {
	    	indexer.createIndex();
	    	return;
	    }
	    
	    if (line.hasOption("f"))
	    {
	    	if (line.hasOption('r'))
		    {
		        indexer.setRebuild(true);
		    }
	    	else if (line.hasOption("d"))
		    {
		    	indexer.setDelete(true);
		    }
	    }
	    
	    if (line.hasOption("f") || line.hasOption("t"))
	    {
	    	if (line.hasOption("s"))
		    {
		    	indexer.setStart(Integer.parseInt(line.getOptionValue("s")));
		    }
	    	if (line.hasOption("x"))
		    {
		    	indexer.setExecute(true);
		    }
	    	if (line.hasOption("p"))
	    	{
	    		indexer.setStdOut(true);
	    	}
	    	if (line.hasOption("o"))
	    	{
	    		indexer.setFileOut(true);
	    		indexer.setOutFile(line.getOptionValue("o"));
	    	}
	    }
	    
	    if (line.hasOption("t"))
	    {
	    	indexer.prepTables();
	    	return;
	    }
	    
	    if (line.hasOption("f"))
	    {
	    	indexer.setExecute(true);
	    	indexer.initBrowse();
	    	return;
	    }
	    
	    indexer.usage(options);
        context.complete();
	}

	/**
	 * output the usage information
	 * 
	 * @param options
	 */
	private void usage(Options options)
	{
		HelpFormatter formatter = new HelpFormatter();
    	formatter.printHelp("IndexBrowse", options);
	}
	
	/**
	 * Prepare the tables for the browse indices
	 * 
	 * @throws BrowseException
	 */
	private void prepTables()
    	throws BrowseException
    {
        try
        {
            // first, erase the existing indexes
            clearDatabase();

            createItemTables();

            // for each current browse index, make all the relevant tables
            for (int i = 0; i < bis.length; i++)
            {
                createTables(bis[i]);

                // prepare some CLI output
                StringBuffer logMe = new StringBuffer();
                for (SortOption so : SortOption.getSortOptions())
                {
                    logMe.append(" ").append(so.getMetadata()).append(" ");
                }

                output.message("Creating browse index " + bis[i].getName() +
                        ": index by " + bis[i].getMetadata() +
                        " sortable by: " + logMe.toString());
            }
        }
        catch (SortException se)
        {
            throw new BrowseException("Error in SortOptions", se);
        }
    }
    
	/**
	 * delete all the existing browse tables
	 * 
	 * @throws BrowseException
	 */
    public void clearDatabase()
		throws BrowseException
	{
    	try
    	{
    		output.message("Deleting old indices");
    		
    		// notice that we have to do this without reference to the BrowseIndex[]
    		// because they do not necessarily reflect what currently exists in
    		// the database
    		
    		int i = getStart();
    		while (true)
    		{
    			String tableName = BrowseIndex.getTableName(i, false, false, false, false);
                String distinctTableName = BrowseIndex.getTableName(i, false, false, true, false);
                String distinctMapName = BrowseIndex.getTableName(i, false, false, false, true);
                String sequence = BrowseIndex.getSequenceName(i, false, false);
                String mapSequence = BrowseIndex.getSequenceName(i, false, true);
                String distinctSequence = BrowseIndex.getSequenceName(i, true, false);

                // These views are no longer used, but as we are cleaning the database,
                // they may exist and need to be removed
                String colViewName = BrowseIndex.getTableName(i, false, true, false, false);
                String comViewName = BrowseIndex.getTableName(i, true, false, false, false);
                String distinctColViewName = BrowseIndex.getTableName(i, false, true, false, true);
                String distinctComViewName = BrowseIndex.getTableName(i, true, false, false, true);

    			output.message("Checking for " + tableName);
    			if (dao.testTableExistance(tableName))
                {
                    output.message("...found");

                    output.message("Deleting old index and associated resources: " + tableName);

                    // prepare a statement which will delete the table and associated
                    // resources
                    String dropper = dao.dropIndexAndRelated(tableName, this.execute());
                    String dropSeq = dao.dropSequence(sequence, this.execute());
                    output.sql(dropper);
                    output.sql(dropSeq);

                    // These views are no longer used, but as we are cleaning the database,
                    // they may exist and need to be removed
                    String dropColView = dao.dropView( colViewName, this.execute() );
                    String dropComView = dao.dropView( comViewName, this.execute() );
                    output.sql(dropColView);
                    output.sql(dropComView);
                }

                // NOTE: we need a secondary context to check for the existance
                // of the table, because if an SQLException is thrown, then
                // the connection is aborted, and no more transaction stuff can be
                // done.  Therefore we use a blank context to make the requests,
                // not caring if it gets aborted or not
                output.message("Checking for " + distinctTableName);
                if (!dao.testTableExistance(distinctTableName))
    			{
                    if (i < bis.length || i < 10)
                    {
                        output.message("... doesn't exist; but will carry on as there may be something that conflicts");
                    }
                    else
                    {
        				output.message("... doesn't exist; no more tables to delete");
        				break;
                    }
    			}
                else
                {
        			output.message("...found");

        			output.message("Deleting old index and associated resources: " + distinctTableName);

    				// prepare statements that will delete the distinct value tables
    				String dropDistinctTable = dao.dropIndexAndRelated(distinctTableName, this.execute());
    				String dropMap = dao.dropIndexAndRelated(distinctMapName, this.execute());
    				String dropDistinctMapSeq = dao.dropSequence(mapSequence, this.execute());
    				String dropDistinctSeq = dao.dropSequence(distinctSequence, this.execute());
                    output.sql(dropDistinctTable);
                    output.sql(dropMap);
                    output.sql(dropDistinctMapSeq);
                    output.sql(dropDistinctSeq);

                    // These views are no longer used, but as we are cleaning the database,
                    // they may exist and need to be removed
                    String dropDistinctColView = dao.dropView( distinctColViewName, this.execute() );
                    String dropDistinctComView = dao.dropView( distinctComViewName, this.execute() );
                    output.sql(dropDistinctColView);
                    output.sql(dropDistinctComView);
                }

    			i++;
    		}

            dropItemTables(BrowseIndex.getItemBrowseIndex());
            dropItemTables(BrowseIndex.getWithdrawnBrowseIndex());
            
    		if (execute())
    		{
    			context.commit();
    		}
    	}
    	catch (SQLException e)
    	{
    		log.error("caught exception: ", e);
    		throw new BrowseException(e);
    	}
	}

    /**
     * drop the tables and related database entries for the internal
     * 'item' tables
     * @param bix
     * @throws BrowseException
     */
    private void dropItemTables(BrowseIndex bix) throws BrowseException
    {
        if (dao.testTableExistance(bix.getTableName()))
        {
            String tableName = bix.getTableName();
            String dropper = dao.dropIndexAndRelated(tableName, this.execute());
            String dropSeq = dao.dropSequence( bix.getSequenceName(false, false), this.execute() );
            output.sql(dropper);
            output.sql(dropSeq);

            // These views are no longer used, but as we are cleaning the database,
            // they may exist and need to be removed
            String colViewName = bix.getTableName(false, true, false, false);
            String comViewName = bix.getTableName(true, false, false, false);
            String dropColView = dao.dropView( colViewName, this.execute() );
            String dropComView = dao.dropView( comViewName, this.execute() );
            output.sql(dropColView);
            output.sql(dropComView);
        }
    }

    /**
     * Create the internal full item tables
     * @throws BrowseException
     */
    private void createItemTables() throws BrowseException
    {
        try
        {
            // prepare the array list of sort options
            List<Integer> sortCols = new ArrayList<Integer>();
            for (SortOption so : SortOption.getSortOptions())
            {
                sortCols.add(new Integer(so.getNumber()));
            }

            createItemTables(BrowseIndex.getItemBrowseIndex(), sortCols);
            createItemTables(BrowseIndex.getWithdrawnBrowseIndex(), sortCols);
            
            if (execute())
            {
                context.commit();
            }
        }
        catch (SortException se)
        {
            throw new BrowseException("Error in SortOptions", se);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /**
     * Create the internal full item tables for a particular index
     * (ie. withdrawn / in archive)
     * @param bix
     * @param sortCols
     * @throws BrowseException
     */
    private void createItemTables(BrowseIndex bix, List<Integer> sortCols)
            throws BrowseException
    {
        String tableName = bix.getTableName();
        
        String itemSeq   = dao.createSequence(bix.getSequenceName(false, false), this.execute());
        String itemTable = dao.createPrimaryTable(tableName, sortCols, execute);
        String[] itemIndices = dao.createDatabaseIndices(tableName, sortCols, false, this.execute());

        output.sql(itemSeq);
        output.sql(itemTable);
        for (int i = 0; i < itemIndices.length; i++)
        {
            output.sql(itemIndices[i]);
        }
    }
    /**
     * Create the browse tables for the given browse index
     * 
     * @param bi		the browse index to create
     * @throws BrowseException
     */
	private void createTables(BrowseIndex bi)
    	throws BrowseException
    {
		try
		{
			// if this is a single view, create the DISTINCT tables and views
			if (bi.isMetadataIndex())
			{
	            // if this is a single view, create the DISTINCT tables and views
	            String distinctTableName = bi.getDistinctTableName();
				String distinctSeq = bi.getSequenceName(true, false);
				String distinctMapName = bi.getMapTableName();
				String mapSeq = bi.getSequenceName(false, true);
				
				// FIXME: at the moment we have not defined INDEXes for this data
				// add this later when necessary
				
				String distinctTableSeq = dao.createSequence(distinctSeq, this.execute());
				String distinctMapSeq = dao.createSequence(mapSeq, this.execute());
				String createDistinctTable = dao.createDistinctTable(distinctTableName, this.execute());
				String createDistinctMap = dao.createDistinctMap(distinctTableName, distinctMapName, this.execute());
                String[] mapIndices = dao.createMapIndices(distinctTableName, distinctMapName, this.execute());
				
				output.sql(distinctTableSeq);
				output.sql(distinctMapSeq);
				output.sql(createDistinctTable);
				output.sql(createDistinctMap);
                for (int i = 0; i < mapIndices.length; i++)
                {
                    output.sql(mapIndices[i]);
                }
			}

			if (execute())
			{
				context.commit();
			}
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new BrowseException(e);
		}
    }
    
	/**
	 * index everything
	 * 
	 * @throws SQLException
	 * @throws BrowseException
	 */
    public void initBrowse()
		throws SQLException, BrowseException
	{
		Date start = new Date();
		
		output.message("Creating browse indexes for DSpace");
		
	    Date initDate = new Date();
	    long init = initDate.getTime() - start.getTime();
	    
	    output.message("init complete (" + Long.toString(init) + " ms)");
	    
	    if (delete())
	    {
	    	output.message("Deleting browse tables");
	    	
	    	clearDatabase();
	    	
	    	output.message("Browse tables deleted");
	    	return;
	    }
	    else if (rebuild())
	    {
	    	output.message("Preparing browse tables");
	    	
	    	prepTables();
	    	
	    	output.message("Browse tables prepared");
	    }
	    
	    Date prepDate = new Date();
	    long prep = prepDate.getTime() - start.getTime();
	    long prepinit = prepDate.getTime() - initDate.getTime();
	    
	    output.message("tables prepped (" + Long.toString(prep) + " ms, " + Long.toString(prepinit) + " ms)");
	    
	    int count = createIndex();
	    
	    context.complete();
	    
	    Date endDate = new Date();
	    long end = endDate.getTime() - start.getTime();
	    long endprep = endDate.getTime() - prepDate.getTime();
	    
	    output.message("content indexed (" + Long.toString(end) + " ms, " + Long.toString(endprep) + " ms)");
	    output.message("Items indexed: " + Integer.toString(count));
	    
	    if (count > 0)
	    {
	    	long overall = end / count;
	    	long specific = endprep / count;
	    	
	    	output.message("Overall average time per item: " + Long.toString(overall) + " ms");
	    	output.message("Index only average time per item: " + Long.toString(specific) + " ms");
	    }
	   
	    output.message("Browse indexing completed");
	}

    /**
     * create the indices for all the items
     * 
     * @return
     * @throws BrowseException
     */
    private int createIndex()
    	throws BrowseException
    {
    	try
    	{
    		// first, pre-prepare the known metadata fields that we want to query
    		// on
    		for (int k = 0; k < bis.length; k++)
    		{
    			bis[k].generateMdBits();
    		}
    		
    		// now get the ids of ALL the items in the database
//            BrowseItemDAO biDao = BrowseDAOFactory.getItemInstance(context);
//            BrowseItem[] items = biDao.findAll();
            ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
            List<Item> items = itemDAO.getItems();

    		// go through every item id, grab the relevant metadata
    		// and write it into the database
    		
//    		for (int j = 0; j < items.length; j++)
    		for (Item item : items)
    		{
//                indexItem(new ItemMetadataProxy(items[j].getID(), items[j]));
                indexItem(new ItemMetadataProxy(item));
    			
    			// after each item we commit the context and clear the cache
    			context.commit();
    			context.clearCache();
    		}
    		
    		// penultimately we have to delete any items that couldn't be located in the
    		// index list
            pruneIndexes();
            
            // Make sure the deletes are written back
            context.commit();
    		
//    		return items.length;
    		return items.size();
    	}
    	catch (SQLException e)
    	{
    		log.error("caught exception: ", e);
    		throw new BrowseException(e);
    	}
    }
    
    /**
     * Currently does nothing
     *
     */
    private void checkConfig()
    {
        // FIXME: exactly in what way do we want to check the config?
    }
    
    /**
	 * Take a string representation of a metadata field, and return it as an array.
	 * This is just a convenient utility method to basically break the metadata 
	 * representation up by its delimiter (.), and stick it in an array, inserting
	 * the value of the init parameter when there is no metadata field part.
	 * 
	 * @param mfield	the string representation of the metadata
	 * @param init	the default value of the array elements
	 * @return	a three element array with schema, element and qualifier respectively
	 */
	public String[] interpretField(String mfield, String init)
		throws IOException
	{
		StringTokenizer sta = new StringTokenizer(mfield, ".");
		String[] field = {init, init, init};
		
		int i = 0;
		while (sta.hasMoreTokens())
		{
			field[i++] = sta.nextToken();
		}
		
		// error checks to make sure we have at least a schema and qualifier for both
		if (field[0] == null || field[1] == null)
		{
			throw new IOException("at least a schema and element be " +
					"specified in configuration.  You supplied: " + mfield);
		}
		
		return field;
	}
	
	// private inner class
	//	 Hides the Item / BrowseItem in such a way that we can remove
	//	 the duplication in indexing an item.
    // FIXME: I (JR) think this is pointless now that BrowseItem doesn't exist.
	private class ItemMetadataProxy
	{
	    private Item item;
//	    private BrowseItem browseItem;
	    private int id;
	    
	    ItemMetadataProxy(Item item)
	    {
	        this.item       = item;
//	        this.browseItem = null;
	        this.id         = 0;
	    }

//	    ItemMetadataProxy(int id, BrowseItem browseItem)
//	    {
//	        this.item       = null;
//	        this.browseItem = browseItem;
//	        this.id         = id;
//	    }

	    public DCValue[] getMetadata(String schema, String element, String qualifier, String lang)
	        throws SQLException
	    {
	        if (item != null)
	        {
	            return item.getMetadata(schema, element, qualifier, lang);
	        }
	        
            throw new IllegalStateException("this shouldn't have happened");
//	        return browseItem.getMetadata(schema, element, qualifier, lang);
	    }
	    
	    public int getID()
	    {
//	        if (item != null)
//	        {
	            return item.getID();
//	        }
//	        
//	        return id;
	    }
	    
	    /**
	     * Is the Item archived?
	     * @return
	     */
	    public boolean isArchived()
	    {
//	    	if (item != null)
//	    	{
	    		return item.isArchived();
//	    	}
//	    	
//	    	return browseItem.isArchived();
	    }
	    
        /**
         * Is the Item withdrawn?
         * @return
         */
        public boolean isWithdrawn()
        {
//            if (item != null)
//            {
            	return item.isWithdrawn();
//            }
//            
//            return browseItem.isWithdrawn();
        }
	}
}
