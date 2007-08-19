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
import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;

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
    	throws SQLException, BrowseException
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
    
    /**
     * Remove any indexed information for the given item from the given index
     * 
     * @param item		the item to remove
     * @param bi		the index to remove from
     * @throws BrowseException
     */
    private void removeIndex(Item item, BrowseIndex bi)
    	throws BrowseException
    {
        removeIndex(item.getID(), bi);
    }
    
    /**
     * Remove any indexed information for the given item from the given index
     * 
     * @param itemID    the ID of the item to remove
     * @param bi        the index to remove from
     * @throws BrowseException
     */
    private void removeIndex(int itemID, BrowseIndex bi)
        throws BrowseException
    {
        if (bi.isMetadataIndex())
        {
            // remove old metadata from the item index
            dao.deleteByItemID(bi.getTableName(), itemID);
            dao.deleteByItemID(bi.getMapName(), itemID);
        }
    }
    
    private void removeIndex(int itemID, String table)
        throws BrowseException
    {
        dao.deleteByItemID(table, itemID);
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
        indexItem(new ItemMetadataProxy(item));
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
            // Remove from the item indexes (archive and withdrawn)
            removeIndex(item.getID(), BrowseIndex.getItemBrowseIndex().getTableName());
            removeIndex(item.getID(), BrowseIndex.getWithdrawnBrowseIndex().getTableName());

            // Index any archived item that isn't withdrawn
            if (item.isArchived() && !item.isWithdrawn())
            {
                Map<Integer, String> sortMap = getSortValues(item, itemMDMap);
                dao.insertIndex(BrowseIndex.getItemBrowseIndex().getTableName(), item.getID(), sortMap);
            }
            else if (item.isWithdrawn())
            {
                // If it's withdrawn, add it to the withdrawn items index
                Map<Integer, String> sortMap = getSortValues(item, itemMDMap);
                dao.insertIndex(BrowseIndex.getWithdrawnBrowseIndex().getTableName(), item.getID(), sortMap);
            }

            // Now update the metadata indexes
            for (int i = 0; i < bis.length; i++)
            {
                log.debug("Indexing for item " + item.getID() + ", for index: " + bis[i].getTableName());
                
                if (bis[i].isMetadataIndex())
                {
                    // remove old metadata from the item index
                    removeIndex(item.getID(), bis[i]);
        
                    // now index the new details - but only if it's archived and not withdrawn
                    if (item.isArchived() && !item.isWithdrawn())
                    {
                        // get the metadata from the item
                        String[] md = bis[i].getMdBits();
                        DCValue[] values = item.getMetadata(md[0], md[1], md[2], Item.ANY);
                        
                        // if we have values to index on, then do so
                        if (values != null)
                        {
                            for (int x = 0; x < values.length; x++)
                            {
                                // get the normalised version of the value
                                String nVal = BrowseOrder.makeSortString(values[x].value, values[x].language, bis[i].getDataType()); 
            
                                Map sortMap = getSortValues(item, itemMDMap);
                                
                                dao.insertIndex(bis[i].getTableName(), item.getID(), values[x].value, nVal, sortMap);
                                
                                if (bis[i].isMetadataIndex())
                                {
                                    int distinctID = dao.getDistinctID(bis[i].getTableName(true, false, false), values[x].value, nVal);
                                    dao.createDistinctMapping(bis[i].getMapName(), item.getID(), distinctID);
                                }
                            }
                        }
                    }
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
                String nValue = BrowseOrder.makeSortString(value.value, value.language, so.getType());
                sortMap.put(key, nValue);
            }
        }
        return sortMap;
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
		// go over the indices and index the item
		for (int i = 0; i < bis.length; i++)
		{
			log.debug("Removing indexing for removed item " + item.getID() + ", for index: " + bis[i].getTableName());
			removeIndex(item, bis[i]);
	    }
	    
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
	    IndexBrowse indexer = new IndexBrowse();
	    
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
    			logMe.append(" " + so.getMetadata() + " ");
    		}
    		
    		output.message("Creating browse index " + bis[i].getName() + 
    				": index by " + bis[i].getMetadata() + 
    				" sortable by: " + logMe.toString());
        }
    }
    
	/**
	 * delete all the existing browse tables
	 * 
	 * @throws BrowseException
	 */
    private void clearDatabase()
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
                String colViewName = BrowseIndex.getTableName(i, false, true, false, false);
                String comViewName = BrowseIndex.getTableName(i, true, false, false, false);
    			String distinctTableName = BrowseIndex.getTableName(i, false, false, true, false);
    			String distinctMapName = BrowseIndex.getTableName(i, false, false, false, true);
                String distinctColViewName = BrowseIndex.getTableName(i, false, true, false, true);
                String distinctComViewName = BrowseIndex.getTableName(i, true, false, false, true);
    			String sequence = BrowseIndex.getSequenceName(i, false, false);
    			String mapSequence = BrowseIndex.getSequenceName(i, false, true);
    			String distinctSequence = BrowseIndex.getSequenceName(i, true, false);

    			output.message("Checking for " + tableName);
    			if (!dao.testTableExistance(tableName))
    			{
                    if (i < bis.length)
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
        			
        			output.message("Deleting old index and associated resources: " + tableName);
        			
        			// prepare a statement which will delete the table and associated
        			// resources
        			String dropper = dao.dropIndexAndRelated(tableName, this.execute());
        			String dropSeq = dao.dropSequence(sequence, this.execute());
                    String dropColView = dao.dropView( colViewName, this.execute() );
                    String dropComView = dao.dropView( comViewName, this.execute() );
        			
        			output.sql(dropper);
        			output.sql(dropSeq);
                    output.sql(dropColView);
                    output.sql(dropComView);
    
    
        			// NOTE: we need a secondary context to check for the existance
        			// of the table, because if an SQLException is thrown, then
        			// the connection is aborted, and no more transaction stuff can be
        			// done.  Therefore we use a blank context to make the requests,
        			// not caring if it gets aborted or not
        			
        			output.message("Checking for " + distinctTableName);
        			boolean distinct = true;
        			if (!dao.testTableExistance(distinctTableName))
        			{
        				output.message("... no distinct index for this table");
        				distinct = false;
        			}
        			else
        			{
        				output.message("...found");
        			}
        			
        			if (distinct)
        			{
        				// prepare statements that will delete the distinct value tables
        				String dropDistinctTable = dao.dropIndexAndRelated(distinctTableName, this.execute());
        				String dropMap = dao.dropIndexAndRelated(distinctMapName, this.execute());
        				String dropDistinctMapSeq = dao.dropSequence(mapSequence, this.execute());
        				String dropDistinctSeq = dao.dropSequence(distinctSequence, this.execute());
                        String dropDistinctColView = dao.dropView( distinctColViewName, this.execute() );
                        String dropDistinctComView = dao.dropView( distinctComViewName, this.execute() );
        				
        				output.sql(dropDistinctTable);
        				output.sql(dropMap);
        				output.sql(dropDistinctMapSeq);
        				output.sql(dropDistinctSeq);
                        output.sql(dropDistinctColView);
                        output.sql(dropDistinctComView);
        			}
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
            String tableName = bix.getTableName(false, false, false, false);
            String colViewName = bix.getTableName(false, true, false, false);
            String comViewName = bix.getTableName(true, false, false, false);
            String dropper = dao.dropIndexAndRelated(tableName, this.execute());
            String dropSeq = dao.dropSequence( bix.getSequenceName(false, false), this.execute() );
            String dropColView = dao.dropView( colViewName, this.execute() );
            String dropComView = dao.dropView( comViewName, this.execute() );

            output.sql(dropper);
            output.sql(dropSeq);
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
        String tableName = bix.getTableName(false, false, false, false);
        String colViewName = bix.getTableName(false, true, false, false);
        String comViewName = bix.getTableName(true, false, false, false);
        
        String itemSeq   = dao.createSequence(bix.getSequenceName(false, false), this.execute());
        String itemTable = dao.createPrimaryTable(tableName, sortCols, execute);
        String[] itemIndices = dao.createDatabaseIndices(tableName, sortCols, false, this.execute());
        String itemColView = dao.createCollectionView(tableName, colViewName, this.execute());
        String itemComView = dao.createCommunityView(tableName, comViewName, this.execute());

        output.sql(itemSeq);
        output.sql(itemTable);
        for (int i = 0; i < itemIndices.length; i++)
        {
            output.sql(itemIndices[i]);
        }
        output.sql(itemColView);
        output.sql(itemComView);
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
	        // prepare the array list of sort options
            List<Integer> sortCols = new ArrayList<Integer>();
            for (SortOption so : SortOption.getSortOptions())
            {
                sortCols.add(new Integer(so.getNumber()));
            }

	        // get the table names that the browse index is in charge of
			String tableName   = bi.getTableName();
			String sequence    = bi.getSequenceName(false, false);
			String colViewName = bi.getTableName(false, true);
			String comViewName = bi.getTableName(true, false);
			
			// if this is a single view, create the DISTINCT tables and views
			if (bi.isMetadataIndex())
			{
	            String createSeq = dao.createSequence(sequence, this.execute());
	            String createTable = dao.createSecondaryTable(tableName, sortCols, this.execute());
	            String[] databaseIndices = dao.createDatabaseIndices(tableName, sortCols, true, this.execute());
                String createColView = dao.createCollectionView(tableName, colViewName, this.execute());
                String createComView = dao.createCommunityView(tableName, comViewName, this.execute());
	            
	            output.sql(createSeq);
	            output.sql(createTable);
	            for (int i = 0; i < databaseIndices.length; i++)
	            {
	                output.sql(databaseIndices[i]);
	            }
                output.sql(createColView);
                output.sql(createComView);

	            // if this is a single view, create the DISTINCT tables and views
	            String distinctTableName = bi.getTableName(false, false, true, false);
				String distinctSeq = bi.getSequenceName(true, false);
				String distinctMapName = bi.getTableName(false, false, false, true);
				String mapSeq = bi.getSequenceName(false, true);
				String distinctColMapName = bi.getTableName(false, true, false, true);
				String distinctComMapName = bi.getTableName(true, false, false, true);
				
				
				// FIXME: at the moment we have not defined INDEXes for this data
				// add this later when necessary
				
				String distinctTableSeq = dao.createSequence(distinctSeq, this.execute());
				String distinctMapSeq = dao.createSequence(mapSeq, this.execute());
				String createDistinctTable = dao.createDistinctTable(distinctTableName, this.execute());
				String createDistinctMap = dao.createDistinctMap(distinctTableName, distinctMapName, this.execute());
				String createDistinctColView = dao.createCollectionView(distinctMapName, distinctColMapName, this.execute());
				String createDistinctComView = dao.createCommunityView(distinctMapName, distinctComMapName, this.execute());
				
				output.sql(distinctTableSeq);
				output.sql(distinctMapSeq);
				output.sql(createDistinctTable);
				output.sql(createDistinctMap);
				output.sql(createDistinctColView);
				output.sql(createDistinctComView);
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
    private void initBrowse()
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
            BrowseItemDAO biDao = BrowseDAOFactory.getItemInstance(context);
            BrowseItem[] items = biDao.findAll();

    		// go through every item id, grab the relevant metadata
    		// and write it into the database
    		
    		for (int j = 0; j < items.length; j++)
    		{
                indexItem(new ItemMetadataProxy(items[j].getID(), items[j]));
    			
    			// after each item we commit the context and clear the cache
    			context.commit();
    			context.clearCache();
    		}
    		
    		// penultimately we have to delete any items that couldn't be located in the
    		// index list
    		for (int k = 0; k < bis.length; k++)
    		{
                if (bis[k].isMetadataIndex())
                {
                    dao.pruneExcess(bis[k].getTableName(false, false, false, false), bis[k].getTableName(false, false, false, true), false);
    				dao.pruneDistinct(bis[k].getTableName(false, false, true, false), bis[k].getTableName(false, false, false, true));
    			}
    		}

            dao.pruneExcess(BrowseIndex.getItemBrowseIndex().getTableName(false, false, false, false), null, false);
            dao.pruneExcess(BrowseIndex.getWithdrawnBrowseIndex().getTableName(false, false, false, false), null, true);
            
            // Make sure the deletes are written back
            context.commit();
    		
    		return items.length;
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
	private class ItemMetadataProxy
	{
	    private Item item;
	    private BrowseItem browseItem;
	    private int id;
	    
	    ItemMetadataProxy(Item item)
	    {
	        this.item       = item;
	        this.browseItem = null;
	        this.id         = 0;
	    }

	    ItemMetadataProxy(int id, BrowseItem browseItem)
	    {
	        this.item       = null;
	        this.browseItem = browseItem;
	        this.id         = id;
	    }

	    public DCValue[] getMetadata(String schema, String element, String qualifier, String lang)
	        throws SQLException
	    {
	        if (item != null)
	        {
	            return item.getMetadata(schema, element, qualifier, lang);
	        }
	        
	        return browseItem.getMetadata(schema, element, qualifier, lang);
	    }
	    
	    public int getID()
	    {
	        if (item != null)
	        {
	            return item.getID();
	        }
	        
	        return id;
	    }
	    
	    /**
	     * Is the Item archived?
	     * @return
	     */
	    public boolean isArchived()
	    {
	    	if (item != null)
	    	{
	    		return item.isArchived();
	    	}
	    	
	    	return browseItem.isArchived();
	    }
	    
        /**
         * Is the Item withdrawn?
         * @return
         */
        public boolean isWithdrawn()
        {
            if (item != null)
            {
            	return item.isWithdrawn();
            }
            
            return browseItem.isWithdrawn();
        }
	}
}
