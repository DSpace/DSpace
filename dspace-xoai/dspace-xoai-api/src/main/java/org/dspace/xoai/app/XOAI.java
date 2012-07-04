/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.exceptions.CompilingException;
import org.dspace.xoai.solr.DSpaceSolrSearch;
import org.dspace.xoai.solr.DSpaceSolrServer;
import org.dspace.xoai.solr.exceptions.DSpaceSolrException;
import org.dspace.xoai.solr.exceptions.DSpaceSolrIndexerException;
import org.dspace.xoai.util.XOAICacheManager;
import org.dspace.xoai.util.XOAIDatabaseManager;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@SuppressWarnings("deprecation")
public class XOAI
{
    private static Logger log = LogManager.getLogger(XOAI.class);

    private Context _context;

    private boolean _optimize;

    private boolean _verbose;

    private static List<String> getFileFormats(Item item)
    {
        List<String> formats = new ArrayList<String>();
        try
        {
            for (Bundle b : item.getBundles("ORIGINAL"))
            {
                for (Bitstream bs : b.getBitstreams())
                {
                    if (!formats.contains(bs.getFormat().getMIMEType()))
                    {
                        formats.add(bs.getFormat().getMIMEType());
                    }
                }
            }
        }
        catch (SQLException ex)
        {
            log.error(ex.getMessage(), ex);
        }
        return formats;
    }

    public XOAI(Context ctx, boolean optimize, boolean verb)
    {
        _context = ctx;
        _optimize = optimize;
        _verbose = verb;
    }

    public XOAI(Context ctx, boolean hasOption)
    {
        _context = ctx;
        _verbose = hasOption;
    }

    private void println(String line)
    {
        System.out.println(line);
    }

