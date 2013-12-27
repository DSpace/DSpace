/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.MetadataAuthorityManager;
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
    	throws SQLException, BrowseException
    {
    	this.context = context;
    	
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
                pruneDistinctIndex(bis[i], null);
            }
        }

        dao.pruneExcess(BrowseIndex.getItemBrowseIndex().getTableName(), false);
        dao.pruneExcess(BrowseIndex.getWithdrawnBrowseIndex().getTableName(), true);
        dao.pruneExcess(BrowseIndex.getPrivateBrowseIndex().getTableName(), true);
    }

    private void pruneDistinctIndex(BrowseIndex bi, List<Integer> removedIds) throws BrowseException
    {
        dao.pruneMapExcess(bi.getMapTableName(), false, removedIds);
        dao.pruneDistinct(bi.getDistinctTableName(), bi.getMapTableName(), removedIds);
    }

    /**
     * Index the given item
     * 
     * @param item	the item to index
     * @throws BrowseException
     */
    public void indexItem(Item item) throws BrowseException
    {
        indexItem(item, false);
    }

    void indexItem(Item item, boolean addingNewItem) throws BrowseException
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
        if (item.isArchived())
        {
            indexItem(new ItemMetadataProxy(item), addingNewItem);
        }
        else if (item.isWithdrawn() || !item.isArchived())
        {
            indexItem(new ItemMetadataProxy(item), false);
        }
    }
    
       /**
         * Index the given item
         * 
         * @param item  the item to index
         * @throws BrowseException
         */
    private void indexItem(ItemMetadataProxy item, boolean addingNewItem)
        throws BrowseException
    {
        // Map to store the metadata from the Item
        // so that we don't grab it multiple times
        Map<String, String> itemMDMap = new HashMap<String, String>();
        
        try
        {
            boolean reqCommunityMappings = false;
            Map<Integer, String> sortMap = getSortValues(item, itemMDMap);
            if (item.isArchived() && item.isDiscoverable())
            {
                // Try to update an existing record in the item index
                if (!dao.updateIndex(BrowseIndex.getItemBrowseIndex().getTableName(), item.getID(), sortMap))
                {
                    // Record doesn't exist - ensure that it doesn't exist in the withdrawn index,
                    // and add it to the archived item index
                    dao.deleteByItemID(BrowseIndex.getWithdrawnBrowseIndex().getTableName(), item.getID());
                    dao.deleteByItemID(BrowseIndex.getPrivateBrowseIndex().getTableName(), item.getID());
                    dao.insertIndex(BrowseIndex.getItemBrowseIndex().getTableName(), item.getID(), sortMap);
                }

                reqCommunityMappings = true;
            }
            else if (!item.isDiscoverable())
            {
            	if (!dao.updateIndex(BrowseIndex.getPrivateBrowseIndex().getTableName(), item.getID(), sortMap)) {
                    dao.deleteByItemID(BrowseIndex.getItemBrowseIndex().getTableName(), item.getID());
                    dao.insertIndex(BrowseIndex.getPrivateBrowseIndex().getTableName(), item.getID(), sortMap);
                }
            }
            else if (item.isWithdrawn())
            {
                // Try to update an existing record in the withdrawn index
                if (!dao.updateIndex(BrowseIndex.getWithdrawnBrowseIndex().getTableName(), item.getID(), sortMap))
                {
                    // Record doesn't exist - ensure that it doesn't exist in the item index,
                    // and add it to the withdrawn item index
                    dao.deleteByItemID(BrowseIndex.getItemBrowseIndex().getTableName(), item.getID());
                    dao.insertIndex(BrowseIndex.getWithdrawnBrowseIndex().getTableName(), item.getID(), sortMap);
                }
            }
            else
            {
                // This item shouldn't exist in either index - ensure that it is removed
                dao.deleteByItemID(BrowseIndex.getItemBrowseIndex().getTableName(), item.getID());
                dao.deleteByItemID(BrowseIndex.getWithdrawnBrowseIndex().getTableName(), item.getID());
                dao.deleteByItemID(BrowseIndex.getPrivateBrowseIndex().getTableName(), item.getID());
            }

            // Update the community mappings if they are required, or remove them if they aren't
            if (reqCommunityMappings)
            {
                dao.updateCommunityMappings(item.getID());
            }
            else
            {
                dao.deleteCommunityMappings(item.getID());
            }

            // Now update the metadata indexes
            for (int i = 0; i < bis.length; i++)
            {
                if (bis[i].isMetadataIndex())
                {
                    log.debug("Indexing for item " + item.getID() + ", for index: " + bis[i].getTableName());
                    Set<Integer> distIDSet = new HashSet<Integer>();

                    // now index the new details - but only if it's archived and not withdrawn
                    if (item.isArchived() && !item.isWithdrawn())
                    {
                        // get the metadata from the item
                        for (int mdIdx = 0; mdIdx < bis[i].getMetadataCount(); mdIdx++)
                        {
                            String[] md = bis[i].getMdBits(mdIdx);
                            DCValue[] values = item.getMetadata(md[0], md[1], md[2], Item.ANY);

                            // if we have values to index on, then do so
                            if (values != null && values.length > 0)
                            {
                                int minConfidence = MetadataAuthorityManager.getManager()
                                        .getMinConfidence(values[0].schema, values[0].element, values[0].qualifier);

                                for (DCValue value : values)
                                {
                                    // Ensure that there is a value to index before inserting it
                                    if (StringUtils.isEmpty(value.value))
                                    {
                                        log.error("Null metadata value for item " + item.getID() + ", field: " +
                                                value.schema + "." +
                                                value.element +
                                                (value.qualifier == null ? "" : "." + value.qualifier));
                                    }
                                    else
                                    {
                                        if (bis[i].isAuthorityIndex() &&
                                                (value.authority == null || value.confidence < minConfidence))
                                        {
                                            // skip to next value in this authority field if value is not authoritative
                                            log.debug("Skipping non-authoritative value: " + item.getID() + ", field=" + value.schema + "." + value.element + "." + value.qualifier + ", value=" + value.value + ", authority=" + value.authority + ", confidence=" + value.confidence + " (BAD AUTHORITY)");
                                            continue;

                                        }

                                        // is there any valid (with appropriate confidence) authority key?
                                        if (value.authority != null
                                                && value.confidence >= minConfidence)
                                        {
                                            boolean isValueInVariants = false;

                                            // Are there variants of this value
                                            List<String> variants = ChoiceAuthorityManager.getManager()
                                                    .getVariants(value.schema, value.element, value.qualifier,
                                                            value.authority, value.language);

                                            // If we have variants, index them
                                            if (variants != null)
                                            {
                                                for (String var : variants)
                                                {
                                                    String nVal = OrderFormat.makeSortString(var, value.language, bis[i].getDataType());
                                                    distIDSet.add(dao.getDistinctID(bis[i].getDistinctTableName(), var, value.authority, nVal));
                                                    if (var.equals(value.value))
                                                    {
                                                        isValueInVariants = true;
                                                    }
                                                }
                                            }

                                            // If we didn't index the value as one of the variants, add it now
                                            if (!isValueInVariants)
                                            {
                                                // get the normalised version of the value
                                                String nVal = OrderFormat.makeSortString(value.value, value.language, bis[i].getDataType());
                                                distIDSet.add(dao.getDistinctID(bis[i].getDistinctTableName(), value.value, value.authority, nVal));
                                            }
                                        }
                                        else // put it in the browse index as if it hasn't have an authority key
                                        {
                                            // get the normalised version of the value
                                            String nVal = OrderFormat.makeSortString(value.value, value.language, bis[i].getDataType());
                                            distIDSet.add(dao.getDistinctID(bis[i].getDistinctTableName(), value.value, null, nVal));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Do we have any mappings?
                    if (distIDSet.isEmpty())
                    {
                        if (!addingNewItem)
                        {
                            // remove any old mappings
                            List<Integer> distinctIds = dao.deleteMappingsByItemID(bis[i].getMapTableName(), item.getID());
                            if (distinctIds != null && distinctIds.size() > 0)
                            {
                                dao.pruneDistinct(bis[i].getDistinctTableName(), bis[i].getMapTableName(), distinctIds);
                            }
                        }
                    }
                    else
                    {
                        // Update the existing mappings
                        MappingResults results = dao.updateDistinctMappings(bis[i].getMapTableName(), item.getID(), distIDSet);
                        if (results.getRemovedDistinctIds() != null && results.getRemovedDistinctIds().size() > 0)
                        {
                            pruneDistinctIndex(bis[i], results.getRemovedDistinctIds());
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
        try
        {
            // now obtain the sort order values that we will use
            Map<Integer, String> sortMap = new HashMap<Integer, String>();
            for (SortOption so : SortOption.getSortOptions())
            {
                Integer key = Integer.valueOf(so.getNumber());
                String metadata = so.getMetadata();

                // If we've already used the metadata for this Item
                // it will be cached in the map
                DCValue value = null;

                if (itemMDMap != null)
                {
                    value = (DCValue) itemMDMap.get(metadata);
                }

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
                        {
                            itemMDMap.put(metadata, dcv[0]);
                        }
                    }
                }

                // normalise the values as we insert into the sort map
                if (value != null && value.value != null)
                {
                    String nValue = OrderFormat.makeSortString(value.value, value.language, so.getType());
                    sortMap.put(key, nValue);
                } else {
                	// Add an empty entry to clear out any old values in the sort columns.
                	sortMap.put(key, null);
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
	 * remove all the indices for the given item
	 * 
	 * @param item		the item to be removed
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
    			dao.deleteByItemID(bis[i].getMapTableName(), itemID);
		    }
	    }

        // Remove from the item indexes (archive and withdrawn)
        dao.deleteByItemID(BrowseIndex.getItemBrowseIndex().getTableName(), itemID);
        dao.deleteByItemID(BrowseIndex.getWithdrawnBrowseIndex().getTableName(), itemID);
        dao.deleteByItemID(BrowseIndex.getPrivateBrowseIndex().getTableName(), itemID);
        dao.deleteCommunityMappings(itemID);

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
        Date startTime = new Date();
        try
        {
            Context context = new Context();
            context.turnOffAuthorisationSystem();
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
        finally
        {
            Date endTime = new Date();
            System.out.println("Started: " + startTime.getTime());
            System.out.println("Ended: " + endTime.getTime());
            System.out.println("Elapsed time: " + ((endTime.getTime() - startTime.getTime()) / 1000) + " secs (" + (endTime.getTime() - startTime.getTime()) + " msecs)");

        }
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
                StringBuilder logMe = new StringBuilder();
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
    			if (dao.testTableExistence(tableName))
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
                if (!dao.testTableExistence(distinctTableName))
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
            dropItemTables(BrowseIndex.getPrivateBrowseIndex());
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
        if (dao.testTableExistence(bix.getTableName()))
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
                sortCols.add(Integer.valueOf(so.getNumber()));
            }

            createItemTables(BrowseIndex.getItemBrowseIndex(), sortCols);
            createItemTables(BrowseIndex.getWithdrawnBrowseIndex(), sortCols);
            createItemTables(BrowseIndex.getPrivateBrowseIndex(), sortCols);
            
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
		Date localStart = new Date();
		
		output.message("Creating browse indexes for DSpace");
		
	    Date initDate = new Date();
	    long init = initDate.getTime() - localStart.getTime();
	    
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
	    long prep = prepDate.getTime() - localStart.getTime();
	    long prepinit = prepDate.getTime() - initDate.getTime();
	    
	    output.message("tables prepped (" + Long.toString(prep) + " ms, " + Long.toString(prepinit) + " ms)");
	    
	    int count = createIndex();
	    
	    context.complete();
	    
	    Date endDate = new Date();
	    long end = endDate.getTime() - localStart.getTime();
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
                // Creating the indexes from scracth, so treat each item as if it's new
                indexItem(new ItemMetadataProxy(items[j].getID(), items[j]), true);
    			
    			// after each item we commit the context and clear the cache
    			context.commit();
    			context.clearCache();
    		}
            
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
	private static class ItemMetadataProxy
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
        
        /**
         * Is the Item discoverable?
         * @return
         */
        public boolean isDiscoverable()
        {
            if (item != null)
            {
            	return item.isDiscoverable();
            }
            
            return browseItem.isDiscoverable();
        }
        
        
	}
}
