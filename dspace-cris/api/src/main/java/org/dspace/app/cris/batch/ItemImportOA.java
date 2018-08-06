/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.transform.TransformerException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.AdditionalMetadataUpdateProcessPlugin;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.IdentifierService;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.util.ItemUtils;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * Import from the imp_* table in the DSpace data model. Because the script
 * manage the history we suggest to run this script form {@link ItemImportMainOA} 
 * Available operation: -a to build item -r to update
 * (List of metadata to remove first and after do an update [by default all metadata are delete,
 * specifying only the dc.title it will obtain an append on the other metadata]; 
 * use this option many times on the single metadata e.g. -m dc.title -m dc.contributor.*) 
 * -d to remove the item
 * 
 * Status changes: -p to send in workspace -w to send in workspace step one -y to send
 * in workspace step two -x to send in workspace step three -z to send inarchive
 * 
 * 
 * Call the script with the option -h to discover more setting.
 * 
 * <em>For massive import see {@link ItemImportMainOA}</em>
 */
public class ItemImportOA
{

    private DSpace dspace = new DSpace();

    /** logger */
    private static Logger log = Logger.getLogger(ItemImportOA.class);

    private boolean goToWFStepOne = false;

    private boolean goToWFStepTwo = false;

    private boolean goToWFStepThree = false;

    private boolean goToPublishing = false;

    private boolean goToWithdrawn = false;

    private boolean workspace = false;

    private EPerson myEPerson = null;

    private EPerson batchJob = null;

    private String[] metadataClean = null;

    private String sourceRef = null;
    
