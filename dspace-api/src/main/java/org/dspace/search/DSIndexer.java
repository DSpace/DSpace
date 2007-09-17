/*
 * DSIndexer.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.search;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;

/**
 * DSIndexer contains the methods that index Items and their metadata,
 * collections, communities, etc. It is meant to either be invoked from the
 * command line (see dspace/bin/index-all) or via the indexContent() methods
 * within DSpace.
 * 
 * As of 1.4.2 this class has new incremental update of index functionality
 * and better detection of locked state thanks to Lucene 2.1 moving write.lock.
 * It will attempt to attain a lock on the index in the event that an update
 * is requested and will wait a maximum of 30 seconds (a worst case scenario)
 * to attain the lock before giving up and logging the failure to log4j and 
 * to the DSpace administrator email account. 
 * 
 * The Administrator can choose to run DSIndexer in a cron that
 * repeats regularly, a failed attempt to index from the UI will be "caught" up
 * on in that cron.
 * 
 * @author Mark Diggory
 * @author Graham Triggs
 */
public class DSIndexer
{
    private static final Logger log = Logger.getLogger(DSIndexer.class);

    private static final String LAST_INDEXED_FIELD = "DSIndexer.lastIndexed";
    
    private static final long WRITE_LOCK_TIMEOUT = 30000 /* 30 sec */;
    
    // Class to hold the index configuration (one instance per config line)
    private static class IndexConfig
    {
        String indexName;
        String schema;
        String element;
        String qualifier = null;
    }
    
    private static String index_directory = ConfigurationManager.getProperty("search.dir");
    
    private static int maxfieldlength = -1;
    	
    // TODO: Support for analyzers per language, or multiple indices
    /** The analyzer for this DSpace instance */
    private static Analyzer analyzer = null;

    // Static initialisation of index configuration /
    private static IndexConfig[] indexConfigArr = new IndexConfig[0];
    
    static {
    	
    	// calculate maxfieldlength
    	if (ConfigurationManager.getProperty("search.maxfieldlength") != null)
        {
            maxfieldlength = ConfigurationManager.getIntProperty("search.maxfieldlength");
        }
    	
        // read in indexes from the config
        ArrayList indexConfigList = new ArrayList();

        // read in search.index.1, search.index.2....
        for (int i = 1; ConfigurationManager.getProperty("search.index." + i) != null; i++)
        {
            indexConfigList.add(ConfigurationManager.getProperty("search.index." + i));
        }
    
        if (indexConfigList.size() > 0)
        {
            indexConfigArr = new IndexConfig[indexConfigList.size()];
            
            for (int i = 0; i < indexConfigList.size(); i++)
            {
                indexConfigArr[i] = new IndexConfig();
                String index = (String) indexConfigList.get(i);
    
                String[] configLine = index.split(":");
                
                indexConfigArr[i].indexName = configLine[0];
    
                // Get the schema, element and qualifier for the index
                // TODO: Should check valid schema, element, qualifier?
                String[] parts = configLine[1].split("\\.");
    
                switch (parts.length)
                {
                case 3:
                    indexConfigArr[i].qualifier = parts[2];
                case 2:
                    indexConfigArr[i].schema  = parts[0];
                    indexConfigArr[i].element = parts[1];
                    break;
                default:
                    log.warn("Malformed configuration line: search.index." + i);
                    // FIXME: Can't proceed here, no suitable exception to throw
                    throw new RuntimeException(
                            "Malformed configuration line: search.index." + i);
                }
            }
        }
        
        /*
         * Increase the default write lock so that Indexing can be interupted.
         */
        IndexWriter.setDefaultWriteLockTimeout(WRITE_LOCK_TIMEOUT);
    
        /*
         * Create the index directory if it doesn't already exist.
         */
        if(!IndexReader.indexExists(index_directory))
    	{
    		try {
    			new File(index_directory).mkdirs();
				openIndex(null,true).close();
			} catch (IOException e) {
                throw new RuntimeException(
                        "Could not create search index: " + e.getMessage(),e);
               }
    	}
    }

