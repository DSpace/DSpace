/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.Util;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.sort.OrderFormat;
import org.dspace.sort.SortOption;

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
 * 
 * @deprecated Since DSpace 4 the system use an abstraction layer named
 *             Discovery to provide access to different search providers. The
 *             legacy system built upon Apache Lucene is likely to be removed in
 *             a future version. If you are interested in using Lucene as backend
 *             for the DSpace search system, please consider to build a Lucene
 *             implementation of the Discovery interfaces
 */
@Deprecated
public class DSIndexer
{
    private static final Logger log = Logger.getLogger(DSIndexer.class);

    private static final String LAST_INDEXED_FIELD    = "DSIndexer.lastIndexed";
    private static final String DOCUMENT_STATUS_FIELD = "DSIndexer.status";

    private static final long WRITE_LOCK_TIMEOUT = 30000 /* 30 sec */;

    private static Thread delayedIndexFlusher = null;
    private static int indexFlushDelay = ConfigurationManager.getIntProperty("search.index.delay", -1);

    private static int batchFlushAfterDocuments = ConfigurationManager.getIntProperty("search.batch.documents", 20);
    private static boolean batchProcessingMode = false;
    static final Version luceneVersion = Version.LATEST;

    // Class to hold the index configuration (one instance per config line)
    private static class IndexConfig
    {
        String indexName;
        String schema;
        String element;
        String qualifier = null;
        String type = "text";

        IndexConfig()
        {
        }

        IndexConfig(String indexName, String schema, String element, String qualifier, String type)
        {
            this.indexName = indexName;
            this.schema = schema;
            this.element = element;
            this.qualifier = qualifier;
            this.type = type;
        }
    }

    private static String indexDirectory = ConfigurationManager.getProperty("search.dir");

    private static int maxfieldlength = -1;

    // TODO: Support for analyzers per language, or multiple indices
    /** The analyzer for this DSpace instance */
    private static volatile Analyzer analyzer = null;

    /** Static initialisation of index configuration */
    /** Includes backwards compatible default configuration */
    private static IndexConfig[] indexConfigArr = new IndexConfig[]
    {
        new IndexConfig("author",     "dc", "contributor", Item.ANY,          "text") ,
        new IndexConfig("author",     "dc", "creator",     Item.ANY,          "text"),
        new IndexConfig("author",     "dc", "description", "statementofresponsibility", "text"),
        new IndexConfig("title",      "dc", "title",       Item.ANY,          "text"),
        new IndexConfig("keyword",    "dc", "subject",     Item.ANY,          "text"),
        new IndexConfig("abstract",   "dc", "description", "abstract",        "text"),
        new IndexConfig("abstract",   "dc", "description", "tableofcontents", "text"),
        new IndexConfig("series",     "dc", "relation",    "ispartofseries",  "text"),
        new IndexConfig("mimetype",   "dc", "format",      "mimetype",        "text"),
        new IndexConfig("sponsor",    "dc", "description", "sponsorship",     "text"),
        new IndexConfig("identifier", "dc", "identifier",  Item.ANY,          "text")
    };