    public static void main(String[] argv)
    {
        Context context = null;

        try
        {
            context = new Context();
            impRecord(context, argv);
            context.complete();
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            throw new RuntimeException();
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

    }

    public static int impRecord(Context context, String[] argv) throws Exception
    {
        // instantiate loader
        ItemImportOA myLoader = new ItemImportOA();

        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("a", "add", false, "add items to DSpace");
        options.addOption("b", "bitstream", false, "clear old bitstream");
        options.addOption("c", "collection", true,
                "destination collection(s) Handle or database ID");
        options.addOption("d", "delete", false, "delete items");
        options.addOption("e", "eperson", true, "id eperson doing importing");
        options.addOption("E", "batch_user", true, "user batch job");
        options.addOption("g", "withdrawn", false,
                "set item in withdrawn state");
        options.addOption("h", "help", false, "help");
        options.addOption("i", "record", true, "record ID");
        options.addOption("I", "importID", true, "import ID");        
        options.addOption("k", "handle", true, "handle of item");
        options.addOption("m", "metadata", true,
                "List of metadata to remove first and after do an update [by default all metadata are delete, specifying only the dc.title it will obtain an append on the other metadata]; use this option many times on the single metadata e.g. -m dc.title -m dc.contributor.*");
        options.addOption("o", "item", true, "item ID");
        options.addOption("p", "workspace", false,
                "send submission back to workspace");
        options.addOption("r", "replace", false, "update items");
        options.addOption("w", "workflow1", false,
                "send submission through collection's workflow step one");
        options.addOption("x", "workflow3", false,
                "send submission through collection's workflow step three");
        options.addOption("y", "workflow2", false,
                "send submission through collection's workflow step two");
        options.addOption("z", "published", false,
                "send submission through item's deposit");
        options.addOption("R", "sourceref", true,
                "name of the source");

        CommandLine line = parser.parse(options, argv);

        String command = null; // add replace remove, etc
        int epersonID = -1; // db ID
        String[] collections = null; // db ID or handles
        String handle = null;
        String imp_record_id = null;
        int item_id = 0;
        int imp_id = 0;

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ItemImport\n", options);
            System.out.println(
                    "adding items: ItemImport -a -e eperson -c collection");
            System.out.println(
                    "replacing items: ItemImport -r -e eperson -c collection");
            System.out.println("deleting items: ItemImport -d -e eperson");
            System.out.println(
                    "If multiple collections are specified, the first collection will be the one that owns the item.");

            System.exit(0);
        }

        if (line.hasOption('a'))
        {
            command = "add";
        }

        if (line.hasOption('r'))
        {
            command = "replace";
        }

        if (line.hasOption('d'))
        {
            command = "delete";
        }

        if (line.hasOption('w'))
        {
            myLoader.setGoToWFStepOne(true);
        }

        if (line.hasOption('y'))
        {
            myLoader.setGoToWFStepTwo(true);
        }

        if (line.hasOption('x'))
        {
            myLoader.setGoToWFStepThree(true);
        }

        if (line.hasOption('g'))
        {
            myLoader.setGoToWithdrawn(true);
        }

        if (line.hasOption('z'))
        {
            myLoader.setGoToPublishing(true);
        }

        if (line.hasOption('p'))
        {
            myLoader.setWorkspace(true);
        }

        if (line.hasOption('e')) // eperson
        {
            epersonID = Integer.parseInt(line.getOptionValue('e').trim());
        }

        if (line.hasOption('c')) // collections
        {
            collections = line.getOptionValues('c');
        }

        if (line.hasOption('i')) // record ID
        {
            imp_record_id = line.getOptionValue('i').trim();
        }
        if (line.hasOption('o')) // item ID (replace or delete)
        {
            item_id = Integer.parseInt(line.getOptionValue('o').trim());
        }
        if (line.hasOption('I')) // item ID (replace or delete)
        {
            imp_id = Integer.parseInt(line.getOptionValue('I').trim());
        }
        if (line.hasOption('E'))
        {
            String batchjob = line.getOptionValue('E').trim();
            EPerson tempBatchJob = EPerson.findByEmail(context, batchjob);
            myLoader.setBatchJob(tempBatchJob);
            if (tempBatchJob == null)
            {
                throw new RuntimeException("User batch job not found");
            }
        }

        if (line.hasOption('m'))
        {
            myLoader.metadataClean = line.getOptionValues('m');
        }
        if (line.hasOption('k'))
        {
            handle = line.getOptionValue('k').trim();
        }
        boolean clearOldBitstream = false;
        if (line.hasOption('b'))
        {
            clearOldBitstream = true;
        }

        if (line.hasOption('R'))
        {
            myLoader.setSourceRef(line.getOptionValue('R'));
        }
        
        // now validate
        // must have a command set
        EPerson currUser = myLoader.batchJob;
        EPerson tempMyEPerson = null;

        if (command == null)
        {
            System.out.println(
                    "Error - must run with either add, replace, or remove (run with -h flag for details)");
            System.exit(1);
        }
        else if (command.equals("add") || command.equals("replace"))
        {
            if (epersonID == -1)
            {
                System.out.println(
                        "Error - an eperson to do the importing must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
            tempMyEPerson = EPerson.find(context, epersonID);
            if (command.equals("add"))
            {
                currUser = tempMyEPerson;
            }
            if (collections == null)
            {
                System.out.println(
                        "Error - at least one destination collection must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
        }
        else if (command.equals("delete"))
        {
            tempMyEPerson = myLoader.batchJob;
        }

        myLoader.setMyEPerson(currUser);

        if (tempMyEPerson == null)
        {
            System.out.println("Error, eperson cannot be found: " + epersonID);
            throw new RuntimeException(
                    "Error, eperson cannot be found: " + epersonID);
        }

        context.setCurrentUser(tempMyEPerson);

        // find collections
        Collection[] mycollections = null;

        // don't need to validate collections set if command is "delete"
        if (!command.equals("delete"))
        {
            System.out.println("Destination collections:");

            mycollections = new Collection[collections.length];

            // validate each collection arg to see if it's a real collection
            for (int i = 0; i < collections.length; i++)
            {
                // is the ID a handle?
                if (collections[i].indexOf('/') != -1)
                {
                    // string has a / so it must be a handle - try and resolve
                    // it
                    mycollections[i] = (Collection) HandleManager
                            .resolveToObject(context, collections[i]);

                    // resolved, now make sure it's a collection
                    if ((mycollections[i] == null) || (mycollections[i]
                            .getType() != Constants.COLLECTION))
                    {
                        mycollections[i] = null;
                    }
                }
                // not a handle, try and treat it as an integer collection
                // database ID
                else if (collections[i] != null)
                {
                    mycollections[i] = Collection.find(context,
                            Integer.parseInt(collections[i].trim()));
                }

                // was the collection valid?
                if (mycollections[i] == null)
                {
                    throw new IllegalArgumentException("Cannot resolve "
                            + collections[i] + " to collection");
                }

                // print progress info
                String owningPrefix = "";

                if (i == 0)
                {
                    owningPrefix = "Owning ";
                }

                System.out.println(owningPrefix + " Collection: "
                        + mycollections[i].getMetadata("name"));
            }
        } // end of validating collections

        try
        {
            context.turnOffAuthorisationSystem();
            if (command.equals("add"))
            {
                item_id = myLoader.addItem(context, mycollections, imp_id,
                        handle, clearOldBitstream);

                if(StringUtils.isNotBlank(myLoader.getSourceRef())) {
                    DatabaseManager.updateQuery(context,
                        "INSERT INTO imp_record_to_item " + "VALUES ( ? , ?,  ?)",
                        imp_record_id, item_id, myLoader.getSourceRef());
                }
                else {
                    DatabaseManager.updateQuery(context,
                            "INSERT INTO imp_record_to_item " + "VALUES ( ? , ?,  null)",
                            imp_record_id, item_id);                    
                }
            }
            else if (command.equals("replace"))
            {
                myLoader.replaceItems(context, mycollections, imp_record_id,
                        item_id, imp_id, clearOldBitstream);
            }
            else if (command.equals("delete")
                    || command.equals("deleteintegra"))
            {
                Item item = Item.find(context, item_id);
                if (item != null)
                {
                    ItemUtils.removeOrWithdrawn(context, item);
                }
                if (command.equals("delete")
                        && (item == null || !item.isWithdrawn()))
                {
                    DatabaseManager.updateQuery(context,
                            "DELETE FROM imp_record_to_item "
                                    + "WHERE imp_record_id = ? AND imp_item_id = ?",
                            imp_record_id, item_id);
                }
            }

            DatabaseManager.updateQuery(context,
                    "UPDATE imp_record " + "SET last_modified = LOCALTIMESTAMP"
                            + " WHERE imp_id = ?",
                    imp_id);
            context.restoreAuthSystemState();
            return item_id;
        }
        catch (RuntimeException e)
        {
            log.warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void replaceItems(Context c, Collection[] mycollections,
            String imp_record_id, int item_id, int imp_id,
            boolean clearOldBitstream) throws Exception
    {

        Item oldItem = Item.find(c, item_id);

        // check item
        if (oldItem == null)
        {
            throw new RuntimeException("No item found with id: " + item_id);
        }

        processItemUpdate(c, imp_id, clearOldBitstream, oldItem);
    }

    private void processItemUpdate(Context c, int imp_id,
            boolean clearOldBitstream, Item item) throws SQLException,
                    AuthorizeException, TransformerException, IOException
    {

        int item_id = item.getID();
        if (metadataClean != null && metadataClean.length > 0)
        {
            for (String mc : metadataClean)
            {
                StringTokenizer dcf = new StringTokenizer(mc.trim(), ".");

                String[] tokens = { "", "", "" };
                int i = 0;
                while (dcf.hasMoreTokens())
                {
                    tokens[i] = dcf.nextToken().trim();
                    i++;
                }
                String schema = tokens[0];
                String element = tokens[1];
                String qualifier = tokens[2];

                if ("*".equals(qualifier))
                {
                    item.clearMetadata(schema, element, Item.ANY, Item.ANY);
                }
                else if ("".equals(qualifier))
                {
                    item.clearMetadata(schema, element, null, Item.ANY);
                }
                else
                {
                    item.clearMetadata(schema, element, qualifier, Item.ANY);
                }
            }
        }
        else
        {
            item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        }

        // now fill out dublin core for item
        loadDublinCore(c, item, imp_id);
        // and the bitstreams
        processImportBitstream(c, item, imp_id, clearOldBitstream);
        
        List<AdditionalMetadataUpdateProcessPlugin> additionalMetadataUpdateProcessPlugins = (List<AdditionalMetadataUpdateProcessPlugin>) dspace
                .getServiceManager().getServicesByType(AdditionalMetadataUpdateProcessPlugin.class);
        for(AdditionalMetadataUpdateProcessPlugin additionalMetadataUpdateProcessPlugin : additionalMetadataUpdateProcessPlugins) {
            additionalMetadataUpdateProcessPlugin.process(c, item, getSourceRef());
        }
        
        item.update();

        if (goToWithdrawn)
        {
            if (item.isArchived())
            {
                ItemUtils.removeOrWithdrawn(c, item);
            }
            else
            {
                throw new RuntimeException("Item corresponding imp_id=" + imp_id
                        + " is not in archive");
            }
        }
        else
        {
            if (goToPublishing)
            {
                if (item.isWithdrawn())
                {
                    item.reinstate();
                }
            }
            // check if item is in workspace status
            TableRow trWsi = DatabaseManager.findByUnique(c, "workspaceitem",
                    "item_id", item_id);
            if (trWsi != null)
            {
                Integer idWsi = trWsi.getIntColumn("workspace_item_id");
                WorkspaceItem wsi = WorkspaceItem.find(c, idWsi);

                if (goToWFStepOne || goToWFStepTwo || goToWFStepThree
                        || goToPublishing)
                {
                    WorkflowItem wfi = WorkflowManager.startWithoutNotify(c,
                            wsi);
                    if ((wfi != null
                            && wfi.getState() == WorkflowManager.WFSTATE_STEP1POOL)
                            && (goToWFStepTwo || goToWFStepThree
                                    || goToPublishing))
                    {

                        WorkflowManager.claim(c, wfi, batchJob);
                        WorkflowManager.advance(c, wfi, batchJob);
                    }
                    if ((wfi != null
                            && wfi.getState() == WorkflowManager.WFSTATE_STEP2POOL)
                            && (goToWFStepThree || goToPublishing))
                    {
                        WorkflowManager.claim(c, wfi, batchJob);
                        WorkflowManager.advance(c, wfi, batchJob);
                    }
                    if ((wfi != null
                            && wfi.getState() == WorkflowManager.WFSTATE_STEP3POOL)
                            && goToPublishing)
                    {
                        WorkflowManager.claim(c, wfi, batchJob);
                        WorkflowManager.advance(c, wfi, batchJob);
                    }
                }
            }
            else if (workspace || goToWFStepOne || goToWFStepTwo
                    || goToWFStepThree || goToPublishing)
            {

                // check if item is in workflow status
                TableRow trWfi = DatabaseManager.findByUnique(c, "workflowitem",
                        "item_id", item_id);
                if (trWfi != null)
                {
                    Integer idWfi = trWfi.getIntColumn("workflow_id");
                    WorkflowItem wfi = WorkflowItem.find(c, idWfi);
                    if (workspace)
                    {
                        WorkflowManager.abort(c, wfi, batchJob);
                    }
                    else
                    {
                        int state = wfi.getState();
                        if (state == WorkflowManager.WFSTATE_STEP1POOL
                                && !goToWFStepOne)
                        {
                            WorkflowManager.claim(c, wfi, batchJob);
                            WorkflowManager.advance(c, wfi, batchJob);
                            if ((wfi != null
                                    && wfi.getState() == WorkflowManager.WFSTATE_STEP2POOL)
                                    && (goToWFStepThree || goToPublishing))
                            {
                                WorkflowManager.claim(c, wfi, batchJob);
                                WorkflowManager.advance(c, wfi, batchJob);
                            }
                            if ((wfi != null
                                    && wfi.getState() == WorkflowManager.WFSTATE_STEP3POOL)
                                    && (goToPublishing))
                            {
                                WorkflowManager.claim(c, wfi, batchJob);
                                WorkflowManager.advance(c, wfi, batchJob);
                            }
                        }
                        else if (state == WorkflowManager.WFSTATE_STEP1)
                        {
                            if (goToWFStepOne)
                            {
                                WorkflowManager.unclaim(c, wfi, wfi.getOwner());
                            }
                            else
                            {
                                WorkflowManager.advance(c, wfi, batchJob);
                                if ((wfi != null
                                        && wfi.getState() == WorkflowManager.WFSTATE_STEP2POOL)
                                        && (goToWFStepThree || goToPublishing))
                                {
                                    WorkflowManager.claim(c, wfi, batchJob);
                                    WorkflowManager.advance(c, wfi, batchJob);
                                }
                                if ((wfi != null
                                        && wfi.getState() == WorkflowManager.WFSTATE_STEP3POOL)
                                        && (goToPublishing))
                                {
                                    WorkflowManager.claim(c, wfi, batchJob);
                                    WorkflowManager.advance(c, wfi, batchJob);
                                }
                            }
                        }
                        else if (state == WorkflowManager.WFSTATE_STEP2POOL
                                && !goToWFStepTwo)
                        {
                            WorkflowManager.claim(c, wfi, batchJob);
                            WorkflowManager.advance(c, wfi, batchJob);

                            if ((wfi != null
                                    && wfi.getState() == WorkflowManager.WFSTATE_STEP3POOL)
                                    && (goToPublishing))
                            {
                                WorkflowManager.claim(c, wfi, batchJob);
                                WorkflowManager.advance(c, wfi, batchJob);
                            }
                            // anomaly control
                            if (goToWFStepOne)
                            {
                                throw new RuntimeException("Error: Item "
                                        + item_id + " in status "
                                        + WorkflowManager.WFSTATE_STEP2POOL
                                        + " no turn back in status "
                                        + WorkflowManager.WFSTATE_STEP1);
                            }
                        }
                        else if (state == WorkflowManager.WFSTATE_STEP2)
                        {
                            if (goToWFStepTwo)
                            {
                                WorkflowManager.unclaim(c, wfi, wfi.getOwner());
                            }
                            else
                            {
                                WorkflowManager.advance(c, wfi, batchJob);
                                if ((wfi != null
                                        && wfi.getState() == WorkflowManager.WFSTATE_STEP3POOL)
                                        && (goToPublishing))
                                {
                                    WorkflowManager.claim(c, wfi, batchJob);
                                    WorkflowManager.advance(c, wfi, batchJob);
                                }
                                // anomaly control
                                if (goToWFStepOne)
                                {
                                    throw new RuntimeException("Error: Item "
                                            + item_id + " in status "
                                            + WorkflowManager.WFSTATE_STEP2POOL
                                            + " no turn back in status "
                                            + WorkflowManager.WFSTATE_STEP1);
                                }
                            }
                        }
                        else if (state == WorkflowManager.WFSTATE_STEP3POOL
                                && !goToWFStepThree)
                        {
                            WorkflowManager.claim(c, wfi, batchJob);
                            WorkflowManager.advance(c, wfi, batchJob);

                            // anomaly control
                            if (goToWFStepOne || goToWFStepTwo)
                            {
                                throw new RuntimeException("Error: Item "
                                        + item_id + " in status "
                                        + WorkflowManager.WFSTATE_STEP3POOL
                                        + " no turn back in status "
                                        + (goToWFStepOne
                                                ? WorkflowManager.WFSTATE_STEP1
                                                : WorkflowManager.WFSTATE_STEP2));
                            }
                        }
                        else if (state == WorkflowManager.WFSTATE_STEP3)
                        {
                            if (goToWFStepThree)
                            {
                                WorkflowManager.unclaim(c, wfi, wfi.getOwner());
                            }
                            else
                            {
                                WorkflowManager.advance(c, wfi, batchJob);
                                // anomaly control
                                if (goToWFStepOne || goToWFStepTwo)
                                {
                                    throw new RuntimeException("Error: Item "
                                            + item_id + " in status "
                                            + WorkflowManager.WFSTATE_STEP3POOL
                                            + " no turn back in status "
                                            + (goToWFStepOne
                                                    ? WorkflowManager.WFSTATE_STEP1
                                                    : WorkflowManager.WFSTATE_STEP2));
                                }
                            }
                        }
                    }
                }
                else
                {
                    // then item is in publish state
                    if (!goToPublishing)
                    {
                        throw new RuntimeException(
                                "Error: Item " + item_id + " in status "
                                        + WorkflowManager.WFSTATE_ARCHIVE
                                        + " no turn back.");
                    }
                    else
                    {
                        item.update();
                    }
                }
            }

            // UPdate visibility
        }
    }

    /**
     * item? try and add it to the archive c mycollection path itemname handle -
     * non-null means we have a pre-defined handle already mapOut - mapfile
     * we're writing
     */
    private int addItem(Context c, Collection[] mycollections, int imp_id,
            String handle, boolean clearOldBitstream) throws Exception
    {

        // gestione richiesta di whithdrawn per item non gi� in archivio
        if (goToWithdrawn)
        {
            throw new RuntimeException("Item corresponding imp_id=" + imp_id
                    + " is not in archive");
        }

        // create workspace item
        Item myitem = null;
        WorkspaceItem wi = null;
        c.setCurrentUser(myEPerson);

        wi = WorkspaceItem.create(c, mycollections[0], false);
        myitem = wi.getItem();

        if (StringUtils.isNotEmpty(handle))
        {
            // se ti arriva allora chiami il service che ti registra l'handle
            IdentifierService identifierService = dspace
                    .getSingletonService(IdentifierService.class);
            identifierService.register(c, myitem, handle);
        }

        // now fill out dublin core for item
        loadDublinCore(c, myitem, imp_id);
        // and the bitstreams
        processImportBitstream(c, myitem, imp_id, clearOldBitstream);
        
        List<AdditionalMetadataUpdateProcessPlugin> additionalMetadataUpdateProcessPlugins = (List<AdditionalMetadataUpdateProcessPlugin>) dspace
                .getServiceManager().getServicesByType(AdditionalMetadataUpdateProcessPlugin.class);
        for(AdditionalMetadataUpdateProcessPlugin additionalMetadataUpdateProcessPlugin : additionalMetadataUpdateProcessPlugins) {
            additionalMetadataUpdateProcessPlugin.process(c, myitem, getSourceRef());
        }
        
        wi.setMultipleFiles(true);
        wi.setMultipleTitles(true);
        wi.setPublishedBefore(true);
        wi.setStageReached(1);
        wi.update();

        if (goToWFStepOne || goToWFStepTwo || goToWFStepThree)
        {
            WorkflowItem wfi = WorkflowManager.startWithoutNotify(c, wi);

            int status = wfi.getState();

            if (status == WorkflowManager.WFSTATE_STEP1POOL
                    && (goToWFStepTwo || goToWFStepThree))
            {
                WorkflowManager.claim(c, wfi, batchJob);
                WorkflowManager.advance(c, wfi, batchJob);
                status = wfi.getState();
            }

            if (status == WorkflowManager.WFSTATE_STEP2POOL && goToWFStepThree)
            {
                WorkflowManager.claim(c, wfi, batchJob);
                WorkflowManager.advance(c, wfi, batchJob);
            }
        }
        else if (goToPublishing)
        {
            WorkflowItem wfi = WorkflowManager.startWithoutNotify(c, wi);

            if ((wfi != null
                    && wfi.getState() == WorkflowManager.WFSTATE_STEP1POOL))
            {
                WorkflowManager.claim(c, wfi, batchJob);
                WorkflowManager.advance(c, wfi, batchJob);
            }
            if ((wfi != null
                    && wfi.getState() == WorkflowManager.WFSTATE_STEP2POOL))
            {
                WorkflowManager.claim(c, wfi, batchJob);
                WorkflowManager.advance(c, wfi, batchJob);
            }
            if ((wfi != null
                    && wfi.getState() == WorkflowManager.WFSTATE_STEP3POOL))
            {
                WorkflowManager.claim(c, wfi, batchJob);
                WorkflowManager.advance(c, wfi, batchJob);
            }

            // Non necessaria perche la registrazione viene effettuata al
            // termine del wkf
            // only process handle file if not using workflow system// only
            // process handle file if not using workflow system
            // InstallItem.installItem(c, wi, null);
            // InstallItem.installItem(c, wfi, handle, false);
        }

        // now add to multiple collections if requested
        if (mycollections.length > 1)
        {
            for (int i = 1; i < mycollections.length; i++)
            {
                mycollections[i].addItem(myitem);
            }
        }

        return myitem.getID();
    }

    private void loadDublinCore(Context c, Item myitem, int imp_id)
            throws SQLException, AuthorizeException, TransformerException
    {
        TableRowIterator retTRI = null;
        TableRow row_data = null;

        String myQuery = "SELECT * FROM imp_metadatavalue WHERE imp_id = ? ORDER BY imp_metadatavalue_id, imp_element, imp_qualifier, metadata_order";

        retTRI = DatabaseManager.query(c, myQuery, imp_id);

        // Add each one as a new format to the registry
        while (retTRI.hasNext())
        {
            row_data = retTRI.next();
            addDCValue(c, myitem, "dc", row_data);
        }
        retTRI.close();
    }

    /**
     * Recupera dal TableRow le informazioni per creare il metadato, per
     * l'authority inserendo il segnaposto "N/A" verr� memorizzato il valore
     * vuoto e quindi il sistema non cercher� di associare un authority in
     * automatico.
     * 
     * @param c
     * @param i
     * @param schema
     * @param n
     * @throws TransformerException
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void addDCValue(Context c, Item i, String schema, TableRow n)
            throws TransformerException, SQLException, AuthorizeException
    {
        String value = n.getStringColumn("imp_value");
        // compensate for empty value getting read as "null", which won't
        // display
        if (value == null)
            value = "";
        String impSchema = n.getStringColumn("imp_schema");
        String element = n.getStringColumn("imp_element");
        String qualifier = n.getStringColumn("imp_qualifier");
        String authority = n.getStringColumn("imp_authority");
        int confidence = n.getIntColumn("imp_confidence");
        String language = "";
        if (StringUtils.isNotBlank(impSchema)) {
        	schema = impSchema;
        }
        language = n.getStringColumn("TEXT_LANG");

        System.out.println("\tSchema: " + schema + " Element: " + element
                + " Qualifier: " + qualifier + " Value: " + value);

        if (qualifier == null || qualifier.equals("none")
                || "".equals(qualifier))
        {
            qualifier = null;
        }

        // if language isn't set, use the system's default value
        if (language == null || language.equals(""))
        {
            language = ConfigurationManager.getProperty("default.language");
        }

        // a goofy default, but there it is
        if (language == null)
        {
            language = "en";
        }

        // let's check that the actual metadata field exists.
        MetadataSchema foundSchema = MetadataSchema.find(c, schema);

        if (foundSchema == null)
        {
            System.out.println("ERROR: schema '" + schema
                    + "' was not found in the registry.");
            return;
        }

        int schemaID = foundSchema.getSchemaID();
        MetadataField foundField = MetadataField.findByElement(c, schemaID,
                element, qualifier);

        if (foundField == null)
        {
            System.out.println(
                    "ERROR: Metadata field: '" + schema + "." + element + "."
                            + qualifier + "' was not found in the registry.");
            return;
        }

        boolean bShare = ConfigurationManager
                .getBooleanProperty("sharepriority." + schema + "." + element
                        + "." + qualifier + ".share");
        int share = -1;
        if (bShare)
        {
            share = n.getIntColumn("IMP_SHARE");
        }

        if (authority != null && authority.equals("N/A"))
        {
            // remove placeholder and insert the value
            authority = null;
            confidence = Choices.CF_UNSET;
            if (bShare)
            {
                //TODO not yet implemented
                // i.addMetadata(schema, element, qualifier, language, value,
                // authority, confidence, share, -1);
            }
            else
            {
                i.addMetadata(schema, element, qualifier, language, value,
                        authority, confidence);
            }
        }
        else if (StringUtils.isNotEmpty(authority))
        {
            if (bShare)
            {
                //TODO not yet implemented
                // i.addMetadata(schema, element, qualifier, language, value,
                // authority, confidence, share, -1);
            }
            else
            {
                i.addMetadata(schema, element, qualifier, language, value,
                        authority, confidence);
            }
        }
        else
        {
            if (bShare)
            {
                //TODO not yet implemented
                // i.addMetadata(schema, element, qualifier, language, value,
                // share);
            }
            else
            {
                i.addMetadata(schema, element, qualifier, language, value);
            }
        }
    }

    /**
     * Import a bitstream to relative item.
     * 
     * @param c
     * @param i
     * @param imp_id
     * @throws AuthorizeException
     * @throws IOException
     * @throws SQLException
     */
    private void processImportBitstream(Context c, Item i, int imp_id,
            boolean clearOldBitstream)
                    throws SQLException, IOException, AuthorizeException
    {

        if (clearOldBitstream)
        {
            Bundle[] bnds = i.getBundles();
            if (bnds != null)
            {
                for (Bundle bundle : bnds)
                {
                    Bitstream[] bts = bundle.getBitstreams();
                    if (bts != null)
                    {
                        for (Bitstream b : bts)
                        {
                            bundle.removeBitstream(b);
                            bundle.update();
                        }
                    }
                    i.removeBundle(bundle);
                }
            }
        }
        // retrieve the attached
        String sql_bs = "SELECT * FROM imp_bitstream WHERE imp_id = ? order by bitstream_order asc";

        TableRowIterator rows_bs_all = DatabaseManager.queryTable(c,
                "imp_bitstream", sql_bs, imp_id);

        while (rows_bs_all.hasNext())
        {
            TableRow imp_bitstream = rows_bs_all.next();

            String filepath = imp_bitstream.getStringColumn("filepath");
            String bundleName = imp_bitstream.getStringColumn("bundle");
            String description = imp_bitstream.getStringColumn("description");
            Boolean primary_bitstream = imp_bitstream
                    .getBooleanColumn("primary_bitstream");

            int assetstore = imp_bitstream.getIntColumn("assetstore");
            System.out.println("\tProcessing contents file: " + filepath);

            String name_file = imp_bitstream.getStringColumn("name");

            String start_date = "";

            // 0: all
            // 1: embargo
            // 2: only an authorized group
            // 3: not visible
            int embargo_policy = imp_bitstream.getIntColumn("embargo_policy");
            String embargo_start_date = imp_bitstream
                    .getStringColumn("embargo_start_date");
            Group embargoGroup = null;
            if (embargo_policy != -1)
            {
                if (embargo_policy == 3)
                {
                    start_date = null;
                    embargoGroup = Group.find(c, 1);
                }
                else if (embargo_policy == 2)
                {
                    embargoGroup = Group.find(c, embargo_policy);
                    if (embargo_start_date != null)
                    {
                        start_date = embargo_start_date;
                    }
                    else
                    {
                        start_date = null;
                    }
                }
                else if (embargo_policy == 1 && embargo_start_date != null)
                {
                    start_date = embargo_start_date;
                }
                else if (embargo_policy == 0)
                {
                    start_date = null;
                }
            }

            int imp_bitstream_id = imp_bitstream.getIntColumn("imp_bitstream_id");
            String valueMD5 = imp_bitstream.getStringColumn("md5value");
            byte[] content = imp_bitstream.getBinaryData("imp_blob");
            Bitstream bs = processBitstreamEntry(c, i, filepath, bundleName,
                    description, primary_bitstream, name_file,
                    assetstore, embargoGroup, start_date, content,
                    valueMD5,imp_bitstream_id);
            
            // HACK: replace the bytea with a register like operation
            if (content != null)
            {
                imp_bitstream.setColumnNull("imp_blob");
                imp_bitstream.setColumn("assetstore", bs.getStoreNumber());
                String assetstorePath;
                if (bs.getStoreNumber() == 0)
                {
                    assetstorePath = ConfigurationManager
                            .getProperty("assetstore.dir") + File.separatorChar;
                }
                else
                {
                    assetstorePath = ConfigurationManager.getProperty(
                            "assetstore.dir." + bs.getStoreNumber())
                            + File.separatorChar;
                }
                int length = assetstorePath.length();
                imp_bitstream.setColumn("filepath",
                        bs.getSource().substring(length));
                DatabaseManager.update(c, imp_bitstream);
            }

        }
        rows_bs_all.close();
    }

    /**
     * Process bitstream
     * 
     * @param c
     * @param i
     * @param bitstreamPath
     * @param bundleName
     * @param description
     * @param license
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    private Bitstream processBitstreamEntry(Context c, Item i,
            String bitstreamPath, String bundleName,
            String description, Boolean primaryBitstream, String name_file,
            int alreadyInAssetstoreNr, Group embargoGroup, String start_date,
            byte[] content, String valueMD5,int imp_bitstream_id)
                    throws SQLException, IOException, AuthorizeException
    {
        String fullpath = null;

        if (alreadyInAssetstoreNr == -1)
        {
            fullpath = bitstreamPath;
        }
        else
        {
            fullpath = ConfigurationManager
                    .getProperty("assetstore.dir." + alreadyInAssetstoreNr)
                    + File.separatorChar + bitstreamPath;
        }

        Bitstream bs = null;
        String newBundleName = bundleName;

        if (bundleName == null || bundleName.length() == 0)
        {
            // is it license.txt?
            if (bitstreamPath.endsWith("license.txt"))
            {
                newBundleName = "LICENSE";
            }
            else
            {
                // call it ORIGINAL
                newBundleName = "ORIGINAL";
            }
        }

        // find the bundle
        Bundle[] bundles = i.getBundles(newBundleName);
        Bundle targetBundle = null;

        if (bundles.length < 1)
        {
            // not found, create a new one
            targetBundle = i.createBundle(newBundleName);
        }
        else
        {
            // put bitstreams into first bundle
            targetBundle = bundles[0];
        }

        // get an input stream
        if (alreadyInAssetstoreNr == -1)
        {
            InputStream bis;
            if (content != null)
            {
                bis = new ByteArrayInputStream(content);
            }
            else
            {
                bis = new BufferedInputStream(new FileInputStream(fullpath));
            }

            // now add the bitstream
            bs = targetBundle.createBitstream(bis);
        }
        else
        {
            bs = targetBundle.registerBitstream(alreadyInAssetstoreNr,
                    bitstreamPath, valueMD5 == null);
            if (valueMD5 != null)
            {
                bs.setMD5Value(valueMD5);
            }
        }

        bs.setDescription(description);
        if (primaryBitstream)
        {
            targetBundle.setPrimaryBitstreamID(bs.getID());
        }
        if (name_file != null)
            bs.setName(name_file);
        else
            bs.setName(new File(fullpath).getName());

        // Identify the format
        // FIXME - guessing format guesses license.txt incorrectly as a text
        // file format!
        BitstreamFormat bf = FormatIdentifier.guessFormat(c, bs);
        bs.setFormat(bf);
        if (bitstreamPath != null)
        {
            bs.setSource(bitstreamPath);
        }
        else
        {
            bs.setSource(BitstreamStorageManager.absolutePath(c, bs.getID()));
        }

        if (embargoGroup == null) {
            embargoGroup = Group.find(c, 0);
        }
        Date embargoDate = null;
        if (StringUtils.isNotBlank(start_date)) {
            String[] split_date = start_date.split("/");
            int embargo_year = Integer.parseInt(split_date[2]);
            int embargo_month = Integer.parseInt(split_date[1]);
            int embargo_day = Integer.parseInt(split_date[0]);
            if (embargo_year > 0 && embargo_month > 0 && embargo_day > 0) {
                Calendar cal = Calendar.getInstance();
                embargo_month--;
                cal.set(embargo_year, embargo_month, embargo_day, 0, 0, 0);
                embargoDate = cal.getTime();
            }
        }
        AuthorizeManager.removeAllPoliciesByDSOAndType(c, bs,
                ResourcePolicy.TYPE_CUSTOM);
        AuthorizeManager.removeAllPoliciesByDSOAndType(c, bs,
                ResourcePolicy.TYPE_INHERITED);
        ResourcePolicy rp = ResourcePolicy.create(c);
        rp.setResource(bs);
        rp.setAction(Constants.READ);
        rp.setRpType(ResourcePolicy.TYPE_CUSTOM);
        rp.setGroup(embargoGroup);
        rp.setStartDate(embargoDate);
        rp.update();
     
        processBitstreamMetadata(c,bs,imp_bitstream_id);
        bs.update();
        return bs;
    }

    private void processBitstreamMetadata(Context c,Bitstream b,int imp_bitstream_id) throws SQLException{
    	String myQuery = "SELECT * FROM imp_bitstream_metadatavalue WHERE imp_bitstream_id = ? ORDER BY imp_bitstream_metadatavalue_id,imp_schema, imp_element, imp_qualifier, metadata_order";
    	TableRowIterator tri= DatabaseManager.queryTable(c, "imp_bitstream_metadatavalue",myQuery, imp_bitstream_id);
    	
    	while(tri.hasNext()){
    		TableRow tr = tri.next(c);
    		String schema = tr.getStringColumn("imp_schema");
    		String element = tr.getStringColumn("imp_element");
    		String qualifier = tr.getStringColumn("imp_qualifier");
    		String value = tr.getStringColumn("imp_value");
    		String authority = tr.getStringColumn("imp_authority");
    		int confidence = tr.getIntColumn("imp_confidence");
            String language = tr.getStringColumn("text_lang");

            System.out.println("\tSchema: " + schema + " Element: " + element
                    + " Qualifier: " + qualifier + " Value: " + value);

            if (!StringUtils.isNotBlank(qualifier) || StringUtils.equals("none",qualifier))
            {
                qualifier = null;
            }

            if(!StringUtils.isNotBlank(authority) || StringUtils.equalsIgnoreCase("N/A", authority)){
            	authority= null;
            	confidence=Choices.CF_UNSET;
            }
            // if language isn't set, use the system's default value
            if (!StringUtils.isNotBlank(language))
            {
                language = Item.ANY;
            }
            
    		b.addMetadata(schema, element, qualifier,language, value, authority, confidence);
    	}
    	
    	tri.close();
    }
    
    public void setGoToWFStepOne(boolean goToWFStepOne)
    {
        this.goToWFStepOne = goToWFStepOne;
    }

    public void setGoToWFStepTwo(boolean goToWFStepTwo)
    {
        this.goToWFStepTwo = goToWFStepTwo;
    }

    public void setGoToWFStepThree(boolean goToWFStepThree)
    {
        this.goToWFStepThree = goToWFStepThree;
    }

    public void setGoToPublishing(boolean goToPublishing)
    {
        this.goToPublishing = goToPublishing;
    }

    public void setGoToWithdrawn(boolean goToWithdrawn)
    {
        this.goToWithdrawn = goToWithdrawn;
    }

    public void setWorkspace(boolean workspace)
    {
        this.workspace = workspace;
    }

    public void setMyEPerson(EPerson myEPerson)
    {
        this.myEPerson = myEPerson;
    }

    public void setBatchJob(EPerson batchJob)
    {
        this.batchJob = batchJob;
    }

    public String getSourceRef()
    {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef)
    {
        this.sourceRef = sourceRef.trim();
    }

}