    /**
     * If the handle for the "dso" already exists in the index, and
     * the "dso" has a lastModified timestamp that is newer than 
     * the document in the index then it is updated, otherwise a 
     * new document is added.
     * 
     * @param context Users Context
     * @param dso DSpace Object (Item, Collection or Community
     * @throws SQLException
     * @throws IOException
     */
    public static void indexContent(Context context, DSpaceObject dso)
    throws SQLException, IOException
    {
    	indexContent(context, dso, false);
    }
    /**
     * If the handle for the "dso" already exists in the index, and
     * the "dso" has a lastModified timestamp that is newer than 
     * the document in the index then it is updated, otherwise a 
     * new document is added.
     * 
     * @param context Users Context
     * @param dso DSpace Object (Item, Collection or Community
     * @param force Force update even if not stale.
     * @throws SQLException
     * @throws IOException
     */
    public static void indexContent(Context context, DSpaceObject dso, boolean force)
    throws SQLException, IOException
    {
    	
    	String handle = dso.getHandle();
    	
    	if(handle == null)
    	{
    		handle = HandleManager.findHandle(context, dso);   
    	}

		Term t = new Term("handle", handle);
		
        IndexWriter writer = null;
        
        try
        {
            switch (dso.getType())
            {
            case Constants.ITEM :
                Item item = (Item)dso;
                if (item.isArchived() && !item.isWithdrawn())
                {
                    if(requiresIndexing(handle, ((Item)dso).getLastModified()) || force)
                    {
                        Document doc = buildDocument(context, (Item) dso);

                        /* open inside stale block, after building doc
                         * to limit the total time spent in a lock.
                         */
                        writer = openIndex(context, false);
                        writer.updateDocument(t, doc);

                        log.info("Wrote Item: " + handle + " to Index");
                    }
                }
                
                break;
                
            case Constants.COLLECTION :
            	writer = openIndex(context, false);
            	writer.updateDocument(t, buildDocument(context, (Collection) dso));
            	log.info("Wrote Collection: " + handle + " to Index");
            	break;

            case Constants.COMMUNITY :
            	writer = openIndex(context, false);
            	writer.updateDocument(t, buildDocument(context, (Community) dso));
                log.info("Wrote Community: " + handle + " to Index");
                break;
                
            default :
                log.error("Only Items, Collections and Communities can be Indexed");
            }

        } catch (Exception e) {
			log.error(e.getMessage(), e);
		}
        finally
        {
        	/* drop the lock */
            if(writer != null)
            	writer.close();
        }
    }

    /**
     * unIndex removes an Item, Collection, or Community only works if the
     * DSpaceObject has a handle (uses the handle for its unique ID)
     * 
     * @param context
     * @param dso DSpace Object, can be Community, Item, or Collection
     * @throws SQLException
     * @throws IOException
     */
    public static void unIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException
    {
        try
        {
        	unIndexContent(context, dso.getHandle());
        }
        catch(Exception exception)
        {
            log.error(exception.getMessage(),exception);
            emailException(exception);
        }
    }

    /**
     * Unindex a Docment in the Lucene Index.
     * 
     * @param context
     * @param handle 
     * @throws SQLException
     * @throws IOException
     */
    public static void unIndexContent(Context context, String handle)
            throws SQLException, IOException
    {
        
        IndexWriter writer = openIndex(context, false);
        
        try
        {
            if (handle != null)
            {
                // we have a handle (our unique ID, so remove)
                Term t = new Term("handle", handle);
                writer.deleteDocuments(t);
            }
            else
            {
                log.warn("unindex of content with null handle attempted");

                // FIXME: no handle, fail quietly - should log failure
                //System.out.println("Error in unIndexContent: Object had no
                // handle!");
            }
        }
        finally
        {
            writer.close();
        }
    }
    
    

    /**
     * reIndexContent removes something from the index, then re-indexes it
     *
     * @param context context object
     * @param dso  object to re-index
     */
    public static void reIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException
    {
        try
        {
        	indexContent(context, dso);
        }
        catch(Exception exception)
        {
            log.error(exception.getMessage(),exception);
            emailException(exception);
        }
    }
    