    static {

    	// calculate maxfieldlength
    	if (ConfigurationManager.getProperty("search.maxfieldlength") != null)
        {
            maxfieldlength = ConfigurationManager.getIntProperty("search.maxfieldlength");
        }

        // read in indexes from the config
        ArrayList<String> indexConfigList = new ArrayList<String>();

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
                String index = indexConfigList.get(i);

                String[] configLine = index.split(":");

                indexConfigArr[i].indexName = configLine[0];

                // Get the schema, element and qualifier for the index
                // TODO: Should check valid schema, element, qualifier?
                String[] parts = configLine[1].split("\\.");

                switch (parts.length)
                {
                case 3:
                    indexConfigArr[i].qualifier = parts[2];
                    // Fall through for other parts of the array
                case 2:
                    indexConfigArr[i].schema  = parts[0];
                    indexConfigArr[i].element = parts[1];
                    break;
                default:
                    log.warn("Malformed configuration line: search.index." + i);
                    // FIXME: Can't proceed here, no suitable exception to throw
                    throw new IllegalStateException("Malformed configuration line: search.index." + i);
                }

                if (configLine.length > 2)
                {
                    indexConfigArr[i].type = configLine[2];
                }
            }
        }

        /*
         * Increase the default write lock so that Indexing can be interrupted.
         */
        IndexWriterConfig.setDefaultWriteLockTimeout(WRITE_LOCK_TIMEOUT);

        /*
         * Create the index directory if it doesn't already exist.
         */
        try
        {
            if (!DirectoryReader.indexExists(FSDirectory.open(new File(indexDirectory))))
            {

                if (!new File(indexDirectory).mkdirs())
                {
                    log.error("Unable to create index directory: " + indexDirectory);
                }
                openIndex(true).close();
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not create search index: " + e.getMessage(),e);
        }
    }

    public static void setBatchProcessingMode(boolean mode)
    {
        batchProcessingMode = mode;
        if (mode == false)
        {
            flushIndexingTaskQueue();
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
    public static void indexContent(Context context, DSpaceObject dso) throws SQLException, DCInputsReaderException
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
    public static void indexContent(Context context, DSpaceObject dso, boolean force) throws SQLException, DCInputsReaderException
    {
        try
        {
            IndexingTask task = prepareIndexingTask(context, dso, force);
            if (task != null)
            {
                processIndexingTask(task);
            }
        }
        catch (IOException e)
        {
            log.error(e);
        }
    }

    /**
     * unIndex removes an Item, Collection, or Community only works if the
     * DSpaceObject has a handle (uses the handle for its unique ID)
     *
     * @param context DSpace context
     * @param dso DSpace Object, can be Community, Item, or Collection
     * @throws SQLException
     * @throws IOException
     */
    public static void unIndexContent(Context context, DSpaceObject dso) throws SQLException, IOException
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
     * Unindex a Document in the Lucene Index.
     *
     * @param context
     * @param handle
     * @throws SQLException
     * @throws IOException
     */
    public static void unIndexContent(Context context, String handle) throws SQLException, IOException
    {
        if (handle != null)
        {
            IndexingTask task = new IndexingTask(IndexingTask.Action.DELETE, new Term("handle", handle), null);
            if (task != null)
            {
                processIndexingTask(task);
            }
        }
        else
        {
            log.warn("unindex of content with null handle attempted");

            // FIXME: no handle, fail quietly - should log failure
            //System.out.println("Error in unIndexContent: Object had no
            // handle!");
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
        openIndex(true).close();

        /* Reindex all content preemptively. */
        DSIndexer.updateIndex(c, true);
    }

    /**
     * Optimize the existing index. Important to do regularly to reduce
     * filehandle usage and keep performance fast!
     *
     * @param c Users Context
     * @throws SQLException
     * @throws IOException
     */
    public static void optimizeIndex(Context c) throws SQLException, IOException
    {
        IndexWriter writer = openIndex(false);

        try
        {
            flushIndexingTaskQueue(writer);
            //With lucene 4.0 this method has been deleted , as it is horribly inefficient and very
            //rarely justified. Lucene's multi-segment search performance has improved
            //over time, and the default TieredMergePolicy now targets segments with
            //deletions. For more info see http://blog.trifork.com/2011/11/21/simon-says-optimize-is-bad-for-you/
            //writer.optimize();
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
        Date startTime = new Date();
        try
        {
            setBatchProcessingMode(true);
            Context context = new Context();
            context.setIgnoreAuthorization(true);

            String usage = "org.dspace.search.DSIndexer [-cbhof[r <item handle>]] or nothing to update/clean an existing index.";
            Options options = new Options();
            HelpFormatter formatter = new HelpFormatter();
            CommandLine line = null;

            options.addOption(OptionBuilder
                            .withArgName("item handle")
                            .hasArg(true)
                            .withDescription(
                                    "remove an Item, Collection or Community from index based on its handle")
                            .create("r"));

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
        finally
        {
            setBatchProcessingMode(false);
            Date endTime = new Date();
            System.out.println("Started: " + startTime.getTime());
            System.out.println("Ended: " + endTime.getTime());
            System.out.println("Elapsed time: " + ((endTime.getTime() - startTime.getTime()) / 1000) + " secs (" + (endTime.getTime() - startTime.getTime()) + " msecs)");
        }
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
     * Uses indexContent and isStale to check state of item in index.
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
                ItemIterator items = null;
                try
                {
                    for(items = Item.findAll(context);items.hasNext();)
                    {
                        Item item = (Item) items.next();
                        indexContent(context, item);
                        item.decache();
                    }
                }
                finally
                {
                    if (items != null)
                    {
                        items.close();
                    }
                }

                for (Collection collection : Collection.findAll(context))
                {
                    indexContent(context, collection);
    	            context.removeCached(collection, collection.getID());
                }

                for (Community community : Community.findAll(context))
    	        {
                    indexContent(context, community);
    	            context.removeCached(community, community.getID());
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
    	
    	Bits liveDocs = MultiFields.getLiveDocs(reader);
    	  
    	for(int i = 0 ; i < reader.numDocs(); i++)
    	{
    	    if (!liveDocs.get(i))
    		{         
    	        // document is deleted...
    	        log.debug("Encountered deleted doc: " + i);
    		}
    	    else {
                Document doc = reader.document(i);
        		String handle = doc.get("handle");
                if (!StringUtils.isEmpty(handle))
                {
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
    static Analyzer getAnalyzer()
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
                Constructor constructor = analyzerClass.getDeclaredConstructor(Version.class);
                constructor.setAccessible(true);
                analyzer = (Analyzer) constructor.newInstance(luceneVersion);
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


    static IndexingTask prepareIndexingTask(Context context, DSpaceObject dso, boolean force) throws SQLException, IOException, DCInputsReaderException
    {
        String handle = HandleManager.findHandle(context, dso);
        
        // DATASHARE - start
        if(handle == null){
            log.error("No handle found for :" + dso.getID());
        }
        // DATASHARE - start
        
        Term term = new Term("handle", handle);
        IndexingTask action = null;
        switch (dso.getType())
        {
        case Constants.ITEM :
            Item item = (Item)dso;
            if (item.isArchived() && !item.isWithdrawn())
            {
                /** If the item is in the repository now, add it to the index*/
                if (requiresIndexing(term, ((Item)dso).getLastModified()) || force)
                {
                    log.info("Writing Item: " + handle + " to Index");
                    action = new IndexingTask(IndexingTask.Action.UPDATE, term, buildDocumentForItem(context, (Item)dso));
                }
            }
            else
            {
                action = new IndexingTask(IndexingTask.Action.DELETE, term, null);
            }
            break;

        case Constants.COLLECTION :
            log.info("Writing Collection: " + handle + " to Index");
            action = new IndexingTask(IndexingTask.Action.UPDATE, term, buildDocumentForCollection((Collection)dso));
            break;

        case Constants.COMMUNITY :
            log.info("Writing Community: " + handle + " to Index");
            action = new IndexingTask(IndexingTask.Action.UPDATE, term, buildDocumentForCommunity((Community)dso));
            break;

        default :
            log.error("Only Items, Collections and Communities can be Indexed");
        }
        return action;
    }

    static void processIndexingTask(IndexingTask task) throws IOException
    {
        if (batchProcessingMode)
        {
            addToIndexingTaskQueue(task);
        }
        else if (indexFlushDelay > 0)
        {
            addToIndexingTaskQueue(task);
            startDelayedIndexFlusher();
        }
        else
        {
            IndexWriter writer = null;
            try
            {
                writer = openIndex(false);
                executeIndexingTask(writer, task);
            }
            finally
            {
                if (task.getDocument() != null)
                {
                    closeAllReaders(task.getDocument());
                }

                if (writer != null)
                {
                    try
                    {
                        writer.close();
                    }
                    catch (IOException e)
                    {
                        log.error("Unable to close IndexWriter", e);
                    }
                }
            }
        }
    }

    private static void executeIndexingTask(IndexWriter writer, IndexingTask action) throws IOException
    {
        if (action != null)
        {
            if (action.isDelete())
            {
                if (action.getDocument() != null)
                {
                    writer.updateDocument(action.getTerm(), action.getDocument());
                }
                else
                {
                    writer.deleteDocuments(action.getTerm());
                }
            }
            else
            {
                writer.updateDocument(action.getTerm(), action.getDocument());
            }
        }
    }

    private static Map<String, IndexingTask> queuedTaskMap = new HashMap<String, IndexingTask>();

    static synchronized void addToIndexingTaskQueue(IndexingTask action)
    {
        if (action != null)
        {
            queuedTaskMap.put(action.getTerm().text(), action);
            if (queuedTaskMap.size() >= batchFlushAfterDocuments)
            {
                flushIndexingTaskQueue();
            }
        }
    }

    static void flushIndexingTaskQueue()
    {
        if (queuedTaskMap.size() > 0)
        {
            IndexWriter writer = null;

            try
            {
                writer = openIndex(false);
                flushIndexingTaskQueue(writer);
            }
            catch (IOException e)
            {
                log.error(e);
            }
            finally
            {
                if (writer != null)
                {
                    try
                    {
                        writer.close();
                    }
                    catch (IOException ex)
                    {
                        log.error(ex);
                    }
                }
            }
        }
    }

    private static synchronized void flushIndexingTaskQueue(IndexWriter writer)
    {
        for (IndexingTask action : queuedTaskMap.values())
        {
            try
            {
                executeIndexingTask(writer, action);
            }
            catch (IOException e)
            {
                log.error(e);
            }
            finally
            {
                if (action.getDocument() != null)
                {
                    closeAllReaders(action.getDocument());
                }
            }
        }

        queuedTaskMap.clear();

        // We've flushed, so we don't need this thread
        if (delayedIndexFlusher != null)
        {
            delayedIndexFlusher.interrupt();
            delayedIndexFlusher = null;
        }
    }

    ////////////////////////////////////
    //      Private
    ////////////////////////////////////

    private static void emailException(Exception exception) {
		// Also email an alert, system admin may need to check for stale lock
		try {
			String recipient = ConfigurationManager
					.getProperty("alert.recipient");

			if (StringUtils.isNotBlank(recipient)) {
				Email email = Email.getEmail(I18nUtil.getEmailFilename(Locale.getDefault(), "internal_error"));
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
	 * @param lastModified
	 * @throws SQLException
	 * @throws IOException
	 */
    private static boolean requiresIndexing(Term t, Date lastModified)
    throws SQLException, IOException
    {

		boolean reindexItem = false;
		boolean inIndex = false;

		IndexReader ir = DSQuery.getIndexReader();
		Bits liveDocs = MultiFields.getLiveDocs(ir);
		DocsEnum docs = MultiFields.getTermDocsEnum(ir, liveDocs, t.field(), t.bytes());

		int id;
        if (docs != null)
        {
            while ((id = docs.nextDoc()) != DocsEnum.NO_MORE_DOCS)
            {
                inIndex = true;
                Document doc = ir.document(id);

                IndexableField lastIndexed = doc.getField(LAST_INDEXED_FIELD);

                if (lastIndexed == null
                        || Long.parseLong(lastIndexed.stringValue()) < lastModified
                                .getTime())
                {
                    reindexItem = true;
                }
            }
        }
		return reindexItem || !inIndex;
	}

    /**
     * prepare index, opening writer, and wiping out existing index if necessary
     */
    private static IndexWriter openIndex(boolean wipeExisting)
            throws IOException
    {
        Directory dir = FSDirectory.open(new File(indexDirectory));
        
        LimitTokenCountAnalyzer decoratorAnalyzer = null; 
        /* Set maximum number of terms to index if present in dspace.cfg */
        if (maxfieldlength == -1)
        {
            decoratorAnalyzer = new LimitTokenCountAnalyzer(getAnalyzer(), Integer.MAX_VALUE);
        }
        else
        {
            decoratorAnalyzer = new LimitTokenCountAnalyzer(getAnalyzer(), maxfieldlength);
        }

        
        IndexWriterConfig iwc = new IndexWriterConfig(luceneVersion, decoratorAnalyzer);
        if(wipeExisting){
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        }else{
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        IndexWriter writer = new IndexWriter(dir, iwc);

        return writer;
    }

    /**
     * @param myitem
     * @return
     * @throws SQLException
     */
    private static String buildItemLocationString(Item myitem) throws SQLException
    {
        // build list of community ids
        Community[] communities = myitem.getCommunities();

        // build list of collection ids
        Collection[] collections = myitem.getCollections();

        // now put those into strings
        StringBuffer location = new StringBuffer();
        int i = 0;

        for (i = 0; i < communities.length; i++)
        {
            location.append(" m").append(communities[i].getID());
        }

        for (i = 0; i < collections.length; i++)
        {
            location.append(" l").append(collections[i].getID());
        }

        return location.toString();
    }

    private static String buildCollectionLocationString(Collection target) throws SQLException
    {
        // build list of community ids
        Community[] communities = target.getCommunities();

        // now put those into strings
        StringBuffer location = new StringBuffer();
        int i = 0;

        for (i = 0; i < communities.length; i++)
        {
            location.append(" m").append(communities[i].getID());
        }

        return location.toString();
    }

    /**
     * Build a Lucene document for a DSpace Community.
     *
     * @param community Community to be indexed
     * @throws SQLException
     * @throws IOException
     */
    private static Document buildDocumentForCommunity(Community community) throws SQLException, IOException
    {
        // Create Lucene Document
        Document doc = buildDocument(Constants.COMMUNITY, community.getID(), community.getHandle(), null);

        // and populate it
        String name = community.getMetadata("name");

        if (name != null)
        {
        	doc.add(new Field("name", name, Field.Store.NO, Field.Index.ANALYZED));
        	doc.add(new Field("default", name, Field.Store.NO, Field.Index.ANALYZED));
        }

        return doc;
    }

    /**
     * Build a Lucene document for a DSpace Collection.
     *
     * @param collection Collection to be indexed
     * @throws SQLException
     * @throws IOException
     */
    private static Document buildDocumentForCollection(Collection collection) throws SQLException, IOException
    {
        String location_text = buildCollectionLocationString(collection);

        // Create Lucene Document
        Document doc = buildDocument(Constants.COLLECTION, collection.getID(), collection.getHandle(), location_text);

        // and populate it
        String name = collection.getMetadata("name");

        if (name != null)
        {
        	doc.add(new Field("name", name, Field.Store.NO, Field.Index.ANALYZED));
        	doc.add(new Field("default", name, Field.Store.NO, Field.Index.ANALYZED));
        }

        return doc;
    }

    /**
     * Build a Lucene document for a DSpace Item and write the index
     *
     * @param item The DSpace Item to be indexed
     * @throws SQLException
     * @throws IOException
     */
    private static Document buildDocumentForItem(Context context, Item item) throws SQLException, IOException, DCInputsReaderException
    {
    	String handle = HandleManager.findHandle(context, item);

    	// get the location string (for searching by collection & community)
        String location = buildItemLocationString(item);

        Document doc = buildDocument(Constants.ITEM, item.getID(), handle, location);

        log.debug("Building Item: " + handle);

        int j;
        if (indexConfigArr.length > 0)
        {
            Metadatum[] mydc;

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


                //Index the controlled vocabularies localized display values for all localized input-forms.xml (e.g. input-forms_el.xml)
                if ("inputform".equalsIgnoreCase(indexConfigArr[i].type)){

                    List<String> newValues = new ArrayList<String>();
                    Locale[] supportedLocales=I18nUtil.getSupportedLocales();

                    // Get the display value of the respective stored value
                    for (int k = 0; k < supportedLocales.length; k++)
                    {
                        List<String> displayValues = Util
                                .getControlledVocabulariesDisplayValueLocalized(
                                        item, mydc, indexConfigArr[i].schema,
                                        indexConfigArr[i].element,
                                        indexConfigArr[i].qualifier,
                                        supportedLocales[k]);
                        if (displayValues != null && !displayValues.isEmpty())
                        {
                            for (int d = 0; d < displayValues.size(); d++)
                            {
                                newValues.add(displayValues.get(d));
                            }
                        }

                    }

                    if (newValues!=null){
                        for (int m=0;m<newValues.size();m++){
                            if (!"".equals(newValues.get(m))){

                                String toAdd=(String) newValues.get(m);
                                doc.add( new Field(indexConfigArr[i].indexName,
                                        toAdd,
                                        Field.Store.NO,
                                        Field.Index.ANALYZED));
                            }
                        }
                    }

                }


             for (j = 0; j < mydc.length; j++)
                {
                    if (!StringUtils.isEmpty(mydc[j].value))
                    {
                        if ("timestamp".equalsIgnoreCase(indexConfigArr[i].type))
                        {
                            Date d = toDate(mydc[j].value);
                            if (d != null)
                            {
                                doc.add( new Field(indexConfigArr[i].indexName,
                                                   DateTools.dateToString(d, DateTools.Resolution.SECOND),
                                                   Field.Store.NO,
                                                   Field.Index.NOT_ANALYZED));

                                doc.add( new Field(indexConfigArr[i].indexName  + ".year",
                                                    DateTools.dateToString(d, DateTools.Resolution.YEAR),
                                                    Field.Store.NO,
                                                    Field.Index.NOT_ANALYZED));
                            }
                        }
                        else if ("date".equalsIgnoreCase(indexConfigArr[i].type))
                        {
                            Date d = toDate(mydc[j].value);
                            if (d != null)
                            {
                                doc.add( new Field(indexConfigArr[i].indexName,
                                                   DateTools.dateToString(d, DateTools.Resolution.DAY),
                                                   Field.Store.NO,
                                                   Field.Index.NOT_ANALYZED));

                                doc.add( new Field(indexConfigArr[i].indexName  + ".year",
                                                    DateTools.dateToString(d, DateTools.Resolution.YEAR),
                                                    Field.Store.NO,
                                                    Field.Index.NOT_ANALYZED));
                            }
                        }
                        else
                        {
                            List<String> variants = null;
                            if (mydc[j].authority != null && mydc[j].confidence >= MetadataAuthorityManager.getManager()
                                    .getMinConfidence(mydc[j].schema, mydc[j].element, mydc[j].qualifier))
                            {
                                variants = ChoiceAuthorityManager.getManager()
                                            .getVariants(mydc[j].schema, mydc[j].element, mydc[j].qualifier,
                                                mydc[j].authority, mydc[j].language);

                                doc.add( new Field(indexConfigArr[i].indexName+"_authority",
                                   mydc[j].authority,
                                   Field.Store.NO,
                                   Field.Index.NOT_ANALYZED));

                                boolean valueAlreadyIndexed = false;
                                if (variants != null)
                                {
                                    for (String var : variants)
                                    {
                                        // TODO: use a delegate to allow custom 'types' to be used to reformat the field
                                        doc.add( new Field(indexConfigArr[i].indexName,
                                                           var,
                                                           Field.Store.NO,
                                                           Field.Index.ANALYZED));
                                        if (var.equals(mydc[j].value))
                                        {
                                            valueAlreadyIndexed = true;
                                        }
                                        else
                                        {   // add to default index too...
                                            // (only variants, main value is already take)
                                             doc.add( new Field("default",
                                                       var,
                                                       Field.Store.NO,
                                                       Field.Index.ANALYZED));
                                        }
                                    }
                                }

                                if (!valueAlreadyIndexed)
                                {
                                    // TODO: use a delegate to allow custom 'types' to be used to reformat the field
                                    doc.add( new Field(indexConfigArr[i].indexName,
                                                       mydc[j].value,
                                                       Field.Store.NO,
                                                       Field.Index.ANALYZED));
                                }
                            }
                            else
                            {
	                            // TODO: use a delegate to allow custom 'types' to be used to reformat the field
	                            doc.add( new Field(indexConfigArr[i].indexName,
	                                               mydc[j].value,
	                                               Field.Store.NO,
	                                               Field.Index.ANALYZED));
                        	}
                        }

                        doc.add( new Field("default", mydc[j].value, Field.Store.NO, Field.Index.ANALYZED));
                    }
                }
            }
        }

        log.debug("  Added Metadata");

        try
        {
            // Now get the configured sort options, and add those as untokenized fields
            // Note that we will use the sort order delegates to normalise the values written
            for (SortOption so : SortOption.getSortOptions())
            {
                String[] somd = so.getMdBits();
                Metadatum[] dcv = item.getMetadata(somd[0], somd[1], somd[2], Item.ANY);
                if (dcv.length > 0)
                {
                    String value = OrderFormat.makeSortString(dcv[0].value, dcv[0].language, so.getType());
                    doc.add( new Field("sort_" + so.getName(), value, Field.Store.NO, Field.Index.NOT_ANALYZED) );
                }
            }
        }
        catch (Exception e)
        {
            log.error(e.getMessage(),e);
        }

        log.debug("  Added Sorting");

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
                            // Add each InputStream to the Indexed Document (Acts like an Append)
                            doc.add(new Field("default", new BufferedReader(new InputStreamReader(myBitstreams[j].retrieve()))));

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

        log.info("Wrote Item: " + handle + " to Index");
        return doc;
    }

    /**
     * Create Lucene document with all the shared fields initialized.
     *
     * @param type Type of DSpace Object
     * @param id
     *@param handle
     * @param location @return
     */
    private static Document buildDocument(int type, int id, String handle, String location)
    {
        Document doc = new Document();

        // want to be able to check when last updated
        // (not tokenized, but it is indexed)
        doc.add(new Field(LAST_INDEXED_FIELD, Long.toString(System.currentTimeMillis()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(DOCUMENT_STATUS_FIELD, "archived", Field.Store.YES, Field.Index.NOT_ANALYZED));

        // KEPT FOR BACKWARDS COMPATIBILITY
        // do location, type, handle first
        doc.add(new Field("type", Integer.toString(type), Field.Store.YES, Field.Index.NO));

        // New fields to weaken the dependence on handles, and allow for faster list display
        doc.add(new Field("search.resourcetype", Integer.toString(type), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("search.resourceid",   Integer.toString(id),   Field.Store.YES, Field.Index.NO));

        // want to be able to search for handle, so use keyword
        // (not tokenized, but it is indexed)
        if (handle != null)
        {
            // ??? not sure what the "handletext" field is but it was there in writeItemIndex ???
            doc.add(new Field("handletext", handle, Field.Store.YES, Field.Index.ANALYZED));

            // want to be able to search for handle, so use keyword
            // (not tokenized, but it is indexed)
            doc.add(new Field("handle", handle, Field.Store.YES, Field.Index.NOT_ANALYZED));

            // add to full text index
            doc.add(new Field("default", handle, Field.Store.NO, Field.Index.ANALYZED));
        }

        if(location != null)
        {
            doc.add(new Field("location", location, Field.Store.NO, Field.Index.ANALYZED));
    	    doc.add(new Field("default", location, Field.Store.NO, Field.Index.ANALYZED));
        }

        return doc;
    }

    private static Document buildDocumentForDeletedHandle(String handle)
    {
        Document doc = new Document();

        // want to be able to check when last updated
        // (not tokenized, but it is indexed)
        doc.add(new Field(LAST_INDEXED_FIELD,    Long.toString(System.currentTimeMillis()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(DOCUMENT_STATUS_FIELD, "deleted", Field.Store.YES, Field.Index.NOT_ANALYZED));

        // Do not add any other fields, as we don't want to be able to find it - just check the last indexed time

        return doc;
    }

    private static Document buildDocumentForWithdrawnItem(Item item)
    {
        Document doc = new Document();

        // want to be able to check when last updated
        // (not tokenized, but it is indexed)
        doc.add(new Field(LAST_INDEXED_FIELD,    Long.toString(System.currentTimeMillis()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(DOCUMENT_STATUS_FIELD, "withdrawn", Field.Store.YES, Field.Index.NOT_ANALYZED));

        // Do not add any other fields, as we don't want to be able to find it - just check the last indexed time

        return doc;
    }

    private static void closeAllReaders(Document doc)
    {
        if (doc != null)
        {
            int count = 0;
            List fields = doc.getFields();
            if (fields != null)
            {
                for (Field field : (List<Field>)fields)
                {
                    Reader r = field.readerValue();
                    if (r != null)
                    {
                        try
                        {
                            r.close();
                            count++;
                        }
                        catch (IOException e)
                        {
                            log.error("Unable to close reader", e);
                        }
                    }
                }
            }

            if (count > 0)
            {
                log.debug("closed " + count + " readers");
            }
        }
    }

    /**
     * Helper function to retrieve a date using a best guess of the potential date encodings on a field
     *
     * @param t
     * @return
     */
    private static Date toDate(String t)
    {
        SimpleDateFormat[] dfArr;

        // Choose the likely date formats based on string length
        switch (t.length())
        {
            case 4:
                dfArr = new SimpleDateFormat[] { new SimpleDateFormat("yyyy") };
                break;
            case 6:
                dfArr = new SimpleDateFormat[] { new SimpleDateFormat("yyyyMM") };
                break;
            case 7:
                dfArr = new SimpleDateFormat[] { new SimpleDateFormat("yyyy-MM") };
                break;
            case 8:
                dfArr = new SimpleDateFormat[] { new SimpleDateFormat("yyyyMMdd"), new SimpleDateFormat("yyyy MMM") };
                break;
            case 10:
                dfArr = new SimpleDateFormat[] { new SimpleDateFormat("yyyy-MM-dd") };
                break;
            case 11:
                dfArr = new SimpleDateFormat[] { new SimpleDateFormat("yyyy MMM dd") };
                break;
            case 20:
                dfArr = new SimpleDateFormat[] { new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") };
                break;
            default:
                dfArr = new SimpleDateFormat[] { new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") };
                break;
        }


        for (SimpleDateFormat df : dfArr)
        {
            try
            {
                // Parse the date
                df.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                df.setLenient(false);
                return df.parse(t);
            }
            catch (ParseException pe)
            {
                log.error("Unable to parse date format", pe);
            }
        }

        return null;
    }

    private static synchronized void startDelayedIndexFlusher()
    {
        if (delayedIndexFlusher != null && !delayedIndexFlusher.isAlive())
        {
            delayedIndexFlusher = null;
        }

        if (delayedIndexFlusher == null && queuedTaskMap.size() > 0)
        {
            delayedIndexFlusher = new Thread(new DelayedIndexFlushThread());
            delayedIndexFlusher.start();
        }
    }

    private static class DelayedIndexFlushThread implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                Thread.sleep(indexFlushDelay);
                DSIndexer.flushIndexingTaskQueue();
            }
            catch (InterruptedException e)
            {
                log.debug(e);
            }
        }
    }
}
