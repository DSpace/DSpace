/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.batch;

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
import java.util.UUID;
import javax.mail.MessagingException;
import javax.xml.transform.TransformerException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.batch.ImpBitstream;
import org.dspace.batch.ImpBitstreamMetadatavalue;
import org.dspace.batch.ImpMetadatavalue;
import org.dspace.batch.ImpRecord;
import org.dspace.batch.ImpRecordToItem;
import org.dspace.batch.ImpWorkflowNState;
import org.dspace.batch.service.ImpBitstreamMetadatavalueService;
import org.dspace.batch.service.ImpBitstreamService;
import org.dspace.batch.service.ImpMetadatavalueService;
import org.dspace.batch.service.ImpRecordService;
import org.dspace.batch.service.ImpRecordToItemService;
import org.dspace.batch.service.ImpServiceFactory;
import org.dspace.batch.service.ImpWorkflowNStateService;
import org.dspace.content.AdditionalMetadataUpdateProcessPlugin;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.ItemUtils;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;

/**
 * Import from the imp_* table in the DSpace data model. Because the script
 * manage the history we suggest to run this script form
 * {@link ItemImportMainOA} Available operation: -a to build item -r to update
 * (List of metadata to remove first and after do an update [by default all
 * metadata are delete, specifying only the dc.title it will obtain an append on
 * the other metadata]; use this option many times on the single metadata e.g.
 * -m dc.title -m dc.contributor.*) -d to remove the item
 * 
 * Status changes: -p to send in workspace -w to send in workspace step one -y
 * to send in workspace step two -x to send in workspace step three -z to send
 * inarchive
 * 
 * 
 * Call the script with the option -h to discover more setting.
 * 
 * <em>For massive import see {@link ItemImportMainOA}</em>
 */
public class ItemImportOA {

    private DSpace dspace = new DSpace();

    /** logger */
    private static Logger log = Logger.getLogger(ItemImportOA.class);

    private boolean workflow = false;

    private boolean reinstate = false;

    private boolean withdrawn = false;

    private boolean backToWorkspace = false;

    private EPerson myEPerson = null;

    private EPerson batchJob = null;

    private String[] metadataClean = null;

    private String sourceRef = null;

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
    private MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance()
            .getMetadataSchemaService();
    private BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
            .getBitstreamFormatService();
    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    private HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    private IdentifierService identifierService = IdentifierServiceFactory.getInstance().getIdentifierService();
    private EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private XmlWorkflowService workflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
    private XmlWorkflowItemService workflowItemService = XmlWorkflowServiceFactory.getInstance()
            .getXmlWorkflowItemService();

    private PoolTaskService poolTaskService = XmlWorkflowServiceFactory.getInstance().getPoolTaskService();
    private ClaimedTaskService claimedTaskService = XmlWorkflowServiceFactory.getInstance().getClaimedTaskService();

    private ImpBitstreamService impBitstreamService = ImpServiceFactory.getInstance().getImpBitstreamService();
    private ImpBitstreamMetadatavalueService impBitstreamMetadatavalueService = ImpServiceFactory.getInstance()
            .getImpBitstreamMetadatavalueService();
    private ImpMetadatavalueService impMetadatavalueService = ImpServiceFactory.getInstance()
            .getImpMetadatavalueService();
    private ImpRecordService impRecordService = ImpServiceFactory.getInstance().getImpRecordService();
    private ImpRecordToItemService impRecordToItemService = ImpServiceFactory.getInstance().getImpRecordToItemService();
    private ImpWorkflowNStateService impWorkflowNStateService = ImpServiceFactory.getInstance()
            .getImpWorkflowNStateService();

    public static void main(String[] argv) {
        Context context = null;

        try {
            context = new Context();
            new ItemImportOA().impRecord(context, argv);
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException();
        } finally {
            if (context != null && context.isValid()) {
                context.abort();
            }
        }
    }