    /**
	 * create full index - wiping old index
	 * 
	 * @param c context to use
	 */
    public static void createIndex(Context c) throws SQLException, IOException
    {

    	/* Create a new index, blowing away the old. */
        openIndex(c, true).close();
        
        /* Reindex all content preemptively. */
        DSIndexer.updateIndex(c, true);

    }
    
    /**
     * Optimize the existing index. Iimportant to do regularly to reduce 
     * filehandle usage and keep performance fast!
     * 
     * @param c Users Context
     * @throws SQLException
     * @throws IOException
     */
    public static void optimizeIndex(Context c) throws SQLException, IOException
    {
        IndexWriter writer = openIndex(c, false);

        try
        {
            writer.optimize();
        }
        finally
        {
            writer.close();
        }
    }

    /**
     * When invoked as a command-line tool, creates, updates, removes 
     * content from the whole index
     *
     * @param args
     *            the command-line arguments, none used
     * @throws IOException 
     * @throws SQLException 
     */
    public static void main(String[] args) throws SQLException, IOException
    {
    	
        Context context = new Context();
        context.setIgnoreAuthorization(true);
        
        String usage = "org.dspace.search.DSIndexer [-cbhouf[d <item handle>]] or nothing to update/clean an existing index.";
        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line = null;

        options.addOption(OptionBuilder
                        .withArgName("item handle")
                        .hasArg(true)
                        .withDescription(
                                "delete an Item, Collection or Community from index based on its handle")
                        .create("d"));

        options.addOption(OptionBuilder.isRequired(false).withDescription(
                "optimize existing index").create("o"));

        options.addOption(OptionBuilder
                        .isRequired(false)
                        .withDescription(
                                "clean existing index removing any documents that no longer exist in the db")
                        .create("c"));

        options.addOption(OptionBuilder.isRequired(false).withDescription(
                "(re)build index, wiping out current one if it exists").create(
                "b"));

        options.addOption(OptionBuilder
                        .isRequired(false)
                        .withDescription(
                                "if updating existing index, force each handle to be reindexed even if uptodate")
                        .create("f"));

        options.addOption(OptionBuilder.isRequired(false).withDescription(
                "print this help message").create("h"));

        try
        {
            line = new PosixParser().parse(options, args);
        }
        catch (Exception e)
        {
            // automatically generate the help statement
            formatter.printHelp(usage, e.getMessage(), options, "");
            System.exit(1);
        }

        if (line.hasOption("h"))
        {
            // automatically generate the help statement
            formatter.printHelp(usage, options);
            System.exit(1);
        }

        if (line.hasOption("r"))
        {
            log.info("Removing " + line.getOptionValue("r") + " from Index");
            unIndexContent(context, line.getOptionValue("r"));
        }
        else if (line.hasOption("o"))
        {
            log.info("Optimizing Index");
            optimizeIndex(context);
        }
        else if (line.hasOption("c"))
        {
            log.info("Cleaning Index");
            cleanIndex(context);
        }
        else if (line.hasOption("u"))
        {
            log.info("Updating Index");
            updateIndex(context, line.hasOption("f"));
        }
        else if (line.hasOption("b"))
        {
            log.info("(Re)building index from scratch.");
            createIndex(context);
        }
        else
        {
            log.info("Updating and Cleaning Index");
            cleanIndex(context);
            updateIndex(context, line.hasOption("f"));
        }

        log.info("Done with indexing");
    }

    /**
     * Iterates over all Items, Collections and Communities. And updates
     * them in the index. Uses decaching to control memory footprint.
     * Uses indexContent and isStale ot check state of item in index.
     * 
     * @param context
     */
    public static void updateIndex(Context context) {
    	updateIndex(context,false);
    }
    
