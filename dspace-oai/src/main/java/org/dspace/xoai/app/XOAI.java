/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import com.lyncode.xoai.dataprovider.exceptions.ConfigurationException;
import com.lyncode.xoai.dataprovider.exceptions.MetadataBindException;
import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
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
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xoai.exceptions.CompilingException;
import org.dspace.xoai.services.api.CollectionsService;
import org.dspace.xoai.services.api.cache.XOAICacheService;
import org.dspace.xoai.services.api.cache.XOAIItemCacheService;
import org.dspace.xoai.services.api.cache.XOAILastCompilationCacheService;
import org.dspace.xoai.services.api.solr.SolrServerResolver;
import org.dspace.xoai.solr.DSpaceSolrSearch;
import org.dspace.xoai.solr.exceptions.DSpaceSolrException;
import org.dspace.xoai.solr.exceptions.DSpaceSolrIndexerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

import static com.lyncode.xoai.dataprovider.core.Granularity.Second;
import static org.dspace.xoai.util.ItemUtils.retrieveMetadata;

import org.dspace.authorize.ResourcePolicy;

/**
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@SuppressWarnings("deprecation")
public class XOAI {
    private static Logger log = LogManager.getLogger(XOAI.class);

    private final Context context;
    private boolean optimize;
    private final boolean verbose;
    private boolean clean;

    @Autowired
    private SolrServerResolver solrServerResolver;
    @Autowired
    private XOAILastCompilationCacheService xoaiLastCompilationCacheService;
    @Autowired
    private XOAIItemCacheService xoaiItemCacheService;
    @Autowired
    private CollectionsService collectionsService;

    private final AuthorizeService authorizeService;
    private final ItemService itemService;
    private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    
    private List<XOAIItemCompilePlugin> xOAIItemCompilePlugins;
    
    private List<String> getFileFormats(Item item) {
        List<String> formats = new ArrayList<>();
        try {
            for (Bundle b : itemService.getBundles(item, "ORIGINAL")) {
                for (Bitstream bs : b.getBitstreams()) {
                    if (!formats.contains(bs.getFormat(context).getMIMEType())) {
                        formats.add(bs.getFormat(context).getMIMEType());
                    }
                }
            }
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }
        return formats;
    }

    public XOAI(Context context, boolean optimize, boolean clean, boolean verbose) {
        this.context = context;
        this.optimize = optimize;
        this.clean = clean;
        this.verbose = verbose;

        // Load necessary DSpace services
        this.authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
    }

    public XOAI(Context ctx, boolean hasOption) {
        this.context = ctx;
        this.verbose = hasOption;

        // Load necessary DSpace services
        this.authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
    }

    private void println(String line) {
        System.out.println(line);
    }

    public int index() throws DSpaceSolrIndexerException {
        int result = 0;
        try {

            if (clean) {
                clearIndex();
                System.out.println("Using full import.");
                result = this.indexAll();
            } else {
                SolrQuery solrParams = new SolrQuery("*:*")
                        .addField("item.lastmodified")
                        .addSortField("item.lastmodified", ORDER.desc).setRows(1);

                SolrDocumentList results = DSpaceSolrSearch.query(solrServerResolver.getServer(), solrParams);
                if (results.getNumFound() == 0) {
                    System.out.println("There are no indexed documents, using full import.");
                    result = this.indexAll();
                } else
                    result = this.index((Date) results.get(0).getFieldValue("item.lastmodified"));

            }
            solrServerResolver.getServer().commit();

            if (optimize) {
                println("Optimizing Index");
                solrServerResolver.getServer().optimize();
                println("Index optimized");
            }

            // Set last compilation date
            xoaiLastCompilationCacheService.put(new Date());
            return result;
        } catch (DSpaceSolrException | SolrServerException | IOException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    private int index(Date last) throws DSpaceSolrIndexerException {
        System.out.println("Incremental import. Searching for documents modified after: " + last.toString());
        /*
         * Index all changed or new items or items whose visibility is viable to
         * change due to an embargo.
         */
        try {
            Iterator<Item> discoverableChangedItems = itemService
                    .findInArchiveOrWithdrawnDiscoverableModifiedSince(context, last);

            Iterator<Item> nonDiscoverableChangedItems = itemService
                    .findInArchiveOrWithdrawnNonDiscoverableModifiedSince(context, last);

            Iterator<Item> possiblyChangedItems = getItemsWithPossibleChangesBefore(last);

            return this.index(discoverableChangedItems) + this.index(nonDiscoverableChangedItems)
                    + this.index(possiblyChangedItems);

        } catch (SQLException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    /**
     * Get all items already in the index which are viable to change visibility
     * due to an embargo. Only consider those which haven't been modified
     * anyways since the last update, so they aren't updated twice in one import
     * run.
     * 
     * @param last
     *            maximum date for an item to be considered for an update
     * @return Iterator over list of items which might have changed their
     *         visibility since the last update.
     * @throws DSpaceSolrIndexerException
     */
    private Iterator<Item> getItemsWithPossibleChangesBefore(Date last) throws DSpaceSolrIndexerException {
        try {
            SolrQuery params = new SolrQuery("item.willChangeStatus:true").addField("item.id");
            SolrDocumentList documents = DSpaceSolrSearch.query(solrServerResolver.getServer(), params);
            List<Item> items = new LinkedList<Item>();
            for (int i = 0; i < documents.getNumFound(); i++) {
                Item item = itemService.find(context,
                        UUID.fromString((String) documents.get(i).getFieldValue("item.id")));
                if (item.getLastModified().before(last)) {
                    items.add(item);
                }
            }
            return items.iterator();
        } catch (SolrServerException | SQLException | DSpaceSolrException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    private int indexAll() throws DSpaceSolrIndexerException {
        System.out.println("Full import");
        try {
            // Index both in_archive items AND withdrawn items. Withdrawn items
            // will be flagged withdrawn
            // (in order to notify external OAI harvesters of their new status)
            Iterator<Item> discoverableItems = itemService.findInArchiveOrWithdrawnDiscoverableModifiedSince(context,
                    null);
            Iterator<Item> nonDiscoverableItems = itemService
                    .findInArchiveOrWithdrawnNonDiscoverableModifiedSince(context, null);
            return this.index(discoverableItems) + this.index(nonDiscoverableItems);
        } catch (SQLException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    /**
     * Check if an item is already indexed. Using this, it is possible to check
     * if withdrawn or nondiscoverable items have to be indexed at all.
     * 
     * @param item
     *            Item that should be checked for its presence in the index.
     * @return has it been indexed?
     */
    private boolean checkIfIndexed(Item item) {
        SolrQuery params = new SolrQuery("item.id:" + item.getID().toString()).addField("item.id");
        try {
            SolrDocumentList documents = DSpaceSolrSearch.query(solrServerResolver.getServer(), params);
            return documents.getNumFound() == 1;
        } catch (DSpaceSolrException | SolrServerException e) {
            return false;
        }
    }

    /**
     * Check if an item is flagged visible in the index.
     * 
     * @param item
     *            Item that should be checked for its presence in the index.
     * @return has it been indexed?
     */
    private boolean checkIfVisibleInOAI(Item item) {
        SolrQuery params = new SolrQuery("item.id:" + item.getID().toString()).addField("item.public");
        try {
            SolrDocumentList documents = DSpaceSolrSearch.query(solrServerResolver.getServer(), params);
            if (documents.getNumFound() == 1) {
                return (boolean) documents.get(0).getFieldValue("item.public");
            } else {
                return false;
            }
        } catch (DSpaceSolrException | SolrServerException e) {
            return false;
        }
    }

    private int index(Iterator<Item> iterator) throws DSpaceSolrIndexerException {
        try {
            int i = 0;
            int batchSize = configurationService.getIntProperty("oai.import.batch.size", 1000);
            SolrServer server = solrServerResolver.getServer();
            ArrayList<SolrInputDocument> list = new ArrayList<>();
            while (iterator.hasNext()) {
                try {
                    Item item = iterator.next();
                    if (item.getHandle() == null) {
                        log.warn("Skipped item without handle: " + item.getID());
                    } else {
                        list.add(this.index(item));
                    }
                    context.uncacheEntity(item);

                } catch (SQLException | MetadataBindException | ParseException | XMLStreamException
                        | WritingXmlException ex) {
                    log.error(ex.getMessage(), ex);
                }
                i++;
                if (i % 1000 == 0) {
                    System.out.println(i + " items prepared so far...");
                }
                if (i % batchSize == 0) {
                    System.out.println(i + " items imported so far...");
                    server.add(list);
                    server.commit();
                    list.clear();
                }
            }
            System.out.println("Total: " + i + " items");
            if (i > 0) {
                server.add(list);
                server.commit(true, true);
                list.clear();
            }
            return i;
        } catch (SolrServerException | IOException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    /**
     * Method to get the most recent date on which the item changed concerning
     * the OAI deleted status (policy start and end dates for all anonymous READ
     * policies and the standard last modification date)
     *
     * @param item
     *            Item
     * @return date
     * @throws SQLException
     */

    private Date getMostRecentModificationDate(Item item) throws SQLException {
        List<Date> dates = new LinkedList<Date>();
        List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, item, Constants.READ);
        for (ResourcePolicy policy : policies) {
            if ((policy.getGroup()!=null) && (policy.getGroup().getName().equals("Anonymous"))) {
                if (policy.getStartDate() != null) {
                    dates.add(policy.getStartDate());
                }
                if (policy.getEndDate() != null) {
                    dates.add(policy.getEndDate());
                }
            }
            context.uncacheEntity(policy);
        }
        dates.add(item.getLastModified());
        Collections.sort(dates);
        Date now = new Date();
        Date lastChange = null;
        for (Date d : dates) {
            if (d.before(now)) {
                lastChange = d;
            }
        }
        return lastChange;
    }

    private SolrInputDocument index(Item item)
            throws SQLException, MetadataBindException, ParseException, XMLStreamException, WritingXmlException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("item.id", item.getID());

        String handle = item.getHandle();
        doc.addField("item.handle", handle);

        boolean isEmbargoed = !this.isPublic(item);
        boolean isCurrentlyVisible = this.checkIfVisibleInOAI(item);
        boolean isIndexed = this.checkIfIndexed(item);

        /*
         * If the item is not under embargo, it should be visible. If it is,
         * make it invisible if this is the first time it is indexed. For
         * subsequent index runs, keep the current status, so that if the item
         * is embargoed again, it is flagged as deleted instead and does not
         * just disappear, or if it is still under embargo, it won't become
         * visible and be known to harvesters as deleted before it gets
         * disseminated for the first time. The item has to be indexed directly
         * after publication even if it is still embargoed, because its
         * lastModified date will not change when the embargo end date (or start
         * date) is reached. To circumvent this, an item which will change its
         * status in the future will be marked as such.
         */

        boolean isPublic = isEmbargoed ? (isIndexed ? isCurrentlyVisible : false) : true;
        
        doc.addField("item.public", isPublic);

        // if the visibility of the item will change in the future due to an
        // embargo, mark it as such.

        doc.addField("item.willChangeStatus", willChangeStatus(item));

        /*
         * Mark an item as deleted not only if it is withdrawn, but also if it
         * is made private, because items should not simply disappear from OAI
         * with a transient deletion policy. Do not set the flag for still
         * invisible embargoed items, because this will override the item.public
         * flag.
         */

        doc.addField("item.deleted",
                (item.isWithdrawn() || !item.isDiscoverable() || (isEmbargoed ? isPublic : false)));

        /*
         * An item that is embargoed will potentially not be harvested by
         * incremental harvesters if the from and until params do not encompass
         * both the standard lastModified date and the anonymous-READ resource
         * policy start date. The same is true for the end date, where
         * harvesters might not get a tombstone record. Therefore, consider all
         * relevant policy dates and the standard lastModified date and take the
         * most recent of those which have already passed.
         */
        doc.addField("item.lastmodified", this.getMostRecentModificationDate(item));

        if (item.getSubmitter() != null) {
            doc.addField("item.submitter", item.getSubmitter().getEmail());
        }

        for (Collection col : item.getCollections())
            doc.addField("item.collections", "col_" + col.getHandle().replace("/", "_"));
        for (Community com : collectionsService.flatParentCommunities(context, item))
            doc.addField("item.communities", "com_" + com.getHandle().replace("/", "_"));

        List<MetadataValue> allData = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (MetadataValue dc : allData) {
            MetadataField field = dc.getMetadataField();
            String key = "metadata." + field.getMetadataSchema().getName() + "." + field.getElement();
            if (field.getQualifier() != null) {
                key += "." + field.getQualifier();
            }
            doc.addField(key, dc.getValue());
            if (dc.getAuthority() != null) {
                doc.addField(key + ".authority", dc.getAuthority());
                doc.addField(key + ".confidence", dc.getConfidence() + "");
            }
        }

        for (String f : getFileFormats(item)) {
            doc.addField("metadata.dc.format.mimetype", f);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlOutputContext xmlContext = XmlOutputContext.emptyContext(out, Second);
        Metadata metadata = retrieveMetadata(context, item);
        
        //Do any additional metadata element, depends on the plugins
        for (XOAIItemCompilePlugin xOAIItemCompilePlugin : getxOAIItemCompilePlugins())
        {
            metadata = xOAIItemCompilePlugin.additionalMetadata(context, metadata, item);
        }
        
        metadata.write(xmlContext);
        xmlContext.getWriter().flush();
        xmlContext.getWriter().close();
        doc.addField("item.compile", out.toString());

        if (verbose) {
            println(String.format("Item %s with handle %s indexed",
                    item.getID().toString(), handle));
        }

        return doc;
    }

    private boolean willChangeStatus(Item item) throws SQLException {

        List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, item, Constants.READ);
        for (ResourcePolicy policy : policies) {
            if ((policy.getGroup()!=null) && (policy.getGroup().getName().equals("Anonymous"))) {
                
                if (policy.getStartDate() != null && policy.getStartDate().after(new Date())) {
                    
                    return true;
                }
                if (policy.getEndDate() != null && policy.getEndDate().after(new Date())) {
                    
                    return true;
                }
            }
            context.uncacheEntity(policy);
        }
        
        return false;
    }

    private boolean isPublic(Item item) {
        boolean pub = false;
        try {
            // Check if READ access allowed on this Item
            pub = authorizeService.authorizeActionBoolean(context, item, Constants.READ);
        } catch (SQLException ex) {
            log.error(ex.getMessage());
        }
        return pub;
    }

    private static boolean getKnownExplanation(Throwable t) {
        if (t instanceof ConnectException) {
            System.err.println(
                    "Solr server (" + configurationService.getProperty("oai.solr.uri", "") + ") is down, turn it on.");
            return true;
        }

        return false;
    }

    private static boolean searchForReason(Throwable t) {
        if (getKnownExplanation(t))
            return true;
        if (t.getCause() != null)
            return searchForReason(t.getCause());
        return false;
    }

    private void clearIndex() throws DSpaceSolrIndexerException {
        try {
            System.out.println("Clearing index");
            solrServerResolver.getServer().deleteByQuery("*:*");
            solrServerResolver.getServer().commit();
            System.out.println("Index cleared");
        } catch (SolrServerException | IOException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    private static void cleanCache(XOAIItemCacheService xoaiItemCacheService, XOAICacheService xoaiCacheService) throws IOException {
        System.out.println("Purging cached OAI responses.");
        xoaiItemCacheService.deleteAll();
        xoaiCacheService.deleteAll();
    }

    private static final String COMMAND_IMPORT = "import";
    private static final String COMMAND_CLEAN_CACHE = "clean-cache";
    private static final String COMMAND_COMPILE_ITEMS = "compile-items";
    private static final String COMMAND_ERASE_COMPILED_ITEMS = "erase-compiled-items";

    public static void main(String[] argv) throws IOException, ConfigurationException {

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(new Class[] { BasicConfiguration.class });

        XOAICacheService cacheService = applicationContext.getBean(XOAICacheService.class);
        XOAIItemCacheService itemCacheService = applicationContext.getBean(XOAIItemCacheService.class);

        Context ctx = null;

        try {
            CommandLineParser parser = new PosixParser();
            Options options = new Options();
            options.addOption("c", "clear", false, "Clear index before indexing");
            options.addOption("o", "optimize", false, "Optimize index at the end");
            options.addOption("v", "verbose", false, "Verbose output");
            options.addOption("h", "help", false, "Shows some help");
            options.addOption("n", "number", true, "FOR DEVELOPMENT MUST DELETE");
            CommandLine line = parser.parse(options, argv);

            String[] validSolrCommands = { COMMAND_IMPORT, COMMAND_CLEAN_CACHE };
            String[] validDatabaseCommands = { COMMAND_CLEAN_CACHE, COMMAND_COMPILE_ITEMS,
                    COMMAND_ERASE_COMPILED_ITEMS };

            boolean solr = true; // Assuming solr by default
            solr = !("database").equals(configurationService.getProperty("oai.storage", "solr"));

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
                System.out.println("OAI 2.0 manager action started");
                long start = System.currentTimeMillis();

                String command = line.getArgs()[0];

                if (COMMAND_IMPORT.equals(command)) {
                    ctx = new Context(Context.Mode.READ_ONLY);
                    XOAI indexer = new XOAI(ctx, 
                            line.hasOption('o'), 
                            line.hasOption('c'), 
                            line.hasOption('v'));

                    applicationContext.getAutowireCapableBeanFactory().autowireBean(indexer);

                    int imported = indexer.index();
                    
                    if (imported > 0) cleanCache(itemCacheService, cacheService);
                    
                } else if (COMMAND_CLEAN_CACHE.equals(command)) {
                    cleanCache(itemCacheService, cacheService);
                } else if (COMMAND_COMPILE_ITEMS.equals(command)) {

                    ctx = new Context();
                    XOAI indexer = new XOAI(ctx, line.hasOption('v'));
                    applicationContext.getAutowireCapableBeanFactory().autowireBean(indexer);

                    indexer.compile();

                    cleanCache(itemCacheService, cacheService);
                } else if (COMMAND_ERASE_COMPILED_ITEMS.equals(command)) {
                    cleanCompiledItems(itemCacheService);
                    cleanCache(itemCacheService, cacheService);
                }

                System.out.println("OAI 2.0 manager action ended. It took "
                        + ((System.currentTimeMillis() - start) / 1000)
                        + " seconds.");
            } else {
                usage();
            }
        } catch (Throwable ex) {
            if (!searchForReason(ex)) {
                ex.printStackTrace();
            }
            log.error(ex.getMessage(), ex);
        }
        finally
        {
            // Abort our context, if still open
            if(ctx!=null && ctx.isValid())
                ctx.abort();
        }
    }

    private static void cleanCompiledItems(XOAIItemCacheService itemCacheService) throws IOException {
        System.out.println("Purging compiled items");
        itemCacheService.deleteAll();
    }

    private void compile() throws CompilingException {
        Iterator<Item> iterator;
        try {
            Date last = xoaiLastCompilationCacheService.get();

            if (last == null) {
                System.out.println("Retrieving all items to be compiled");
                iterator = itemService.findAll(context);
            } else {
                System.out.println("Retrieving items modified after " + last + " to be compiled");
                iterator = itemService.findByLastModifiedSince(context, last);
            }

            while (iterator.hasNext()) {
                Item item = iterator.next();
                if (verbose) System.out.println("Compiling item with handle: " + item.getHandle());
                xoaiItemCacheService.put(item, retrieveMetadata(context, item));
            }

            xoaiLastCompilationCacheService.put(new Date());
        } catch (SQLException | IOException e) {
            throw new CompilingException(e);
        }
        System.out.println("Items compiled");
    }

    private static void usage() {
        boolean solr = true; // Assuming solr by default
        solr = !("database").equals(configurationService.getProperty("oai.storage", "solr"));

        if (solr) {
            System.out.println("OAI Manager Script");
            System.out.println("Syntax: oai <action> [parameters]");
            System.out.println("> Possible actions:");
            System.out.println("     " + COMMAND_IMPORT + " - To import DSpace items into OAI index and cache system");
            System.out.println("     " + COMMAND_CLEAN_CACHE + " - Cleans the OAI cached responses");
            System.out.println("> Parameters:");
            System.out.println("     -o Optimize index after indexing (" + COMMAND_IMPORT + " only)");
            System.out.println("     -c Clear index (" + COMMAND_IMPORT + " only)");
            System.out.println("     -v Verbose output");
            System.out.println("     -h Shows this text");
        } else {
            System.out.println("OAI Manager Script");
            System.out.println("Syntax: oai <action> [parameters]");
            System.out.println("> Possible actions:");
            System.out.println("     " + COMMAND_CLEAN_CACHE + " - Cleans the OAI cached responses");
            System.out.println("     " + COMMAND_COMPILE_ITEMS + " - Compiles all DSpace items");
            System.out.println("     " + COMMAND_ERASE_COMPILED_ITEMS + " - Erase the OAI compiled items");
            System.out.println("> Parameters:");
            System.out.println("     -v Verbose output");
            System.out.println("     -h Shows this text");
        }
    }

	/**
	 * Do any additional content on "item.compile" field, depends on the plugins
	 * 
	 * @return
	 */
	public List<XOAIItemCompilePlugin> getxOAIItemCompilePlugins() {
		if(xOAIItemCompilePlugins==null) {
			xOAIItemCompilePlugins = DSpaceServicesFactory.getInstance().getServiceManager().getServicesByType(XOAIItemCompilePlugin.class);
		}
		return xOAIItemCompilePlugins;
	}

	public void setxOAIItemCompilePlugins(List<XOAIItemCompilePlugin> xOAIItemCompilePlugins) {
		this.xOAIItemCompilePlugins = xOAIItemCompilePlugins;
	}
}