    public UUID impRecord(Context context, String[] argv) throws Exception {
        // instantiate loader
        ItemImportOA myLoader = new ItemImportOA();

        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("a", "add", false, "add items to DSpace");
        options.addOption("b", "bitstream", false, "clear old bitstream");
        options.addOption("c", "collection", true, "destination collection(s) Handle or database ID");
        options.addOption("d", "delete", false, "delete items");
        options.addOption("e", "eperson", true, "uuid eperson doing importing");
        options.addOption("E", "batch_user", true, "user batch job");
        options.addOption("g", "withdrawn", false, "set item in withdrawn state");
        options.addOption("h", "help", false, "help");
        options.addOption("i", "record", true, "record ID");
        options.addOption("I", "importID", true, "import ID");
        options.addOption("k", "handle", true, "handle of item");
        options.addOption("m", "metadata", true,
                "List of metadata to remove first and after do an update [by default all metadata are delete,"
                        + " specifying only the dc.title it will obtain an append on the other metadata];"
                        + " use this option many times on the single metadata e.g. -m dc.title -m dc.contributor.*");
        options.addOption("o", "item", true, "item UUID");
        options.addOption("p", "workspace", false, "send submission back to workspace");
        options.addOption("r", "replace", false, "update items");
        options.addOption("w", "workflow", false, "send submission through collection's workflow");
        options.addOption("z", "reinstate", false, "Reinstate a withdrawn item");
        options.addOption("R", "sourceref", true, "name of the source");

        CommandLine line = parser.parse(options, argv);

        String command = null; // add replace remove, etc
        UUID epersonID = null;
        String[] collections = null;
        String handle = null;
        String imp_record_id = null;
        UUID item_id = null;
        ImpRecord imp_id = null;

        if (line.hasOption('h')) {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ItemImport\n", options);
            System.out.println("adding items: ItemImport -a -e eperson -c collection");
            System.out.println("replacing items: ItemImport -r -e eperson -c collection");
            System.out.println("deleting items: ItemImport -d -e eperson");
            System.out.println(
                    "If multiple collections are specified, the first collection will be the one that owns the item.");

            System.exit(0);
        }

        if (line.hasOption('a')) {
            command = "add";
        }

        if (line.hasOption('r')) {
            command = "replace";
        }

        if (line.hasOption('d')) {
            command = "delete";
        }

        if (line.hasOption('w')) {
            myLoader.setWorkflow(true);
        }

        if (line.hasOption('g')) {
            myLoader.setWithdrawn(true);
        }

        if (line.hasOption('z')) {
            myLoader.setReinstate(true);
        }

        if (line.hasOption('p')) {
            myLoader.setBackToWorkspace(true);
        }
        // person
        if (line.hasOption('e')) {
            epersonID = UUID.fromString(line.getOptionValue('e').trim());
        }
        // collections
        if (line.hasOption('c')) {
            collections = line.getOptionValues('c');
        }
        // record ID
        if (line.hasOption('i')) {
            imp_record_id = line.getOptionValue('i').trim();
        }
        // item ID (replace or delete)
        if (line.hasOption('o')) {
            item_id = UUID.fromString(line.getOptionValue('o').trim());
        }
        // item ID (replace or delete)
        if (line.hasOption('I')) {
            int temp_imp_id = Integer.parseInt(line.getOptionValue('I').trim());
            imp_id = impRecordService.findByID(context, temp_imp_id);
        }
        if (line.hasOption('E')) {
            String batchjob = line.getOptionValue('E').trim();
            EPerson tempBatchJob = epersonService.findByEmail(context, batchjob);

            myLoader.setBatchJob(tempBatchJob);
            if (tempBatchJob == null) {
                throw new RuntimeException("User batch job not found");
            }
        }

        if (line.hasOption('m')) {
            myLoader.metadataClean = line.getOptionValues('m');
        }
        if (line.hasOption('k')) {
            handle = line.getOptionValue('k').trim();
        }
        boolean clearOldBitstream = false;
        if (line.hasOption('b')) {
            clearOldBitstream = true;
        }

        if (line.hasOption('R')) {
            myLoader.setSourceRef(line.getOptionValue('R'));
        }

        // now validate
        // must have a command set
        EPerson currUser = myLoader.batchJob;
        EPerson tempMyEPerson = null;

        if (command == null) {
            System.out.println("Error - must run with either add, replace, or remove (run with -h flag for details)");
            System.exit(1);
        } else if (command.equals("add") || command.equals("replace")) {
            if (epersonID == null) {
                System.out.println("Error - an eperson to do the importing must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
            tempMyEPerson = epersonService.find(context, epersonID);
            if (command.equals("add")) {
                currUser = tempMyEPerson;
            }
            if (collections == null) {
                System.out.println("Error - at least one destination collection must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
        } else if (command.equals("delete")) {
            tempMyEPerson = myLoader.batchJob;
        }

        myLoader.setMyEPerson(currUser);

        if (tempMyEPerson == null) {
            System.out.println("Error, eperson cannot be found: " + epersonID);
            throw new RuntimeException("Error, eperson cannot be found: " + epersonID);
        }

        context.setCurrentUser(tempMyEPerson);

        // find collections
        Collection[] mycollections = null;

        // don't need to validate collections set if command is "delete"
        if (!command.equals("delete")) {
            System.out.println("Destination collections:");

            mycollections = new Collection[collections.length];

            // validate each collection arg to see if it's a real collection
            for (int i = 0; i < collections.length; i++) {
                // is the ID a handle?
                if (collections[i].indexOf('/') != -1) {
                    // string has a / so it must be a handle - try and resolve
                    // it
                    mycollections[i] = (Collection) handleService.resolveToObject(context, collections[i]);

                    // resolved, now make sure it's a collection
                    if ((mycollections[i] == null) || (mycollections[i].getType() != Constants.COLLECTION)) {
                        mycollections[i] = null;
                    }
                } else if (collections[i] != null) {
                    // not a handle, try and treat it as an integer collection
                    // database ID
                    mycollections[i] = collectionService.find(context, UUID.fromString(collections[i].trim()));
                }

                // was the collection valid?
                if (mycollections[i] == null) {
                    throw new IllegalArgumentException("Cannot resolve " + collections[i] + " to collection");
                }

                // print progress info
                String owningPrefix = "";

                if (i == 0) {
                    owningPrefix = "Owning ";
                }

                System.out.println(
                        owningPrefix + " Collection: " + collectionService.getMetadata(mycollections[i], "name"));
            }
        } // end of validating collections

        try {
            context.turnOffAuthorisationSystem();
            if (command.equals("add")) {
                item_id = myLoader.addItem(context, mycollections, imp_id, handle, clearOldBitstream);
                ImpRecordToItem impRecordToItem = null;

                if (StringUtils.isNotBlank(myLoader.getSourceRef())) {
                    impRecordToItem = new ImpRecordToItem();
                    impRecordToItem.setImpRecordId(imp_record_id);
                    impRecordToItem.setImpItemId(item_id);
                    impRecordToItem.setImpSourceref(myLoader.getSourceRef());
                } else {
                    impRecordToItem = new ImpRecordToItem();
                    impRecordToItem.setImpRecordId(imp_record_id);
                    impRecordToItem.setImpItemId(item_id);
                }
                impRecordToItem = impRecordToItemService.create(context, impRecordToItem);
            } else if (command.equals("replace")) {
                myLoader.replaceItems(context, mycollections, imp_record_id, item_id, imp_id, clearOldBitstream);
            } else if (command.equals("delete")) {
                Item item = itemService.find(context, item_id);
                if (item != null) {
                    ItemUtils.removeOrWithdrawn(context, item);
                }
                if (command.equals("delete") && (item == null || !item.isWithdrawn())) {
                    ImpRecordToItem impRecordToItem = impRecordToItemService.findByPK(context, imp_record_id);
                    impRecordToItemService.delete(context, impRecordToItem);
                    impRecordToItem = null;
                }
            }
            imp_id.setLastModified(new Date());
            impRecordService.update(context, imp_id);
            context.restoreAuthSystemState();
            return item_id;
        } catch (RuntimeException e) {
            log.warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void replaceItems(Context c, Collection[] mycollections, String imp_record_id, UUID item_id,
            ImpRecord imp_id, boolean clearOldBitstream) throws Exception {

        Item oldItem = itemService.find(c, item_id);

        // check item
        if (oldItem == null) {
            throw new RuntimeException("No item found with id: " + item_id);
        }

        processItemUpdate(c, imp_id, clearOldBitstream, oldItem);
    }

    private void processItemUpdate(Context c, ImpRecord imp_id, boolean clearOldBitstream, Item item)
            throws SQLException, AuthorizeException, TransformerException, IOException, WorkflowException,
            WorkflowConfigurationException, MessagingException {

        if (metadataClean != null && metadataClean.length > 0) {
            for (String mc : metadataClean) {
                StringTokenizer dcf = new StringTokenizer(mc.trim(), ".");

                String[] tokens = { "", "", "" };
                int i = 0;
                while (dcf.hasMoreTokens()) {
                    tokens[i] = dcf.nextToken().trim();
                    i++;
                }
                String schema = tokens[0];
                String element = tokens[1];
                String qualifier = tokens[2];

                if ("*".equals(qualifier)) {
                    itemService.clearMetadata(c, item, schema, element, Item.ANY, Item.ANY);
                } else if ("".equals(qualifier)) {
                    itemService.clearMetadata(c, item, schema, element, null, Item.ANY);
                } else {
                    itemService.clearMetadata(c, item, schema, element, qualifier, Item.ANY);
                }
            }
        } else {
            itemService.clearMetadata(c, item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        }

        // now fill out dublin core for item
        loadDublinCore(c, item, imp_id);
        // and the bitstreams
        processImportBitstream(c, item, imp_id, clearOldBitstream);

        List<AdditionalMetadataUpdateProcessPlugin> plugins = dspace.getServiceManager()
                .getServicesByType(AdditionalMetadataUpdateProcessPlugin.class);
        for (AdditionalMetadataUpdateProcessPlugin plugin : plugins) {
            plugin.process(c, item, getSourceRef());
        }

        itemService.update(c, item);

        if (withdrawn) {
            if (item.isArchived()) {
                ItemUtils.removeOrWithdrawn(c, item);
            } else {
                throw new RuntimeException("Item corresponding imp_id=" + imp_id + " is not in archive");
            }
        } else {
            if (reinstate) {
                if (item.isWithdrawn()) {
                    itemService.reinstate(c, item);
                }
            }
            // check if item is in workspace status
            WorkspaceItem wsi = workspaceItemService.findByItem(c, item);
            if (wsi != null && workflow) {

                if (workflow) {
                    XmlWorkflowItem wfi = workflowService.startWithoutNotify(c, wsi);

                    processWorkflow(c, wfi, imp_id);
                }
            } else if (backToWorkspace || workflow) {

                // check if item is in workflow status
                XmlWorkflowItem wfi = workflowItemService.findByItem(c, item);

                if (wfi != null) {
                    if (backToWorkspace) {
                        workflowService.abort(c, wfi, batchJob);
                    } else {

                        processWorkflow(c, wfi, imp_id);
                    }
                }
            }

            // UPdate visibility
        }
    }

    /**
     * item? try and add it to the archive c mycollection path itemname handle -
     * non-null means we have a pre-defined handle already mapOut - mapfile we're
     * writing
     */
    private UUID addItem(Context c, Collection[] mycollections, ImpRecord imp_id, String handle,
            boolean clearOldBitstream) throws Exception {

        // hanlde withdraw
        if (withdrawn) {
            throw new RuntimeException("Item corresponding imp_id=" + imp_id + " is not in archive");
        }

        // create workspace item
        Item myitem = null;
        WorkspaceItem wi = null;
        c.setCurrentUser(myEPerson);

        wi = workspaceItemService.create(c, mycollections[0], false);
        myitem = wi.getItem();

        if (StringUtils.isNotEmpty(handle)) {
            identifierService.register(c, myitem, handle);
        }

        // now fill out dublin core for item
        loadDublinCore(c, myitem, imp_id);
        // and the bitstreams
        processImportBitstream(c, myitem, imp_id, clearOldBitstream);

        List<AdditionalMetadataUpdateProcessPlugin> plugins = DSpaceServicesFactory.getInstance().getServiceManager()
                .getServicesByType(AdditionalMetadataUpdateProcessPlugin.class);
        for (AdditionalMetadataUpdateProcessPlugin plugin : plugins) {
            plugin.process(c, myitem, getSourceRef());
        }

        wi.setMultipleFiles(true);
        wi.setMultipleTitles(true);
        wi.setPublishedBefore(true);
        wi.setStageReached(1);
        workspaceItemService.update(c, wi);

        if (workflow) {
            XmlWorkflowItem wfi = workflowService.startWithoutNotify(c, wi);
            processWorkflow(c, wfi, imp_id);
        }

        // now add to multiple collections if requested
        if (mycollections.length > 1) {
            for (int i = 1; i < mycollections.length; i++) {
                collectionService.addItem(c, mycollections[i], myitem);
            }
        }

        return myitem.getID();
    }

    private void processWorkflow(Context context, XmlWorkflowItem wfi, ImpRecord imp_id) throws SQLException,
            IOException, AuthorizeException, WorkflowConfigurationException, MessagingException, WorkflowException {
        List<ImpWorkflowNState> impWorkflowNStates = impWorkflowNStateService.searchWorkflowOps(context, imp_id);
        for (ImpWorkflowNState iwns : impWorkflowNStates) {
            EPerson user = null;
            if (iwns.getImpWNStateEpersonUuid() != null) {
                user = epersonService.find(context, iwns.getImpWNStateEpersonUuid());
            }
            if (user == null) {
                user = batchJob;
            }

            if ("CLAIM".equalsIgnoreCase(iwns.getImpWNStateOp())) {
                PoolTask task = poolTaskService.findByWorkflowIdAndEPerson(context, wfi, user);

                XmlWorkflowServiceFactory factory = (XmlWorkflowServiceFactory) XmlWorkflowServiceFactory.getInstance();
                Workflow workflow = factory.getWorkflowFactory().getWorkflow(task.getWorkflowItem().getCollection());
                Step step = workflow.getStep(task.getStepID());
                WorkflowActionConfig currentActionConfig = step.getActionConfig(task.getActionID());
                workflowService.doState(context, user, null, task.getWorkflowItem().getID(), workflow,
                        currentActionConfig);
            } else if ("ADVANCE".equalsIgnoreCase(iwns.getImpWNStateOp())) {
                ClaimedTask claimedTask = claimedTaskService.findByWorkflowIdAndEPerson(context, wfi, user);

                XmlWorkflowServiceFactory factory = (XmlWorkflowServiceFactory) XmlWorkflowServiceFactory.getInstance();
                Workflow workflow = factory.getWorkflowFactory()
                        .getWorkflow(claimedTask.getWorkflowItem().getCollection());
                Step step = workflow.getStep(claimedTask.getStepID());
                WorkflowActionConfig currentActionConfig = step.getActionConfig(claimedTask.getActionID());

                // TODO: a) define a HttpServeltRequest mock or make an http call.
                // @See
                // https://github.com/DSpace/Rest7Contract/blob/master/claimedtasks.md#post-method-single-resource-level
                // and org.dspace.app.rest.TaskRestRepositoriesIT#approvalTest.
                //
                // Use org.jmockit.HttpServletRequest?. The dependency with "org.jmockit",
                // "jmockit" exist in pom.xml with test scope.
                workflowService.doState(context, user, null, claimedTask.getWorkflowItem().getID(), workflow,
                        currentActionConfig);
            } else if ("UNCLAIM".equalsIgnoreCase(iwns.getImpWNStateOp())) {
                workflowService.sendWorkflowItemBackSubmission(context, wfi, user, imp_id.getImpSourceref(), "");
            } else if ("ABORT".equalsIgnoreCase(iwns.getImpWNStateOp())) {
                workflowService.abort(context, wfi, user);
            }
        }
    }

    private void loadDublinCore(Context c, Item myitem, ImpRecord imp_id)
            throws SQLException, AuthorizeException, TransformerException {
        List<ImpMetadatavalue> impMetadatavlues = impMetadatavalueService.searchByImpRecordId(c, imp_id);
        // Add each one as a new format to the registry
        for (ImpMetadatavalue row_data : impMetadatavlues) {
            addDCValue(c, myitem, "dc", row_data);
        }
    }

    /**
     * Get the information from the TableRow for the specific metadata. The
     * authority column can use the special value [GUESS], case insensitive, to rely
     * on the getBestMatch to guess a potential authority
     * 
     * @param c
     * @param i
     * @param schema
     * @param n
     * @throws TransformerException
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void addDCValue(Context c, Item i, String schema, ImpMetadatavalue n)
            throws TransformerException, SQLException, AuthorizeException {
        String value = n.getImpValue();
        // compensate for empty value getting read as "null", which won't
        // display
        if (value == null) {
            value = "";
        }
        String impSchema = n.getImpSchema();
        String element = n.getImpElement();
        String qualifier = n.getImpQualifier();
        String authority = n.getImpAuthority();
        int confidence = n.getImpConfidence();
        String language = "";
        if (StringUtils.isNotBlank(impSchema)) {
            schema = impSchema;
        }
        language = n.getTextLang();

        System.out.println(
                "\tSchema: " + schema + " Element: " + element + " Qualifier: " + qualifier + " Value: " + value);

        if (qualifier == null || qualifier.equals("none") || "".equals(qualifier)) {
            qualifier = null;
        }

        // if language isn't set, use the system's default value
        if (language == null || language.equals("")) {
            language = configurationService.getProperty("default.language");
        }

        // a goofy default, but there it is
        if (language == null) {
            language = "en";
        }

        // let's check that the actual metadata field exists.
        MetadataSchema foundSchema = metadataSchemaService.find(c, schema);

        if (foundSchema == null) {
            System.out.println("ERROR: schema '" + schema + "' was not found in the registry.");
            return;
        }

        MetadataField foundField = metadataFieldService.findByElement(c, foundSchema, element, qualifier);

        if (foundField == null) {
            System.out.println("ERROR: Metadata field: '" + schema + "." + element + "." + qualifier
                    + "' was not found in the registry.");
            return;
        }

        if (authority != null && authority.equalsIgnoreCase("[GUESS]")) {
            // remove placeholder
            authority = null;
            itemService.addMetadata(c, i, schema, element, qualifier, language, value);
        } else {
            itemService.addMetadata(c, i, schema, element, qualifier, language, value, authority, confidence);
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
    private void processImportBitstream(Context c, Item i, ImpRecord imp_id, boolean clearOldBitstream)
            throws SQLException, IOException, AuthorizeException {

        if (clearOldBitstream) {
            List<Bundle> bnds = i.getBundles();
            if (bnds != null) {
                for (Bundle bundle : bnds) {
                    List<Bitstream> bts = bundle.getBitstreams();
                    if (bts != null) {
                        for (Bitstream b : bts) {
                            bundle.removeBitstream(b);
                            bundleService.update(c, bundle);
                        }
                    }
                    itemService.removeBundle(c, i, bundle);
                }
            }
        }
        // retrieve the attached
        List<ImpBitstream> impBitstreams = impBitstreamService.searchByImpRecord(c, imp_id);

        for (ImpBitstream imp_bitstream : impBitstreams) {
            String filepath = imp_bitstream.getFilepath();
            String bundleName = imp_bitstream.getBundle();
            String description = imp_bitstream.getDescription();
            Boolean primary_bitstream = imp_bitstream.getPrimaryBitstream();

            int assetstore = imp_bitstream.getAssetstore();
            System.out.println("\tProcessing contents file: " + filepath);

            String name_file = imp_bitstream.getName();

            String start_date = "";

            // 0: all
            // 1: embargo
            // 2: only authorized group
            // 3: not visible
            int embargo_policy = imp_bitstream.getEmbargoPolicy();
            String embargo_start_date = imp_bitstream.getEmbargoStartDate();
            Group embargoGroup = null;
            if (embargo_policy != -1) {
                if (embargo_policy == ImpBitstream.NOT_VISIBLE) {
                    start_date = null;
                    embargoGroup = groupService.findByName(c, Group.ADMIN);
                } else if (embargo_policy == ImpBitstream.USE_GROUP) {
                    try {
                        embargoGroup = groupService.find(c, imp_bitstream.getEmbargoGroup());
                        if (embargo_start_date != null) {
                            start_date = embargo_start_date;
                        } else {
                            start_date = null;
                        }
                    } catch (Exception e) {
                        throw new SQLException(
                                "The group with UUID " + imp_bitstream.getEmbargoGroup() + " does not exist", e);
                    }
                } else if (embargo_policy == ImpBitstream.EMBARGO && embargo_start_date != null) {
                    start_date = embargo_start_date;
                } else if (embargo_policy == ImpBitstream.ALL) {
                    start_date = null;
                }
            }

            Bitstream bs = processBitstreamEntry(c, i, filepath, bundleName, description, primary_bitstream, name_file,
                    assetstore, embargoGroup, start_date, imp_bitstream);

            // HACK: replace the bytea with a register like operation
            if (imp_bitstream.getImpBlob() != null) {
                imp_bitstream.setImpBlob(null);
                imp_bitstream.setAssetstore(bs.getStoreNumber());
                String assetstorePath;
                if (bs.getStoreNumber() == 0) {
                    assetstorePath = configurationService.getProperty("assetstore.dir") + File.separatorChar;
                } else {
                    assetstorePath = configurationService.getProperty("assetstore.dir." + bs.getStoreNumber())
                            + File.separatorChar;
                }
                int length = assetstorePath.length();
                imp_bitstream.setFilepath(bs.getSource().substring(length));
                impBitstreamService.update(c, imp_bitstream);
            }

        }
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
    private Bitstream processBitstreamEntry(Context c, Item i, String bitstreamPath, String bundleName,
            String description, Boolean primaryBitstream, String name_file, int alreadyInAssetstoreNr,
            Group embargoGroup, String start_date, ImpBitstream imp_bitstream_id)
            throws SQLException, IOException, AuthorizeException {
        String fullpath = null;

        if (alreadyInAssetstoreNr == -1) {
            fullpath = bitstreamPath;
        } else {
            fullpath = configurationService.getProperty("assetstore.dir." + alreadyInAssetstoreNr) + File.separatorChar
                    + bitstreamPath;
        }

        Bitstream bs = null;
        String newBundleName = bundleName;

        if (bundleName == null || bundleName.length() == 0) {
            // is it license.txt?
            if (bitstreamPath.endsWith("license.txt")) {
                newBundleName = "LICENSE";
            } else {
                // call it ORIGINAL
                newBundleName = "ORIGINAL";
            }
        }

        // find the bundle
        List<Bundle> bundles = i.getBundles(newBundleName);
        Bundle targetBundle = null;

        if (bundles.isEmpty()) {
            // not found, create a new one
            targetBundle = bundleService.create(c, i, newBundleName);
        } else {
            // put bitstreams into first bundle
            targetBundle = bundles.get(0);
        }

        // get an input stream
        if (alreadyInAssetstoreNr == -1) {
            InputStream bis;
            if (imp_bitstream_id.getImpBlob() != null) {
                bis = new ByteArrayInputStream(imp_bitstream_id.getImpBlob());
            } else {
                bis = new BufferedInputStream(new FileInputStream(fullpath));
            }

            // now add the bitstream
            bs = bitstreamService.create(c, bis);
            bundleService.addBitstream(c, targetBundle, bs);
        } else {
            bs = bitstreamService.register(c, alreadyInAssetstoreNr, bitstreamPath);
            if (imp_bitstream_id.getMd5value() != null) {
                bs.setChecksumAlgorithm("MD5");
                bs.setChecksum(imp_bitstream_id.getMd5value());
            }
        }

        bs.setDescription(c, description);
        if (primaryBitstream) {
            targetBundle.setPrimaryBitstreamID(bs);
        }
        if (name_file != null) {
            bs.setName(c, name_file);
        } else {
            bs.setName(c, new File(fullpath).getName());
        }

        // Identify the format
        // FIXME - guessing format guesses license.txt incorrectly as a text
        // file format!
        BitstreamFormat bf = bitstreamFormatService.guessFormat(c, bs);
        bitstreamService.setFormat(c, bs, bf);

        if (bitstreamPath != null) {
            bs.setSource(c, bitstreamPath);
        } else {
            bs.setSource(c, bs.getInternalId());
        }

        if (embargoGroup == null) {
            embargoGroup = groupService.findByName(c, Group.ANONYMOUS);
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
                cal.clear();
                cal.set(embargo_year, embargo_month, embargo_day, 0, 0, 0);
                embargoDate = cal.getTime();
            }
        }
        authorizeService.removeAllPolicies(c, bs);
        ResourcePolicy rp = authorizeService.createResourcePolicy(c, bs, embargoGroup, null,
                Constants.DEFAULT_BITSTREAM_READ, null, null, null, embargoDate, null);

        processBitstreamMetadata(c, bs, imp_bitstream_id);
        bitstreamService.update(c, bs);
        return bs;
    }

    private void processBitstreamMetadata(Context c, Bitstream b, ImpBitstream imp_bitstream_id) throws SQLException {
        List<ImpBitstreamMetadatavalue> impBitstreamMetadatavalues = impBitstreamMetadatavalueService
                .searchByImpBitstream(c, imp_bitstream_id);

        for (ImpBitstreamMetadatavalue tr : impBitstreamMetadatavalues) {
            String schema = tr.getImpSchema();
            String element = tr.getImpElement();
            String qualifier = tr.getImpQualifier();
            String value = tr.getImpValue();
            String authority = tr.getImpAuthority();
            int confidence = tr.getImpConfidence();
            String language = tr.getTextLang();

            System.out.println(
                    "\tSchema: " + schema + " Element: " + element + " Qualifier: " + qualifier + " Value: " + value);

            if (!StringUtils.isNotBlank(qualifier) || StringUtils.equals("none", qualifier)) {
                qualifier = null;
            }

            if (!StringUtils.isNotBlank(authority) || StringUtils.equalsIgnoreCase("N/A", authority)) {
                authority = null;
                confidence = Choices.CF_UNSET;
            }
            // if language isn't set, use the system's default value
            if (!StringUtils.isNotBlank(language)) {
                language = Item.ANY;
            }

            bitstreamService.addMetadata(c, b, schema, element, qualifier, language, value, authority, confidence);
        }
    }

    public void setWorkflow(boolean workflow) {
        this.workflow = workflow;
    }

    public void setReinstate(boolean reinstate) {
        this.reinstate = reinstate;
    }

    public void setWithdrawn(boolean goToWithdrawn) {
        this.withdrawn = goToWithdrawn;
    }

    public void setBackToWorkspace(boolean backToWorkspace) {
        this.backToWorkspace = backToWorkspace;
    }

    public void setMyEPerson(EPerson myEPerson) {
        this.myEPerson = myEPerson;
    }

    public void setBatchJob(EPerson batchJob) {
        this.batchJob = batchJob;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef.trim();
    }

}