    /**
     * Iterates over all Items, Collections and Communities. And updates
     * them in the index. Uses decaching to control memory footprint.
     * Uses indexContent and isStale ot check state of item in index.
     * 
     * At first it may appear counterintuitive to have an IndexWriter/Reader
     * opened and closed on each DSO. But this allows the UI processes
     * to step in and attain a lock and write to the index even if other
     * processes/jvms are running a reindex.
     * 
     * @param context
     * @param force 
     */
    public static void updateIndex(Context context, boolean force) {

    		try
    		{
    			
    			for(ItemIterator i = Item.findAll(context);i.hasNext();)
    	        {
    	            Item item = (Item) i.next();
    	            indexContent(context,item,force);
    	            item.decache();
    	        }
    			
    			Collection[] collections = Collection.findAll(context);
    	        for (int i = 0; i < collections.length; i++)
    	        {
    	            indexContent(context,collections[i],force);
    	            context.removeCached(collections[i], collections[i].getID());
    	            
    	        }
    		
    			Community[] communities = Community.findAll(context);
    	        for (int i = 0; i < communities.length; i++)
    	        {
    	            indexContent(context,communities[i],force);
    	            context.removeCached(communities[i], communities[i].getID());
    	        }
    			
    	        optimizeIndex(context);
    			
    		}
    		catch(Exception e)
    		{
    			log.error(e.getMessage(), e);
    		}

	}
    
    /**
     * Iterates over all documents in the Lucene index and verifies they 
     * are in database, if not, they are removed.
     * 
     * @param context
     * @throws IOException 
     * @throws SQLException 
     */
    public static void cleanIndex(Context context) throws IOException, SQLException {

    	IndexReader reader = DSQuery.getIndexReader();
    	
    	for(int i = 0 ; i < reader.numDocs(); i++)
    	{
    		if(!reader.isDeleted(i))
    		{
    			Document doc = reader.document(i);
        		String handle = doc.get("handle");
        		
        		DSpaceObject o = HandleManager.resolveToObject(context, handle);

                if (o == null)
                {
                	log.info("Deleting: " + handle);
                	/* Use IndexWriter to delete, its easier to manage write.lock */
                	DSIndexer.unIndexContent(context, handle);
                }
                else
                {
                	context.removeCached(o, o.getID());
                	log.debug("Keeping: " + handle);
                }
    		}
    		else
    		{
    			log.debug("Encountered deleted doc: " + i);
    		}
    	}
	}
    
	/**
     * Get the Lucene analyzer to use according to current configuration (or
     * default). TODO: Should have multiple analyzers (and maybe indices?) for
     * multi-lingual DSpaces.
     *
     * @return <code>Analyzer</code> to use
     * @throws IllegalStateException
     *             if the configured analyzer can't be instantiated
     */
    static Analyzer getAnalyzer() throws IllegalStateException
    {
        if (analyzer == null)
        {
            // We need to find the analyzer class from the configuration
            String analyzerClassName = ConfigurationManager.getProperty("search.analyzer");

            if (analyzerClassName == null)
            {
                // Use default
                analyzerClassName = "org.dspace.search.DSAnalyzer";
            }

            try
            {
                Class analyzerClass = Class.forName(analyzerClassName);
                analyzer = (Analyzer) analyzerClass.newInstance();
            }
            catch (Exception e)
            {
                log.fatal(LogManager.getHeader(null, "no_search_analyzer",
                        "search.analyzer=" + analyzerClassName), e);

                throw new IllegalStateException(e.toString());
            }
        }

        return analyzer;
    }

    
    ////////////////////////////////////
    //      Private
    ////////////////////////////////////