    public void index() throws DSpaceSolrIndexerException
    {
        try
        {
            SolrQuery solrParams = new SolrQuery("*:*")
                    .addField("item.lastmodified")
                    .addSortField("item.lastmodified", ORDER.desc).setRows(1);

            SolrDocumentList results = DSpaceSolrSearch.query(solrParams);
            if (results.getNumFound() == 0)
            {
                System.out.println("There are no indexed documents, using full import.");
                this.indexAll();
            }
            else
                this.index((Date) results.get(0).getFieldValue("item.lastmodified"));
            
            DSpaceSolrServer.getServer().commit();
            
            if (_optimize)
            {
                println("Optimizing Index");
                DSpaceSolrServer.getServer().optimize();
                println("Index optimized");
            }
            
            // Set last compilation date
            XOAICacheManager.setLastCompilationDate(new Date());
        }
        catch (DSpaceSolrException ex)
        {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
        catch (SolrServerException ex)
        {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
        catch (IOException ex)
        {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    private void index(Date last) throws DSpaceSolrIndexerException
    {
        System.out
                .println("Incremental import of information. Searching for documents modified after: "
                        + last.toString());
        try
        {
            TableRowIterator iterator = DatabaseManager
                    .query(_context,
                            "SELECT item_id FROM item WHERE in_archive=TRUE AND last_modified > ?",
                            new java.sql.Date(last.getTime()));
            this.index(iterator);
        }
        catch (SQLException ex)
        {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    private void indexAll() throws DSpaceSolrIndexerException
    {
        System.out.println("Full import of information.");
        try
        {
            TableRowIterator iterator = DatabaseManager.query(_context,
                    "SELECT item_id FROM item WHERE in_archive=TRUE");
            this.index(iterator);
        }
        catch (SQLException ex)
        {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    private void index(TableRowIterator iterator)
            throws DSpaceSolrIndexerException
    {
        try
        {
            List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
            while (iterator.hasNext())
            {
                try
                {
                    docs.add(this.index(Item.find(_context, iterator.next()
                            .getIntColumn("item_id"))));
                    
                    
                    _context.clearCache();
                }
                catch (SQLException ex)
                {
                    log.error(ex.getMessage(), ex);
                }
            }
            SolrServer server = DSpaceSolrServer.getServer();
            server.add(docs);
            server.commit();
        }
        catch (SQLException ex)
        {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
        catch (SolrServerException ex)
        {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
        catch (IOException ex)
        {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    private SolrInputDocument index(Item item) throws SQLException
    {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("item.id", item.getID());
        boolean pub = this.isPublic(item);
        doc.addField("item.public", pub);
        String handle = item.getHandle();
        doc.addField("item.handle", handle);
        doc.addField("item.lastmodified", item.getLastModified());
        doc.addField("item.submitter", item.getSubmitter().getEmail());
        doc.addField("item.deleted", item.isWithdrawn() ? "true" : "false");
        for (Collection col : item.getCollections())
            doc.addField("item.collections",
                    "col_" + col.getHandle().replace("/", "_"));
        for (Community com : XOAIDatabaseManager.flatParentCommunities(item))
            doc.addField("item.communities",
                    "com_" + com.getHandle().replace("/", "_"));

        DCValue[] allData = item.getMetadata(Item.ANY, Item.ANY, Item.ANY,
                Item.ANY);
        for (DCValue dc : allData)
        {
            String key = "metadata." + dc.schema + "." + dc.element;
            if (dc.qualifier != null)
            {
                key += "." + dc.qualifier;
            }
            doc.addField(key, dc.value);
        }

        for (String f : getFileFormats(item))
        {
            doc.addField("metadata.dc.format.mimetype", f);
        }

        if (_verbose)
        {
            println("Item with handle "+handle+" indexed");
        }

        if (_verbose) 
        {
            println("Compiling item...");
        }
        
        XOAICacheManager.compileItem(new DSpaceItem(item));
        
        if (_verbose) 
        {
            println("Item compiled");
        }
        
        
        return doc;
    }

    private boolean isPublic(Item item)
    {
        try
        {
            AuthorizeManager.authorizeAction(_context, item, Constants.READ);
            for (Bundle b : item.getBundles())
                AuthorizeManager.authorizeAction(_context, b, Constants.READ);
            return true;
        }
        catch (AuthorizeException ex)
        {
            log.debug(ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error(ex.getMessage());
        }
        return false;
    }


    private static boolean getKnownExplanation(Throwable t)
    {
        if (t instanceof ConnectException)
        {
            System.err.println("Solr server ("
                    + ConfigurationManager.getProperty("xoai", "solr.url")
                    + ") is down, turn it on.");
            return true;
        }

        return false;
    }

    private static boolean searchForReason(Throwable t)
    {
        if (getKnownExplanation(t))
            return true;
        if (t.getCause() != null)
            return searchForReason(t.getCause());
        return false;
    }

    private static void clearIndex() throws DSpaceSolrIndexerException
    {
        try
        {
            System.out.println("Clearing index");
            DSpaceSolrServer.getServer().deleteByQuery("*:*");
            DSpaceSolrServer.getServer().commit();
            System.out.println("Index cleared");
        }
        catch (SolrServerException ex)
        {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
        catch (IOException ex)
        {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    private static void cleanCache()
    {
        System.out.println("Purging cached OAI responses.");
        XOAICacheManager.deleteCachedResponses();
    }

    private static final String COMMAND_IMPORT = "import";
    private static final String COMMAND_CLEAN_CACHE = "clean-cache";
    private static final String COMMAND_COMPILE_ITEMS = "compile-items";
    private static final String COMMAND_ERASE_COMPILED_ITEMS = "erase-compiled-items";
    
    public static void main(String[] argv)
    {
        try
        {
            CommandLineParser parser = new PosixParser();
            Options options = new Options();
            options.addOption("c", "clear", false, "Clear index before indexing");
            options.addOption("o", "optimize", false,
                    "Optimize index at the end");
            options.addOption("v", "verbose", false, "Verbose output");
            options.addOption("h", "help", false, "Shows some help");
            CommandLine line = parser.parse(options, argv);

            String[] validSolrCommands = { COMMAND_IMPORT, COMMAND_CLEAN_CACHE };
            String[] validDatabaseCommands = { COMMAND_CLEAN_CACHE, COMMAND_COMPILE_ITEMS, COMMAND_ERASE_COMPILED_ITEMS };
            
            
            boolean solr = true; // Assuming solr by default
            solr = !("database").equals(ConfigurationManager.getProperty("xoai", "storage"));
            
            
            boolean run = false;
            if (line.getArgs().length > 0) {
                if (solr) {
                    if (Arrays.asList(validSolrCommands).contains(line.getArgs()[0])) {
                        run = true;
                    }
                } else {
                    if (Arrays.asList(validDatabaseCommands).contains(line.getArgs()[0])) {
                        run = true;
                    }
                }
            }
            
            if (!line.hasOption('h') && run) {
                    System.out.println("XOAI manager action started");
                    long start = System.currentTimeMillis();
                    
                    String command = line.getArgs()[0];
                    
                    if (COMMAND_IMPORT.equals(command)) {
                        if (line.hasOption('c'))
                            clearIndex();

                        Context ctx = new Context();
                        XOAI indexer = new XOAI(ctx,
                                line.hasOption('o'), line.hasOption('v'));

                        indexer.index();
                        
                        cleanCache();
                        
                        ctx.abort();
                    } else if (COMMAND_CLEAN_CACHE.equals(command)) {
                        clearIndex();
                        cleanCache();
                    } else if (COMMAND_COMPILE_ITEMS.equals(command)) {

                        Context ctx = new Context();
                        XOAI indexer = new XOAI(ctx, line.hasOption('v'));

                        indexer.compile();
                        
                        cleanCache();
                        
                        ctx.abort();
                    } else if (COMMAND_ERASE_COMPILED_ITEMS.equals(command)) {
                        cleanCompiledItems();
                        cleanCache();
                    }

                    System.out.println("XOAI manager action ended. It took "
                            + ((System.currentTimeMillis() - start) / 1000)
                            + " seconds.");
            } else {
                usage();
            }
        }
        catch (Throwable ex)
        {
            if (!searchForReason(ex))
            {
                ex.printStackTrace();
            }
            log.error(ex.getMessage(), ex);
        }
    }
    
    private static void cleanCompiledItems()
    {
        System.out.println("Purging compiled items");
        XOAICacheManager.deleteCompiledItems();
    }

    private void compile() throws CompilingException
    {
        ItemIterator iterator;
        try
        {
            Date last = XOAICacheManager.getLastCompilationDate();
            
            if (last == null) {
                System.out.println("Retrieving all items to be compiled");
                iterator = Item.findAll(_context);
            } else {
                System.out.println("Retrieving items modified after "+last+" to be compiled");
                String query = "SELECT * FROM item WHERE last_modified>?";
                iterator = new ItemIterator(_context, DatabaseManager.query(_context, query, new java.sql.Date(last.getTime())));
            }
            
            while (iterator.hasNext()) {
                Item item = iterator.next();
                if (_verbose) System.out.println("Compiling item with handle: "+ item.getHandle());
                XOAICacheManager.compileItem(new DSpaceItem(item));
                _context.clearCache();
            }
            
            XOAICacheManager.setLastCompilationDate(new Date());
        }
        catch (SQLException e)
        {
            throw new CompilingException(e);
        }
        System.out.println("Items compiled");
    }

    private static void usage()
    {
        boolean solr = true; // Assuming solr by default
        solr = !("database").equals(ConfigurationManager.getProperty("xoai", "storage"));
        
        if (solr) {
            System.out.println("OAI Manager Script");
            System.out.println("Syntax: oai <action> [parameters]");
            System.out.println("> Possible actions:");
            System.out.println("     "+COMMAND_IMPORT+" - To import DSpace items into OAI index and cache system");
            System.out.println("     "+COMMAND_CLEAN_CACHE+" - Cleans the OAI cached responses");
            System.out.println("> Parameters:");
            System.out.println("     -o Optimize index after indexing");
            System.out.println("     -c Clear index");
            System.out.println("     -v Verbose output");
            System.out.println("     -h Shows this text");
        } else {
            System.out.println("OAI Manager Script");
            System.out.println("Syntax: oai <action> [parameters]");
            System.out.println("> Possible actions:");
            System.out.println("     "+COMMAND_CLEAN_CACHE+" - Cleans the OAI cached responses");
            System.out.println("     "+COMMAND_COMPILE_ITEMS+" - Compiles all DSpace items");
            System.out.println("     "+COMMAND_ERASE_COMPILED_ITEMS+" - Erase the OAI compiled items");
            System.out.println("> Parameters:");
            System.out.println("     -v Verbose output");
            System.out.println("     -h Shows this text");
        }
        
    }
}