    private static void emailException(Exception exception) {
		// Also email an alert, system admin may need to check for stale lock
		try {
			String recipient = ConfigurationManager
					.getProperty("alert.recipient");

			if (recipient != null) {
				Email email = ConfigurationManager.getEmail("internal_error");
				email.addRecipient(recipient);
				email.addArgument(ConfigurationManager
						.getProperty("dspace.url"));
				email.addArgument(new Date());

				String stackTrace;

				if (exception != null) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					exception.printStackTrace(pw);
					pw.flush();
					stackTrace = sw.toString();
				} else {
					stackTrace = "No exception";
				}

				email.addArgument(stackTrace);
				email.send();
			}
		} catch (Exception e) {
			// Not much we can do here!
			log.warn("Unable to send email alert", e);
		}

	}
    
    /**
	 * Is stale checks the lastModified time stamp in the database and the index
	 * to determine if the index is stale.
	 * 
	 * @param handle
	 * @param dso
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
    private static boolean requiresIndexing(String handle, Date lastModified)
    throws SQLException, IOException
    {
		
		boolean reindexItem = false;
		boolean inIndex = false;
		
		IndexReader ir = DSQuery.getIndexReader();
		
		Term t = new Term("handle", handle);
		TermDocs docs = ir.termDocs(t);
						
		while(docs.next())
		{
			inIndex = true;
			int id = docs.doc();
			Document doc = ir.document(id);

			Field lastIndexed = doc.getField(LAST_INDEXED_FIELD);

			if (lastIndexed == null || Long.parseLong(lastIndexed.stringValue()) < 
					lastModified.getTime()) {
				reindexItem = true;
			}
		}

		return reindexItem || !inIndex;
	}

    /**
     * prepare index, opening writer, and wiping out existing index if necessary
     */
    private static IndexWriter openIndex(Context c, boolean wipe_existing)
            throws IOException
    {
    	
    	IndexWriter writer = new IndexWriter(index_directory, getAnalyzer(), wipe_existing);

        /* Set maximum number of terms to index if present in dspace.cfg */
        if (maxfieldlength == -1)
        {
            writer.setMaxFieldLength(Integer.MAX_VALUE);
        }
        else
        {
            writer.setMaxFieldLength(maxfieldlength);
        }
        
        return writer;
    }

    /**
     * 
     * @param c
     * @param myitem
     * @return
     * @throws SQLException
     */
    private static String buildItemLocationString(Context c, Item myitem)
            throws SQLException
    {
        // build list of community ids
        Community[] communities = myitem.getCommunities();

        // build list of collection ids
        Collection[] collections = myitem.getCollections();

        // now put those into strings
        String location = "";
        int i = 0;

        for (i = 0; i < communities.length; i++)
            location = new String(location + " m" + communities[i].getID());

        for (i = 0; i < collections.length; i++)
            location = new String(location + " l" + collections[i].getID());

        return location;
    }

    private static String buildCollectionLocationString(Context c,
            Collection target) throws SQLException
    {
        // build list of community ids
        Community[] communities = target.getCommunities();

        // now put those into strings
        String location = "";
        int i = 0;

        for (i = 0; i < communities.length; i++)
            location = new String(location + " m" + communities[i].getID());

        return location;
    }

    /**
     * Build a Lucene document for a DSpace Community.
     * 
     * @param context Users Context
     * @param community Community to be indexed
     * @return
     * @throws SQLException
     * @throws IOException
     */
    private static Document buildDocument(Context context, Community community) 
    throws SQLException, IOException
    {
        // Create Lucene Document
        Document doc = buildDocument(Constants.COMMUNITY, community.getHandle(), null);

        // and populate it
        String name = community.getMetadata("name");
        
        if(name != null)
        {
        	doc.add(new Field("name", name, Field.Store.YES, Field.Index.TOKENIZED));
        	doc.add(new Field("default", name, Field.Store.YES, Field.Index.TOKENIZED));
        }
        
        return doc;
    }

    /**
     * Build a Lucene document for a DSpace Collection.
     * 
     * @param context Users Context
     * @param collection Collection to be indexed
     * @return
     * @throws SQLException
     * @throws IOException
     */
    private static Document buildDocument(Context context, Collection collection) 
    throws SQLException, IOException
    {
        String location_text = buildCollectionLocationString(context, collection);

        // Create Lucene Document
        Document doc = buildDocument(Constants.COLLECTION, collection.getHandle(), location_text);

        // and populate it
        String name = collection.getMetadata("name");
        
        if(name != null)
        {
        	doc.add(new Field("name", name, Field.Store.YES, Field.Index.TOKENIZED));
        	doc.add(new Field("default", name, Field.Store.YES, Field.Index.TOKENIZED));
        }
        
        return doc;
        
    }

    /**
     * Build a Lucene document for a DSpace Item.
     * 
     * @param context Users Context
     * @param item The DSpace Item to be indexed
     * @return
     * @throws SQLException
     * @throws IOException
     */
    private static Document buildDocument(Context context, Item item) 
    throws SQLException, IOException
    {
    
    	String handle = item.getHandle();
    	
    	if(handle == null)
    	{
    		handle = HandleManager.findHandle(context, item);   
    	}
    	
    	// get the location string (for searching by collection & community)
        String location = buildItemLocationString(context, item);

        Document doc = buildDocument(Constants.ITEM, handle, location);

        log.debug("Building Item: " + handle);

        int j;
        int k = 0;

        if (indexConfigArr.length > 0)
        {
            ArrayList fields = new ArrayList();
            ArrayList content = new ArrayList();
            DCValue[] mydc;

            for (int i = 0; i < indexConfigArr.length; i++)
            {
                // extract metadata (ANY is wildcard from Item class)
                if (indexConfigArr[i].qualifier!= null && indexConfigArr[i].qualifier.equals("*"))
                {
                    mydc = item.getMetadata(indexConfigArr[i].schema, indexConfigArr[i].element, Item.ANY, Item.ANY);
                }
                else
                {
                    mydc = item.getMetadata(indexConfigArr[i].schema, indexConfigArr[i].element, indexConfigArr[i].qualifier, Item.ANY);
                }

                // put them all from an array of strings to one string for
                // writing out pack all of the arrays of DCValues into plain
                // text strings for the indexer
                String content_text = "";

                for (j = 0; j < mydc.length; j++)
                {
                    content_text = new String(content_text + mydc[j].value + " ");
                }

                // arranges content with fields in ArrayLists with same index to
                // put
                // into hash later
                k = fields.indexOf(indexConfigArr[i].indexName);

                if (k < 0)
                {
                    fields.add(indexConfigArr[i].indexName);
                    content.add(content_text);
                }
                else
                {
                    content_text = new String(content_text
                            + (String) content.get(k) + " ");
                    content.set(k, content_text);
                }
            }

            // build the hash
            for (int i = 0; i < fields.size(); i++)
            {
            
            	doc.add(
            		new Field(
                                    (String) fields.get(i),
            			(String) content.get(i),
            			Field.Store.YES, Field.Index.TOKENIZED
                    ));
    
            	doc.add(new Field("default", (String) content.get(i), Field.Store.YES, Field.Index.TOKENIZED));
            }
        }
        else
        // if no search indexes found in cfg file, for backward compatibility
        {
            // extract metadata (ANY is wildcard from Item class)
            DCValue[] authors = item.getDC("contributor", Item.ANY, Item.ANY);
            for (j = 0; j < authors.length; j++)
            {
            	doc.add(new Field("author", authors[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            	doc.add(new Field("default", authors[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            }

            DCValue[] creators = item.getDC("creator", Item.ANY, Item.ANY);
            for (j = 0; j < creators.length; j++) //also authors
            {
                doc.add(new Field("author", creators[j].value, Field.Store.YES, Field.Index.TOKENIZED));
                doc.add(new Field("default", creators[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            }

            DCValue[] sors = item.getDC("description", "statementofresponsibility", Item.ANY);
            for (j = 0; j < sors.length; j++) //also authors
            {
                doc.add(new Field("author", sors[j].value, Field.Store.YES, Field.Index.TOKENIZED));
                doc.add(new Field("default", sors[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            }

            DCValue[] titles = item.getDC("title", Item.ANY, Item.ANY);
            for (j = 0; j < titles.length; j++)
            {
            	doc.add(new Field("title", titles[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            	doc.add(new Field("default", titles[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            }

            DCValue[] keywords = item.getDC("subject", Item.ANY, Item.ANY);
            for (j = 0; j < keywords.length; j++)
            {
            	doc.add(new Field("keyword", keywords[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            	doc.add(new Field("default", keywords[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            }

            DCValue[] abstracts = item.getDC("description", "abstract", Item.ANY);
            for (j = 0; j < abstracts.length; j++)
            {
            	doc.add(new Field("abstract", abstracts[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            	doc.add(new Field("default", abstracts[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            }

            DCValue[] tocs = item.getDC("description", "tableofcontents", Item.ANY);
            for (j = 0; j < tocs.length; j++)
            {
            	doc.add(new Field("abstract", tocs[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            	doc.add(new Field("default", tocs[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            }

            DCValue[] series = item.getDC("relation", "ispartofseries", Item.ANY);
            for (j = 0; j < series.length; j++)
            {
            	doc.add(new Field("series", series[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            	doc.add(new Field("default", series[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            }

            DCValue[] mimetypes = item.getDC("format", "mimetype", Item.ANY);
            for (j = 0; j < mimetypes.length; j++)
            {
            	doc.add(new Field("mimetype", mimetypes[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            	doc.add(new Field("default", mimetypes[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            }

            DCValue[] sponsors = item.getDC("description", "sponsorship", Item.ANY);
            for (j = 0; j < sponsors.length; j++)
            {
            	doc.add(new Field("sponsor", sponsors[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            	doc.add(new Field("default", sponsors[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            }

            DCValue[] identifiers = item.getDC("identifier", Item.ANY, Item.ANY);
            for (j = 0; j < identifiers.length; j++)
            {
            	doc.add(new Field("identifier", identifiers[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            	doc.add(new Field("default", identifiers[j].value, Field.Store.YES, Field.Index.TOKENIZED));
            }
        }

        log.debug("  Added Metadata");

        try
        {
        	// now get full text of any bitstreams in the TEXT bundle
            // trundle through the bundles
            Bundle[] myBundles = item.getBundles();

            for (int i = 0; i < myBundles.length; i++)
            {
                if ((myBundles[i].getName() != null)
                        && myBundles[i].getName().equals("TEXT"))
                {
                    // a-ha! grab the text out of the bitstreams
                    Bitstream[] myBitstreams = myBundles[i].getBitstreams();

                    for (j = 0; j < myBitstreams.length; j++)
                    {
                        try
                        {
                            InputStreamReader is = new InputStreamReader(
                                    myBitstreams[j].retrieve()); // get input

                            // Add each InputStream to the Indexed Document (Acts like an Append)
                            doc.add(new Field("default", is));

                            log.debug("  Added BitStream: " + myBitstreams[j].getStoreNumber() + "	" + myBitstreams[j].getSequenceID() + "   " + myBitstreams[j].getName());

                        }
                        catch (Exception e)
                        {
                            // this will never happen, but compiler is now happy.
                        	log.error(e.getMessage(),e);
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
        	log.error(e.getMessage(),e);
        }


        return doc;
    }

    /**
     * Create Lucene document with all the shared fields initialized.
     * 
     * @param type Type of DSpace Object
     * @param handle
     * @param location
     * @return
     */
    private static Document buildDocument(int type, String handle, String location)
    {
        Document doc = new Document();

        // want to be able to check when last updated
        // (not tokenized, but it is indexed)
        doc.add(new Field(LAST_INDEXED_FIELD, Long.toString(System.currentTimeMillis()), Field.Store.YES, Field.Index.UN_TOKENIZED));

        
        // do location, type, handle first
        doc.add(new Field("type", Integer.toString(type), Field.Store.YES, Field.Index.NO));

        // want to be able to search for handle, so use keyword
        // (not tokenized, but it is indexed)
        if (handle != null)
        {
            // ??? not sure what the "handletext" field is but it was there in writeItemIndex ???
            doc.add(new Field("handletext", handle, Field.Store.YES, Field.Index.TOKENIZED));

            // want to be able to search for handle, so use keyword
            // (not tokenized, but it is indexed)
            doc.add(new Field("handle", handle, Field.Store.YES, Field.Index.UN_TOKENIZED));

            // add to full text index
            doc.add(new Field("default", handle, Field.Store.YES, Field.Index.TOKENIZED));
        }

        if(location != null)
        {
            doc.add(new Field("location", location, Field.Store.YES, Field.Index.TOKENIZED));
    	    doc.add(new Field("default", location, Field.Store.YES, Field.Index.TOKENIZED));
        }

        return doc;
    }


}
