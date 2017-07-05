/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.TransformationEngine;
import gr.ekt.bte.core.TransformationResult;
import gr.ekt.bte.core.TransformationSpec;
import gr.ekt.bte.dataloader.FileDataLoader;
import gr.ekt.bteio.generators.DSpaceOutputGenerator;
import gr.ekt.bteio.loaders.OAIPMHDataLoader;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.dspace.app.itemimport.service.ItemImportService;
import org.dspace.app.util.LocalSchemaFilenameFilter;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.service.*;
import org.dspace.core.*;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.service.HandleService;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.mail.MessagingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Import items into DSpace. The conventional use is upload files by copying
 * them. DSpace writes the item's bitstreams into its assetstore. Metadata is
 * also loaded to the DSpace database.
 * <P>
 * A second use assumes the bitstream files already exist in a storage
 * resource accessible to DSpace. In this case the bitstreams are 'registered'.
 * That is, the metadata is loaded to the DSpace database and DSpace is given
 * the location of the file which is subsumed into DSpace.
 * <P>
 * The distinction is controlled by the format of lines in the 'contents' file.
 * See comments in processContentsFile() below.
 * <P>
 * Modified by David Little, UCSD Libraries 12/21/04 to
 * allow the registration of files (bitstreams) into DSpace.
 */
public class ItemImportServiceImpl implements ItemImportService, InitializingBean
{
    private final Logger log = Logger.getLogger(ItemImportServiceImpl.class);

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected BitstreamFormatService bitstreamFormatService;
    @Autowired(required = true)
    protected BundleService bundleService;
    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected EPersonService ePersonService;
    @Autowired(required = true)
    protected HandleService handleService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected InstallItemService installItemService;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected MetadataFieldService metadataFieldService;
    @Autowired(required = true)
    protected MetadataSchemaService metadataSchemaService;
    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;
    @Autowired(required = true)
    protected WorkspaceItemService workspaceItemService;
    @Autowired(required = true)
    protected WorkflowService workflowService;

    protected final String tempWorkDir = ConfigurationManager.getProperty("org.dspace.app.batchitemimport.work.dir");

    protected boolean isTest = false;
    protected boolean isResume = false;
    protected boolean useWorkflow = false;
    protected boolean useWorkflowSendEmail = false;
    protected boolean isQuiet = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        //Ensure tempWorkDir exists
        File tempWorkDirFile = new File(tempWorkDir);
        if (!tempWorkDirFile.exists()){
            boolean success = tempWorkDirFile.mkdir();
            if (success) {
                log.info("Created org.dspace.app.batchitemimport.work.dir of: " + tempWorkDir);
            } else {
                log.error("Cannot create batch import directory! " + tempWorkDir);
            }
        }
    }

    // File listing filter to look for metadata files
    protected FilenameFilter metadataFileFilter = new LocalSchemaFilenameFilter();

    // File listing filter to check for folders
    protected FilenameFilter directoryFilter = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String n)
        {
            File item = new File(dir.getAbsolutePath() + File.separatorChar + n);
            return item.isDirectory();
        }
    };
    
    protected ItemImportServiceImpl(){
        //Protected consumer to ensure that we use spring to create a bean, NEVER make this public
    }




    /**
     * In this method, the BTE is instantiated. THe workflow generates the DSpace files
     * necessary for the upload, and the default item import method is called
     * @param c The contect
     * @param mycollections The collections the items are inserted to
     * @param sourceDir The filepath to the file to read data from
     * @param mapFile The filepath to mapfile to be generated
     * @param template whether to use collection template item as starting point
     * @param inputType The type of the input data (bibtex, csv, etc.)
     * @param workingDir The path to create temporary files (for command line or UI based)
     * @throws Exception if error occurs
     */
    @Override
    public void addBTEItems(Context c, List<Collection> mycollections,
            String sourceDir, String mapFile, boolean template, String inputType, String workingDir) throws Exception
    {
    	//Determine the folder where BTE will output the results
    	String outputFolder = null;
    	if (workingDir == null){ //This indicates a command line import, create a random path
    		File importDir = new File(ConfigurationManager.getProperty("org.dspace.app.batchitemimport.work.dir"));
            if (!importDir.exists()){
            	boolean success = importDir.mkdir();
            	if (!success) {
            		log.info("Cannot create batch import directory!");
            		throw new Exception("Cannot create batch import directory!");
            	}
            }
            //Get a random folder in case two admins batch import data at the same time
    		outputFolder = importDir + File.separator + generateRandomFilename(true);
    	}
    	else { //This indicates a UI import, working dir is preconfigured
    		outputFolder = workingDir;
    	}

        BTEBatchImportService dls  = new DSpace().getSingletonService(BTEBatchImportService.class);
        DataLoader dataLoader = dls.getDataLoaders().get(inputType);
        Map<String, String> outputMap = dls.getOutputMap();
        TransformationEngine te = dls.getTransformationEngine();

        if (dataLoader==null){
            System.out.println("ERROR: The key used in -i parameter must match a valid DataLoader in the BTE Spring XML configuration file!");
            return;
        }

        if (outputMap==null){
            System.out.println("ERROR: The key used in -i parameter must match a valid outputMapping in the BTE Spring XML configuration file!");
            return;
        }

        if (dataLoader instanceof FileDataLoader){
            FileDataLoader fdl = (FileDataLoader) dataLoader;
            if (!StringUtils.isBlank(sourceDir)) {
                System.out.println("INFO: Dataloader will load data from the file specified in the command prompt (and not from the Spring XML configuration file)");
                fdl.setFilename(sourceDir);
            }
        }
        else if (dataLoader instanceof OAIPMHDataLoader){
            OAIPMHDataLoader fdl = (OAIPMHDataLoader) dataLoader;
            System.out.println(sourceDir);
            if (!StringUtils.isBlank(sourceDir)){
                System.out.println("INFO: Dataloader will load data from the address specified in the command prompt (and not from the Spring XML configuration file)");
                fdl.setServerAddress(sourceDir);
            }
        }
        if (dataLoader!=null){
            System.out.println("INFO: Dataloader " + dataLoader.toString()+" will be used for the import!");

        	te.setDataLoader(dataLoader);

        	DSpaceOutputGenerator outputGenerator = new DSpaceOutputGenerator(outputMap);
        	outputGenerator.setOutputDirectory(outputFolder);

        	te.setOutputGenerator(outputGenerator);

        	try {
        		TransformationResult res = te.transform(new TransformationSpec());
        		List<String> output = res.getOutput();
        		outputGenerator.writeOutput(output);
        	} catch (Exception e) {
        		System.err.println("Exception");
        		e.printStackTrace();
        		throw e;
        	}
        	addItems(c, mycollections, outputFolder, mapFile, template);
        }
    }

    @Override
    public void addItemsAtomic(Context c, List<Collection> mycollections, String sourceDir, String mapFile, boolean template) throws Exception {
        try {
            addItems(c, mycollections, sourceDir, mapFile, template);
        } catch (Exception addException) {
            log.error("AddItems encountered an error, will try to revert. Error: " + addException.getMessage());
            deleteItems(c, mapFile);
            log.info("Attempted to delete partial (errored) import");
            throw addException;
        }
    }

    @Override
    public void addItems(Context c, List<Collection> mycollections,
            String sourceDir, String mapFile, boolean template) throws Exception
    {
        // create the mapfile
        File outFile = null;
        PrintWriter mapOut = null;
        try {
            Map<String, String> skipItems = new HashMap<>(); // set of items to skip if in 'resume'
            // mode

            System.out.println("Adding items from directory: " + sourceDir);
            log.debug("Adding items from directory: " + sourceDir);
            System.out.println("Generating mapfile: " + mapFile);
            log.debug("Generating mapfile: " + mapFile);

            boolean directoryFileCollections = false;
            if (mycollections == null)
            {
                directoryFileCollections = true;
            }

            if (!isTest)
            {
                // get the directory names of items to skip (will be in keys of
                // hash)
                if (isResume)
                {
                    skipItems = readMapFile(mapFile);
                }

                // sneaky isResume == true means open file in append mode
                outFile = new File(mapFile);
                mapOut = new PrintWriter(new FileWriter(outFile, isResume));

                if (mapOut == null)
                {
                    throw new Exception("can't open mapfile: " + mapFile);
                }
            }

            // open and process the source directory
            File d = new java.io.File(sourceDir);

            if (d == null || !d.isDirectory())
            {
                throw new Exception("Error, cannot open source directory " + sourceDir);
            }

            String[] dircontents = d.list(directoryFilter);

            Arrays.sort(dircontents, ComparatorUtils.naturalComparator());

        for (int i = 0; i < dircontents.length; i++)
        {
            if (skipItems.containsKey(dircontents[i]))
            {
                System.out.println("Skipping import of " + dircontents[i]);
            }
            else
            {
                List<Collection> clist;
                if (directoryFileCollections) {
                    String path = sourceDir + File.separatorChar + dircontents[i];
                    try {
                        List<Collection> cols = processCollectionFile(c, path, "collections");
                        if (cols == null) {
                            System.out.println("No collections specified for item " + dircontents[i] + ". Skipping.");
                            continue;
                        }
                        clist = cols;
                    }
                    catch (IllegalArgumentException e)
                    {
                        System.out.println(e.getMessage() + " Skipping." );
                        continue;
                    }
                }
                else
                {
                    clist = mycollections;
                }
                Item item =addItem(c, clist, sourceDir, dircontents[i], mapOut, template);c.uncacheEntity(item);
                System.out.println(i + " " + dircontents[i]);
            }
        }

        } finally {
            if(mapOut!=null) {
                mapOut.flush();
                mapOut.close();
            }
        }
    }

    @Override
    public void replaceItems(Context c, List<Collection> mycollections,
            String sourceDir, String mapFile, boolean template) throws Exception
    {
        // verify the source directory
        File d = new java.io.File(sourceDir);

        if (d == null || !d.isDirectory())
        {
            throw new Exception("Error, cannot open source directory "
                    + sourceDir);
        }

        // read in HashMap first, to get list of handles & source dirs
        Map<String, String> myHash = readMapFile(mapFile);

        // for each handle, re-import the item, discard the new handle
        // and re-assign the old handle
        for (Map.Entry<String, String> mapEntry : myHash.entrySet())
        {
            // get the old handle
            String newItemName = mapEntry.getKey();
            String oldHandle = mapEntry.getValue();

            Item oldItem = null;

            if (oldHandle.indexOf('/') != -1)
            {
                System.out.println("\tReplacing:  " + oldHandle);

                // add new item, locate old one
                oldItem = (Item) handleService.resolveToObject(c, oldHandle);
            }
            else
            {
                oldItem = itemService.findByIdOrLegacyId(c, oldHandle);
            }

            /* Rather than exposing public item methods to change handles --
             * two handles can't exist at the same time due to key constraints
             * so would require temp handle being stored, old being copied to new and
             * new being copied to old, all a bit messy -- a handle file is written to
             * the import directory containing the old handle, the existing item is
             * deleted and then the import runs as though it were loading an item which
             * had already been assigned a handle (so a new handle is not even assigned).
             * As a commit does not occur until after a successful add, it is safe to
             * do a delete as any error results in an aborted transaction without harming
             * the original item */
            File handleFile = new File(sourceDir + File.separatorChar + newItemName + File.separatorChar + "handle");
            PrintWriter handleOut = new PrintWriter(new FileWriter(handleFile, true));

            if (handleOut == null)
            {
                throw new Exception("can't open handle file: " + handleFile.getCanonicalPath());
            }

            handleOut.println(oldHandle);
            handleOut.close();

            deleteItem(c, oldItem);
            Item newItem = addItem(c, mycollections, sourceDir, newItemName, null, template);
            c.uncacheEntity(oldItem);
            c.uncacheEntity(newItem);
        }
    }

    @Override
    public void deleteItems(Context c, String mapFile) throws Exception
    {
        System.out.println("Deleting items listed in mapfile: " + mapFile);

        // read in the mapfile
        Map<String, String> myhash = readMapFile(mapFile);

        // now delete everything that appeared in the mapFile
        Iterator<String> i = myhash.keySet().iterator();

        while (i.hasNext())
        {
            String itemID = myhash.get(i.next());

            if (itemID.indexOf('/') != -1)
            {
                String myhandle = itemID;
                System.out.println("Deleting item " + myhandle);
                deleteItem(c, myhandle);
            }
            else
            {
                // it's an ID
                Item myitem = itemService.findByIdOrLegacyId(c, itemID);
                System.out.println("Deleting item " + itemID);
                deleteItem(c, myitem);
                c.uncacheEntity(myitem);
            }
        }
    }

    /**
     * item? try and add it to the archive.
     * @param c current Context
     * @param mycollections - add item to these Collections.
     * @param path - directory containing the item directories.
     * @param itemname handle - non-null means we have a pre-defined handle already
     * @param mapOut - mapfile we're writing
     * @param template whether to use collection template item as starting point
     * @return Item
     * @throws Exception if error occurs
     */
    protected Item addItem(Context c, List<Collection> mycollections, String path,
            String itemname, PrintWriter mapOut, boolean template) throws Exception
    {
        String mapOutputString = null;

        System.out.println("Adding item from directory " + itemname);
        log.debug("adding item from directory " + itemname);

        // create workspace item
        Item myitem = null;
        WorkspaceItem wi = null;
        WorkflowItem wfi = null;

        if (!isTest)
        {
            wi = workspaceItemService.create(c, mycollections.iterator().next(), template);
            myitem = wi.getItem();
        }

        // now fill out dublin core for item
        loadMetadata(c, myitem, path + File.separatorChar + itemname
                + File.separatorChar);

        // and the bitstreams from the contents file
        // process contents file, add bistreams and bundles, return any
        // non-standard permissions
        List<String> options = processContentsFile(c, myitem, path
                + File.separatorChar + itemname, "contents");

        if (useWorkflow)
        {
            // don't process handle file
            // start up a workflow
            if (!isTest)
            {
                // Should we send a workflow alert email or not?
                if (useWorkflowSendEmail) {
                    wfi = workflowService.start(c, wi);
                } else {
                    wfi = workflowService.startWithoutNotify(c, wi);
                }

                // send ID to the mapfile
                mapOutputString = itemname + " " + myitem.getID();
            }
        }
        else
        {
            // only process handle file if not using workflow system
            String myhandle = processHandleFile(c, myitem, path
                    + File.separatorChar + itemname, "handle");

            // put item in system
            if (!isTest)
            {
                try {
                    installItemService.installItem(c, wi, myhandle);
                } catch (Exception e) {
                    workspaceItemService.deleteAll(c, wi);
                    log.error("Exception after install item, try to revert...", e);
                    throw e;
                }

                // find the handle, and output to map file
                myhandle = handleService.findHandle(c, myitem);

                mapOutputString = itemname + " " + myhandle;
            }

            // set permissions if specified in contents file
            if (options.size() > 0)
            {
                System.out.println("Processing options");
                processOptions(c, myitem, options);
            }
        }

        // now add to multiple collections if requested
        if (mycollections.size() > 1)
        {
            for (int i = 1; i < mycollections.size(); i++)
            {
                if (!isTest)
                {
                    collectionService.addItem(c, mycollections.get(i), myitem);
                }
            }
        }

        // made it this far, everything is fine, commit transaction
        if (mapOut != null)
        {
            mapOut.println(mapOutputString);
        }

        //Clear intermediary objects from the cache
        c.uncacheEntity(wi);
        c.uncacheEntity(wfi);

        return myitem;
    }

    // remove, given the actual item
    protected void deleteItem(Context c, Item myitem) throws Exception
    {
        if (!isTest)
        {
            ArrayList<Collection> removeList = new ArrayList<>();
            List<Collection> collections = myitem.getCollections();

            // Save items to be removed to prevent concurrent modification exception DS-3322
            for (Collection collection : collections) {
                removeList.add(collection);
            }

            // Remove item from all the collections it's in
            for (Collection collection : removeList) {
                collectionService.removeItem(c, collection, myitem);
            }
        }
    }

    // remove, given a handle
    protected void deleteItem(Context c, String myhandle) throws Exception
    {
        // bit of a hack - to remove an item, you must remove it
        // from all collections it's a part of, then it will be removed
        Item myitem = (Item) handleService.resolveToObject(c, myhandle);

        if (myitem == null)
        {
            System.out.println("Error - cannot locate item - already deleted?");
        }
        else
        {
            deleteItem(c, myitem);
            c.uncacheEntity(myitem);
        }
    }

    ////////////////////////////////////
    // utility methods
    ////////////////////////////////////
    // read in the map file and generate a hashmap of (file,handle) pairs
    protected Map<String, String> readMapFile(String filename) throws Exception
    {
        Map<String, String> myHash = new HashMap<>();

        BufferedReader is = null;
        try
        {
            is = new BufferedReader(new FileReader(filename));

            String line;

            while ((line = is.readLine()) != null)
            {
                String myFile;
                String myHandle;

                // a line should be archive filename<whitespace>handle
                StringTokenizer st = new StringTokenizer(line);

                if (st.hasMoreTokens())
                {
                    myFile = st.nextToken();
                }
                else
                {
                    throw new Exception("Bad mapfile line:\n" + line);
                }

                if (st.hasMoreTokens())
                {
                    myHandle = st.nextToken();
                }
                else
                {
                    throw new Exception("Bad mapfile line:\n" + line);
                }

                myHash.put(myFile, myHandle);
            }
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }

        return myHash;
    }

    // Load all metadata schemas into the item.
    protected void loadMetadata(Context c, Item myitem, String path)
            throws SQLException, IOException, ParserConfigurationException,
            SAXException, TransformerException, AuthorizeException
    {
        // Load the dublin core metadata
        loadDublinCore(c, myitem, path + "dublin_core.xml");

        // Load any additional metadata schemas
        File folder = new File(path);
        File file[] = folder.listFiles(metadataFileFilter);
        for (int i = 0; i < file.length; i++)
        {
            loadDublinCore(c, myitem, file[i].getAbsolutePath());
        }
    }

    protected void loadDublinCore(Context c, Item myitem, String filename)
            throws SQLException, IOException, ParserConfigurationException,
            SAXException, TransformerException, AuthorizeException
    {
        Document document = loadXML(filename);

        // Get the schema, for backward compatibility we will default to the
        // dublin core schema if the schema name is not available in the import
        // file
        String schema;
        NodeList metadata = XPathAPI.selectNodeList(document, "/dublin_core");
        Node schemaAttr = metadata.item(0).getAttributes().getNamedItem(
                "schema");
        if (schemaAttr == null)
        {
            schema = MetadataSchema.DC_SCHEMA;
        }
        else
        {
            schema = schemaAttr.getNodeValue();
        }

        // Get the nodes corresponding to formats
        NodeList dcNodes = XPathAPI.selectNodeList(document,
                "/dublin_core/dcvalue");

        if (!isQuiet)
        {
            System.out.println("\tLoading dublin core from " + filename);
        }

        // Add each one as a new format to the registry
        for (int i = 0; i < dcNodes.getLength(); i++)
        {
            Node n = dcNodes.item(i);
            addDCValue(c, myitem, schema, n);
        }
    }

    protected void addDCValue(Context c, Item i, String schema, Node n) throws TransformerException, SQLException, AuthorizeException
    {
        String value = getStringValue(n); //n.getNodeValue();
        // compensate for empty value getting read as "null", which won't display
        if (value == null)
        {
            value = "";
        }
        else
        {
        	value = value.trim();
        }
        // //getElementData(n, "element");
        String element = getAttributeValue(n, "element");
        String qualifier = getAttributeValue(n, "qualifier"); //NodeValue();
        // //getElementData(n,
        // "qualifier");
        String language = getAttributeValue(n, "language");
        if (language != null)
        {
            language = language.trim();
        }

        if (!isQuiet)
        {
            System.out.println("\tSchema: " + schema + " Element: " + element + " Qualifier: " + qualifier
                    + " Value: " + value);
        }

        if ("none".equals(qualifier) || "".equals(qualifier))
        {
            qualifier = null;
        }
        // only add metadata if it is no test and there is an actual value
        if (!isTest && !value.equals(""))
        {
            itemService.addMetadata(c, i, schema, element, qualifier, language, value);
        }
        else
        {
            // If we're just test the import, let's check that the actual metadata field exists.
        	MetadataSchema foundSchema = metadataSchemaService.find(c,schema);

        	if (foundSchema == null)
        	{
        		System.out.println("ERROR: schema '"+schema+"' was not found in the registry.");
        		return;
        	}

        	MetadataField foundField = metadataFieldService.findByElement(c, foundSchema, element, qualifier);

        	if (foundField == null)
        	{
        		System.out.println("ERROR: Metadata field: '"+schema+"."+element+"."+qualifier+"' was not found in the registry.");
        		return;
            }
        }
    }

    /**
     * Read the collections file inside the item directory. If there
     * is one and it is not empty return a list of collections in
     * which the item should be inserted. If it does not exist or it
     * is empty return null.
     *
     * @param c The context
     * @param path The path to the data directory for this item
     * @param filename The collections file filename. Should be "collections"
     * @return A list of collections in which to insert the item or null
     * @throws IOException if IO error
     * @throws SQLException if database error
     */

    protected List<Collection> processCollectionFile(Context c, String path, String filename) throws IOException, SQLException
    {
        File file = new File(path + File.separatorChar + filename);
        ArrayList<Collection> collections = new ArrayList<>();
        List<Collection> result = null;
        System.out.println("Processing collections file: " + filename);

        if(file.exists())
        {
            BufferedReader br = null;
            try
            {
                br = new BufferedReader(new FileReader(file));
                String line = null;
                while ((line = br.readLine()) != null)
                {
                    DSpaceObject obj = null;
                    if (line.indexOf('/') != -1)
                    {
                        obj = handleService.resolveToObject(c, line);
                        if (obj == null || obj.getType() != Constants.COLLECTION)
                        {
                            obj = null;
                        }
                    }
                    else
                    {
                        obj = collectionService.find(c, UUID.fromString(line));
                    }

                    if (obj == null) {
                        throw new IllegalArgumentException("Cannot resolve " + line + " to a collection.");
                    }
                    collections.add((Collection)obj);

                }

                result = collections;
            }
            catch (FileNotFoundException e)
            {
                System.out.println("No collections file found.");
            }
            finally
            {
                if (br != null)
                {
                    try {
                        br.close();
                    } catch (IOException e) {
                        System.out.println("Non-critical problem releasing resources.");
                    }
                }
            }
        }

        return result;
    }

    /**
     * Read in the handle file contents or return null if empty or doesn't exist
     * @param c DSpace context
     * @param i DSpace item
     * @param path path to handle file
     * @param filename name of file
     * @return handle file contents or null if doesn't exist
     */
    protected String processHandleFile(Context c, Item i, String path, String filename)
    {
        File file = new File(path + File.separatorChar + filename);
        String result = null;

        System.out.println("Processing handle file: " + filename);
        if (file.exists())
        {
            BufferedReader is = null;
            try
            {
                is = new BufferedReader(new FileReader(file));

                // result gets contents of file, or null
                result = is.readLine();

                System.out.println("read handle: '" + result + "'");

            }
            catch (FileNotFoundException e)
            {
                // probably no handle file, just return null
                System.out.println("It appears there is no handle file -- generating one");
            }
            catch (IOException e)
            {
                // probably no handle file, just return null
                System.out.println("It appears there is no handle file -- generating one");
            }
            finally
            {
                if (is != null)
                {
                    try
                    {
                        is.close();
                    }
                    catch (IOException e1)
                    {
                        System.err.println("Non-critical problem releasing resources.");
                    }
                }
            }
        }
        else
        {
            // probably no handle file, just return null
            System.out.println("It appears there is no handle file -- generating one");
        }

        return result;
    }

    /**
     * Given a contents file and an item, stuffing it with bitstreams from the
     * contents file Returns a List of Strings with lines from the contents
     * file that request non-default bitstream permission
     * @param c DSpace Context
     * @param i DSpace item
     * @param path path as string
     * @param filename file name
     * @return List of Strings
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws AuthorizeException if authorization error
     */
    protected List<String> processContentsFile(Context c, Item i, String path,
            String filename) throws SQLException, IOException,
            AuthorizeException
    {
        File contentsFile = new File(path + File.separatorChar + filename);
        String line = "";
        List<String> options = new ArrayList<>();

        System.out.println("\tProcessing contents file: " + contentsFile);

        if (contentsFile.exists())
        {
            BufferedReader is = null;
            try
            {
                is = new BufferedReader(new FileReader(contentsFile));

                while ((line = is.readLine()) != null)
                {
                    if ("".equals(line.trim()))
                    {
                        continue;
                    }

                    //	1) registered into dspace (leading -r)
                    //  2) imported conventionally into dspace (no -r)
                    if (line.trim().startsWith("-r "))
                    {
                        // line should be one of these two:
                        // -r -s n -f filepath
                        // -r -s n -f filepath\tbundle:bundlename
                        // where
                        //		n is the assetstore number
                        //  	filepath is the path of the file to be registered
                        //  	bundlename is an optional bundle name
                        String sRegistrationLine = line.trim();
                        int iAssetstore = -1;
                        String sFilePath = null;
                        String sBundle = null;
                        StringTokenizer tokenizer = new StringTokenizer(sRegistrationLine);
                        while (tokenizer.hasMoreTokens())
                        {
                            String sToken = tokenizer.nextToken();
                            if ("-r".equals(sToken))
                            {
                                continue;
                            }
                            else if ("-s".equals(sToken) && tokenizer.hasMoreTokens())
                            {
                                try
                                {
                                    iAssetstore =
                                        Integer.parseInt(tokenizer.nextToken());
                                }
                                catch (NumberFormatException e)
                                {
                                    // ignore - iAssetstore remains -1
                                }
                            }
                            else if ("-f".equals(sToken) && tokenizer.hasMoreTokens())
                            {
                                sFilePath = tokenizer.nextToken();
                            }
                            else if (sToken.startsWith("bundle:"))
                            {
                                sBundle = sToken.substring(7);
                            }
                            else
                            {
                                // unrecognized token - should be no problem
                            }
                        } // while
                        if (iAssetstore == -1 || sFilePath == null)
                        {
                            System.out.println("\tERROR: invalid contents file line");
                            System.out.println("\t\tSkipping line: "
                                    + sRegistrationLine);
                            continue;
                        }

                        // look for descriptions
                        boolean descriptionExists = false;
                        String descriptionMarker = "\tdescription:";
                        int dMarkerIndex = line.indexOf(descriptionMarker);
                        int dEndIndex = 0;
                        if (dMarkerIndex > 0)
                        {
                        	dEndIndex = line.indexOf("\t", dMarkerIndex + 1);
                        	if (dEndIndex == -1)
                        	{
                        		dEndIndex = line.length();
                        	}
                        	descriptionExists = true;
                        }
                        String sDescription = "";
                        if (descriptionExists)
                        {
                        	sDescription = line.substring(dMarkerIndex, dEndIndex);
                        	sDescription = sDescription.replaceFirst("description:", "");
                        }

                        registerBitstream(c, i, iAssetstore, sFilePath, sBundle, sDescription);
                        System.out.println("\tRegistering Bitstream: " + sFilePath
                                + "\tAssetstore: " + iAssetstore
                                + "\tBundle: " + sBundle
                                + "\tDescription: " + sDescription);
                        continue;				// process next line in contents file
                    }

                    int bitstreamEndIndex = line.indexOf('\t');

                    if (bitstreamEndIndex == -1)
                    {
                        // no extra info
                        processContentFileEntry(c, i, path, line, null, false);
                        System.out.println("\tBitstream: " + line);
                    }
                    else
                    {

                        String bitstreamName = line.substring(0, bitstreamEndIndex);

                        boolean bundleExists = false;
                        boolean permissionsExist = false;
                        boolean descriptionExists = false;

                        // look for a bundle name
                        String bundleMarker = "\tbundle:";
                        int bMarkerIndex = line.indexOf(bundleMarker);
                        int bEndIndex = 0;
                        if (bMarkerIndex > 0)
                        {
                            bEndIndex = line.indexOf("\t", bMarkerIndex + 1);
                            if (bEndIndex == -1)
                            {
                                bEndIndex = line.length();
                            }
                            bundleExists = true;
                        }

                        // look for permissions
                        String permissionsMarker = "\tpermissions:";
                        int pMarkerIndex = line.indexOf(permissionsMarker);
                        int pEndIndex = 0;
                        if (pMarkerIndex > 0)
                        {
                            pEndIndex = line.indexOf("\t", pMarkerIndex + 1);
                            if (pEndIndex == -1)
                            {
                                pEndIndex = line.length();
                            }
                            permissionsExist = true;
                        }

                        // look for descriptions
                        String descriptionMarker = "\tdescription:";
                        int dMarkerIndex = line.indexOf(descriptionMarker);
                        int dEndIndex = 0;
                        if (dMarkerIndex > 0)
                        {
                            dEndIndex = line.indexOf("\t", dMarkerIndex + 1);
                            if (dEndIndex == -1)
                            {
                                dEndIndex = line.length();
                            }
                            descriptionExists = true;
                        }

                        // is this the primary bitstream?
                        String primaryBitstreamMarker = "\tprimary:true";
                        boolean primary = false;
                        String primaryStr = "";
                        if (line.contains(primaryBitstreamMarker))
                        {
                            primary = true;
                            primaryStr = "\t **Setting as primary bitstream**";
                        }

                        if (bundleExists)
                        {
                            String bundleName = line.substring(bMarkerIndex
                                    + bundleMarker.length(), bEndIndex).trim();

                            processContentFileEntry(c, i, path, bitstreamName, bundleName, primary);
                            System.out.println("\tBitstream: " + bitstreamName +
                                               "\tBundle: " + bundleName +
                                               primaryStr);
                        }
                        else
                        {
                            processContentFileEntry(c, i, path, bitstreamName, null, primary);
                            System.out.println("\tBitstream: " + bitstreamName + primaryStr);
                        }

                        if (permissionsExist || descriptionExists)
                        {
                            String extraInfo = bitstreamName;

                            if (permissionsExist)
                            {
                                extraInfo = extraInfo
                                        + line.substring(pMarkerIndex, pEndIndex);
                            }

                            if (descriptionExists)
                            {
                                extraInfo = extraInfo
                                        + line.substring(dMarkerIndex, dEndIndex);
                            }

                            options.add(extraInfo);
                        }
                    }
                }
            }
            finally
            {
                if (is != null)
                {
                    is.close();
                }
            }
        }
        else
        {
            File dir = new File(path);
            String[] dirListing = dir.list();
            for (String fileName : dirListing)
            {
                if (!"dublin_core.xml".equals(fileName) && !fileName.equals("handle") && !metadataFileFilter.accept(dir, fileName))
                {
                    throw new FileNotFoundException("No contents file found");
                }
            }

            System.out.println("No contents file found - but only metadata files found. Assuming metadata only.");
        }

        return options;
    }

    /**
     * each entry represents a bitstream....
     * @param c DSpace Context
     * @param i Dspace Item
     * @param path path to file
     * @param fileName file name
     * @param bundleName bundle name
     * @param primary if primary bitstream
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws AuthorizeException if authorization error
     */
    protected void processContentFileEntry(Context c, Item i, String path,
            String fileName, String bundleName, boolean primary) throws SQLException,
            IOException, AuthorizeException
    {
        String fullpath = path + File.separatorChar + fileName;

        // get an input stream
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
                fullpath));

        Bitstream bs = null;
        String newBundleName = bundleName;

        if (bundleName == null)
        {
            // is it license.txt?
            if ("license.txt".equals(fileName))
            {
                newBundleName = "LICENSE";
            }
            else
            {
                // call it ORIGINAL
                newBundleName = "ORIGINAL";
            }
        }

        if (!isTest)
        {
            // find the bundle
            List<Bundle> bundles = itemService.getBundles(i, newBundleName);
            Bundle targetBundle = null;

            if (bundles.size() < 1)
            {
                // not found, create a new one
                targetBundle = bundleService.create(c, i, newBundleName);
            }
            else
            {
                // put bitstreams into first bundle
                targetBundle = bundles.iterator().next();
            }

            // now add the bitstream
            bs = bitstreamService.create(c, targetBundle, bis);

            bs.setName(c, fileName);

            // Identify the format
            // FIXME - guessing format guesses license.txt incorrectly as a text
            // file format!
            BitstreamFormat bf = bitstreamFormatService.guessFormat(c, bs);
            bitstreamService.setFormat(c, bs, bf);

            // Is this a the primary bitstream?
            if (primary)
            {
                targetBundle.setPrimaryBitstreamID(bs);
                bundleService.update(c, targetBundle);
            }

            bitstreamService.update(c, bs);
        }

        bis.close();
    }

    /**
     * Register the bitstream file into DSpace
     *
     * @param c DSpace Context
     * @param i DSpace Item
     * @param assetstore assetstore number
     * @param bitstreamPath the full filepath expressed in the contents file
     * @param bundleName bundle name
     * @param description bitstream description
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws AuthorizeException if authorization error
     */
    protected void registerBitstream(Context c, Item i, int assetstore,
            String bitstreamPath, String bundleName, String description )
        	throws SQLException, IOException, AuthorizeException
    {
        // TODO validate assetstore number
        // TODO make sure the bitstream is there

        Bitstream bs = null;
        String newBundleName = bundleName;

        if (StringUtils.isBlank(bundleName))
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

        if(!isTest)
        {
        	// find the bundle
	        List<Bundle> bundles = itemService.getBundles(i, newBundleName);
	        Bundle targetBundle = null;

	        if( bundles.size() < 1 )
	        {
	            // not found, create a new one
	            targetBundle = bundleService.create(c, i, newBundleName);
	        }
	        else
	        {
	            // put bitstreams into first bundle
	            targetBundle = bundles.iterator().next();
	        }

	        // now add the bitstream
	        bs = bitstreamService.register(c, targetBundle, assetstore, bitstreamPath);

	        // set the name to just the filename
	        int iLastSlash = bitstreamPath.lastIndexOf('/');
	        bs.setName(c, bitstreamPath.substring(iLastSlash + 1));

	        // Identify the format
	        // FIXME - guessing format guesses license.txt incorrectly as a text file format!
	        BitstreamFormat bf = bitstreamFormatService.guessFormat(c, bs);
            bitstreamService.setFormat(c, bs, bf);
	        bs.setDescription(c, description);

            bitstreamService.update(c, bs);
        }
    }

    /**
     *
     * Process the Options to apply to the Item. The options are tab delimited
     *
     * Options:
     *    {@code
     *      48217870-MIT.pdf        permissions: -r 'MIT Users'     description: Full printable version (MIT only)
     *      permissions:[r|w]-['group name']
     *      description: 'the description of the file'
     *    }
     *      where:
     *    {@code
     *          [r|w] (meaning: read|write)
     *          ['MIT Users'] (the group name)
     *    }
     * @param c DSpace Context
     * @param myItem DSpace Item
     * @param options List of option strings
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    protected void processOptions(Context c, Item myItem, List<String> options)
            throws SQLException, AuthorizeException
    {
        for (String line : options)
        {
            System.out.println("\tprocessing " + line);

            boolean permissionsExist = false;
            boolean descriptionExists = false;

            String permissionsMarker = "\tpermissions:";
            int pMarkerIndex = line.indexOf(permissionsMarker);
            int pEndIndex = 0;
            if (pMarkerIndex > 0)
            {
                pEndIndex = line.indexOf("\t", pMarkerIndex + 1);
                if (pEndIndex == -1)
                {
                    pEndIndex = line.length();
                }
                permissionsExist = true;
            }

            String descriptionMarker = "\tdescription:";
            int dMarkerIndex = line.indexOf(descriptionMarker);
            int dEndIndex = 0;
            if (dMarkerIndex > 0)
            {
                dEndIndex = line.indexOf("\t", dMarkerIndex + 1);
                if (dEndIndex == -1)
                {
                    dEndIndex = line.length();
                }
                descriptionExists = true;
            }

            int bsEndIndex = line.indexOf("\t");
            String bitstreamName = line.substring(0, bsEndIndex);

            int actionID = -1;
            String groupName = "";
            Group myGroup = null;
            if (permissionsExist)
            {
                String thisPermission = line.substring(pMarkerIndex
                        + permissionsMarker.length(), pEndIndex);

                // get permission type ("read" or "write")
                int pTypeIndex = thisPermission.indexOf('-');

                // get permission group (should be in single quotes)
                int groupIndex = thisPermission.indexOf('\'', pTypeIndex);
                int groupEndIndex = thisPermission.indexOf('\'', groupIndex + 1);

                // if not in single quotes, assume everything after type flag is
                // group name
                if (groupIndex == -1)
                {
                    groupIndex = thisPermission.indexOf(' ', pTypeIndex);
                    groupEndIndex = thisPermission.length();
                }

                groupName = thisPermission.substring(groupIndex + 1,
                        groupEndIndex);

                if (thisPermission.toLowerCase().charAt(pTypeIndex + 1) == 'r')
                {
                    actionID = Constants.READ;
                }
                else if (thisPermission.toLowerCase().charAt(pTypeIndex + 1) == 'w')
                {
                    actionID = Constants.WRITE;
                }

                try
                {
                    myGroup = groupService.findByName(c, groupName);
                }
                catch (SQLException sqle)
                {
                    System.out.println("SQL Exception finding group name: "
                            + groupName);
                    // do nothing, will check for null group later
                }
            }

            String thisDescription = "";
            if (descriptionExists)
            {
                thisDescription = line.substring(
                        dMarkerIndex + descriptionMarker.length(), dEndIndex)
                        .trim();
            }

            Bitstream bs = null;
            boolean notfound = true;
            if (!isTest)
            {
                // find bitstream
                List<Bitstream> bitstreams = itemService.getNonInternalBitstreams(c, myItem);
                for (int j = 0; j < bitstreams.size() && notfound; j++)
                {
                    if (bitstreams.get(j).getName().equals(bitstreamName))
                    {
                        bs = bitstreams.get(j);
                        notfound = false;
                    }
                }
            }

            if (notfound && !isTest)
            {
                // this should never happen
                System.out.println("\tdefault permissions set for "
                        + bitstreamName);
            }
            else if (!isTest)
            {
                if (permissionsExist)
                {
                    if (myGroup == null)
                    {
                        System.out.println("\t" + groupName
                                + " not found, permissions set to default");
                    }
                    else if (actionID == -1)
                    {
                        System.out
                                .println("\tinvalid permissions flag, permissions set to default");
                    }
                    else
                    {
                        System.out.println("\tSetting special permissions for "
                                + bitstreamName);
                        setPermission(c, myGroup, actionID, bs);
                    }
                }

                if (descriptionExists)
                {
                    System.out.println("\tSetting description for "
                            + bitstreamName);
                    bs.setDescription(c, thisDescription);
                    bitstreamService.update(c, bs);
                }
            }
        }
    }

    /**
     * Set the Permission on a Bitstream.
     *
     * @param c DSpace Context
     * @param g Dspace Group
     * @param actionID action identifier
     * @param bs Bitstream
     * @see org.dspace.core.Constants
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    protected void setPermission(Context c, Group g, int actionID, Bitstream bs)
            throws SQLException, AuthorizeException
    {
        if (!isTest)
        {
            // remove the default policy
            authorizeService.removeAllPolicies(c, bs);

            // add the policy
            ResourcePolicy rp = resourcePolicyService.create(c);

            rp.setdSpaceObject(bs);
            rp.setAction(actionID);
            rp.setGroup(g);

            resourcePolicyService.update(c, rp);
        }
        else
        {
            if (actionID == Constants.READ)
            {
                System.out.println("\t\tpermissions: READ for " + g.getName());
            }
            else if (actionID == Constants.WRITE)
            {
                System.out.println("\t\tpermissions: WRITE for " + g.getName());
            }
        }

    }

    // XML utility methods
    /**
     * Lookup an attribute from a DOM node.
     * @param n node
     * @param name attribute name
     * @return attribute value
     */
    private String getAttributeValue(Node n, String name)
    {
        NamedNodeMap nm = n.getAttributes();

        for (int i = 0; i < nm.getLength(); i++)
        {
            Node node = nm.item(i);

            if (name.equals(node.getNodeName()))
            {
                return node.getNodeValue();
            }
        }

        return "";
    }


    /**
     * Return the String value of a Node.
     * @param node node
     * @return string value
     */
    protected String getStringValue(Node node)
    {
        String value = node.getNodeValue();

        if (node.hasChildNodes())
        {
            Node first = node.getFirstChild();

            if (first.getNodeType() == Node.TEXT_NODE)
            {
                return first.getNodeValue();
            }
        }

        return value;
    }

    /**
     * Load in the XML from file.
     *
     * @param filename
     *            the filename to load from
     *
     * @return the DOM representation of the XML file
     * @throws IOException if IO error
     * @throws ParserConfigurationException if config error
     * @throws SAXException if XML error
     */
    protected Document loadXML(String filename) throws IOException,
            ParserConfigurationException, SAXException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();

        return builder.parse(new File(filename));
    }

    /**
     * Delete a directory and its child files and directories
     * @param path The directory to delete
     * @return Whether the deletion was successful or not
     */
    protected boolean deleteDirectory(File path)
    {
        if (path.exists())
        {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                if (files[i].isDirectory())
                {
                    deleteDirectory(files[i]);
                }
                else
                {
                    if (!files[i].delete())
                    {
                        log.error("Unable to delete file: " + files[i].getName());
                    }
                }
            }
        }

        boolean pathDeleted = path.delete();
        return (pathDeleted);
    }

    @Override
    public String unzip(File zipfile) throws IOException {
    	return unzip(zipfile, null);
    }
    
    @Override
    public String unzip(File zipfile, String destDir) throws IOException {
        // 2
        // does the zip file exist and can we write to the temp directory
        if (!zipfile.canRead())
        {
            log.error("Zip file '" + zipfile.getAbsolutePath() + "' does not exist, or is not readable.");
        }

        String destinationDir = destDir;
        if (destinationDir == null){
        	destinationDir = tempWorkDir;
        }

        File tempdir = new File(destinationDir);
        if (!tempdir.isDirectory())
        {
            log.error("'" + ConfigurationManager.getProperty("org.dspace.app.itemexport.work.dir") +
                    "' as defined by the key 'org.dspace.app.itemexport.work.dir' in dspace.cfg " +
                    "is not a valid directory");
        }

        if (!tempdir.exists() && !tempdir.mkdirs())
        {
            log.error("Unable to create temporary directory: " + tempdir.getAbsolutePath());
        }
        String sourcedir = destinationDir + System.getProperty("file.separator") + zipfile.getName();
        String zipDir = destinationDir + System.getProperty("file.separator") + zipfile.getName() + System.getProperty("file.separator");


        // 3
        String sourceDirForZip = sourcedir;
        ZipFile zf = new ZipFile(zipfile);
        ZipEntry entry;
        Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements())
        {
            entry = entries.nextElement();
            if (entry.isDirectory())
            {
                if (!new File(zipDir + entry.getName()).mkdirs())
                {
                    log.error("Unable to create contents directory: " + zipDir + entry.getName());
                }
            }
            else
            {
                System.out.println("Extracting file: " + entry.getName());
                log.info("Extracting file: " + entry.getName());

                int index = entry.getName().lastIndexOf('/');
                if (index == -1)
                {
                    // Was it created on Windows instead?
                    index = entry.getName().lastIndexOf('\\');
                }
                if (index > 0)
                {
                    File dir = new File(zipDir + entry.getName().substring(0, index));
                    if (!dir.exists() && !dir.mkdirs())
                    {
                        log.error("Unable to create directory: " + dir.getAbsolutePath());
                    }

                    //Entries could have too many directories, and we need to adjust the sourcedir
                    // file1.zip (SimpleArchiveFormat / item1 / contents|dublin_core|...
                    //            SimpleArchiveFormat / item2 / contents|dublin_core|...
                    // or
                    // file2.zip (item1 / contents|dublin_core|...
                    //            item2 / contents|dublin_core|...

                    //regex supports either windows or *nix file paths
                    String[] entryChunks = entry.getName().split("/|\\\\");
                    if(entryChunks.length > 2) {
                        if(StringUtils.equals(sourceDirForZip, sourcedir)) {
                            sourceDirForZip = sourcedir + "/" + entryChunks[0];
                        }
                    }


                }
                byte[] buffer = new byte[1024];
                int len;
                InputStream in = zf.getInputStream(entry);
                BufferedOutputStream out = new BufferedOutputStream(
                        new FileOutputStream(zipDir + entry.getName()));
                while((len = in.read(buffer)) >= 0)
                {
                    out.write(buffer, 0, len);
                }
                in.close();
                out.close();
            }
        }

        //Close zip file
        zf.close();
        
        if(!StringUtils.equals(sourceDirForZip, sourcedir)) {
            sourcedir = sourceDirForZip;
            System.out.println("Set sourceDir using path inside of Zip: " + sourcedir);
            log.info("Set sourceDir using path inside of Zip: " + sourcedir);
        }

        return sourcedir;
    }

    @Override
    public String unzip(String sourcedir, String zipfilename) throws IOException {
        File zipfile = new File(sourcedir + File.separator + zipfilename);
        return unzip(zipfile);
    }
    
    /**
     * Generate a random filename based on current time
     * @param hidden set to add . as a prefix to make the file hidden
     * @return the filename
     */
    protected String generateRandomFilename(boolean hidden)
    {
    	String filename = String.format("%s", RandomStringUtils.randomAlphanumeric(8));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        String datePart = sdf.format(new Date());
        filename = datePart+"_"+filename;

        return filename;
    }

    /**
     * 
     * Given a local file or public URL to a zip file that has the Simple Archive Format, this method imports the contents to DSpace
     * @param filepath The filepath to local file or the public URL of the zip file
     * @param owningCollection The owning collection the items will belong to
     * @param otherCollections The collections the created items will be inserted to, apart from the owning one
     * @param resumeDir In case of a resume request, the directory that containsthe old mapfile and data 
     * @param inputType The input type of the data (bibtex, csv, etc.), in case of local file
     * @param context The context
     * @param template whether to use template item
     * @throws Exception if error
     */
    @Override
    public void processUIImport(String filepath, Collection owningCollection, String[] otherCollections, String resumeDir, String inputType, Context context, final boolean template) throws Exception
	{
		final EPerson oldEPerson = context.getCurrentUser();
		final String[] theOtherCollections = otherCollections;
		final Collection theOwningCollection = owningCollection;
		final String theFilePath = filepath;
		final String theInputType = inputType;
		final String theResumeDir = resumeDir;
        final boolean useTemplateItem = template;
		
		Thread go = new Thread()
		{
            @Override
			public void run()
			{
				Context context = null;

				String importDir = null;
				EPerson eperson = null;
				
				try {
					
					// create a new dspace context
					context = new Context();
					eperson = ePersonService.find(context, oldEPerson.getID());
					context.setCurrentUser(eperson);
					context.turnOffAuthorisationSystem();
					
					boolean isResume = theResumeDir!=null;
					
					List<Collection> collectionList = new ArrayList<>();
	    			if (theOtherCollections != null) {
                        for (String colID : theOtherCollections) {
                            UUID colId = UUID.fromString(colID);
                            if (!theOwningCollection.getID().equals(colId)) {
                                Collection col = collectionService.find(context, colId);
                                if (col != null) {
                                    collectionList.add(col);
                                }
                            }
                        }
                    }

					importDir = ConfigurationManager.getProperty("org.dspace.app.batchitemimport.work.dir") + File.separator + "batchuploads" + File.separator + context.getCurrentUser().getID() + File.separator + (isResume?theResumeDir:(new GregorianCalendar()).getTimeInMillis());
					File importDirFile = new File(importDir);
					if (!importDirFile.exists()){
						boolean success = importDirFile.mkdirs();
						if (!success) {
							log.info("Cannot create batch import directory!");
							throw new Exception("Cannot create batch import directory!");
						}
					}
					
					String dataPath = null;
					String dataDir = null;
					
					if (theInputType.equals("saf")){ //In case of Simple Archive Format import (from remote url)
						dataPath = importDirFile + File.separator + "data.zip";
						dataDir = importDirFile + File.separator + "data_unzipped2" + File.separator;
					}
					else if (theInputType.equals("safupload")){ //In case of Simple Archive Format import (from upload file)
						FileUtils.copyFileToDirectory(new File(theFilePath), importDirFile);
						dataPath = importDirFile + File.separator + (new File(theFilePath)).getName();
						dataDir = importDirFile + File.separator + "data_unzipped2" + File.separator;
					}
					else { // For all other imports
						dataPath = importDirFile + File.separator + (new File(theFilePath)).getName();
    	    			dataDir = importDirFile + File.separator + "data" + File.separator;
					}
					
					//Clear these files, if a resume
					if (isResume){
						if (!theInputType.equals("safupload")) {
							(new File(dataPath)).delete();
						}
						(new File(importDirFile + File.separator + "error.txt")).delete();
						FileDeleteStrategy.FORCE.delete(new File(dataDir));
						FileDeleteStrategy.FORCE.delete(new File(importDirFile + File.separator + "data_unzipped" + File.separator));
					}

					//In case of Simple Archive Format import we need an extra effort to download the zip file and unzip it
					String sourcePath = null;
					if (theInputType.equals("saf")){ 
						OutputStream os = new FileOutputStream(dataPath);

						byte[] b = new byte[2048];
						int length;

						InputStream is = new URL(theFilePath).openStream();
						while ((length = is.read(b)) != -1) {
							os.write(b, 0, length);
						}

						is.close();
						os.close();

						sourcePath = unzip(new File(dataPath), dataDir);
						
						//Move files to the required folder
						FileUtils.moveDirectory(new File(sourcePath), new File(importDirFile + File.separator + "data_unzipped" + File.separator));
						FileDeleteStrategy.FORCE.delete(new File(dataDir));
						dataDir = importDirFile + File.separator + "data_unzipped" + File.separator;
					}
					else if (theInputType.equals("safupload")){ 
						sourcePath = unzip(new File(dataPath), dataDir);
						//Move files to the required folder
						FileUtils.moveDirectory(new File(sourcePath), new File(importDirFile + File.separator + "data_unzipped" + File.separator));
						FileDeleteStrategy.FORCE.delete(new File(dataDir));
						dataDir = importDirFile + File.separator + "data_unzipped" + File.separator;
					}
					
					//Create mapfile path
					String mapFilePath = importDirFile + File.separator + "mapfile";
					
					List<Collection> finalCollections = null;
					if (theOwningCollection != null){
                        finalCollections = new ArrayList<>();
						finalCollections.add(theOwningCollection);
                        finalCollections.addAll(collectionList);
					}
					
					setResume(isResume);
					
					if (theInputType.equals("saf") || theInputType.equals("safupload")){ //In case of Simple Archive Format import
						addItems(context, finalCollections, dataDir, mapFilePath, template);
					}
					else { // For all other imports (via BTE)
						addBTEItems(context, finalCollections, theFilePath, mapFilePath, useTemplateItem, theInputType, dataDir);
					}
					
					// email message letting user know the file is ready for
                    // download
                    emailSuccessMessage(context, eperson, mapFilePath);
                    
					context.complete();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
                    String exceptionString = ExceptionUtils.getStackTrace(e);

					try
                    {
						File importDirFile = new File(importDir+File.separator+"error.txt");
						PrintWriter errorWriter = new PrintWriter(importDirFile);
						errorWriter.print(exceptionString);
						errorWriter.close();
						
                        emailErrorMessage(eperson, exceptionString);
                        throw new Exception(e.getMessage());
                    }
                    catch (Exception e2)
                    {
                        // wont throw here
                    }
				}
				
				finally
                {
                    // Make sure the database connection gets closed in all conditions.
                	try {
						context.complete();
					} catch (SQLException sqle) {
						context.abort();
					}
                }
			}

		};

		go.isDaemon();
		go.start();
		
	}

    @Override
    public void emailSuccessMessage(Context context, EPerson eperson,
            String fileName) throws MessagingException
    {
        try
        {
            Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "bte_batch_import_success"));
            email.addRecipient(eperson.getEmail());
            email.addArgument(fileName);

            email.send();
        }
        catch (Exception e)
        {
            log.warn(LogManager.getHeader(context, "emailSuccessMessage", "cannot notify user of import"), e);
        }
    }

    @Override
    public void emailErrorMessage(EPerson eperson, String error)
            throws MessagingException
    {
        log.warn("An error occurred during item import, the user will be notified. " + error);
        try
        {
            Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "bte_batch_import_error"));
            email.addRecipient(eperson.getEmail());
            email.addArgument(error);
            email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/feedback");

            email.send();
        }
        catch (Exception e)
        {
            log.warn("error during item import error notification", e);
        }
    }
    
    @Override
    public List<BatchUpload> getImportsAvailable(EPerson eperson)
            throws Exception
    {
        File uploadDir = new File(getImportUploadableDirectory(eperson));
        if (!uploadDir.exists() || !uploadDir.isDirectory())
        {
            return null;
        }

        Map<String, BatchUpload> fileNames = new TreeMap<>();

        for (String fileName : uploadDir.list())
        {
            File file = new File(uploadDir + File.separator + fileName);
            if (file.isDirectory()){
            	
            	BatchUpload upload = new BatchUpload(file);
            	
            	fileNames.put(upload.getDir().getName(), upload);
            }
        }

        if (fileNames.size() > 0)
        {
            return new ArrayList<>(fileNames.values());
        }

        return null;
    }

    @Override
    public String getImportUploadableDirectory(EPerson ePerson)
            throws Exception
    {
        String uploadDir = ConfigurationManager.getProperty("org.dspace.app.batchitemimport.work.dir");
        if (uploadDir == null)
        {
            throw new Exception(
                    "A dspace.cfg entry for 'org.dspace.app.batchitemimport.work.dir' does not exist.");
        }
        String uploadDirBasePath = uploadDir + File.separator + "batchuploads" + File.separator;
        //Check for backwards compatibility with the old identifier
        File uploadDirectory = new File(uploadDirBasePath + ePerson.getLegacyId());
        if(!uploadDirectory.exists())
        {
            uploadDirectory = new File(uploadDirBasePath + ePerson.getID());
        }

        return uploadDirectory.getAbsolutePath();

    }

    @Override
    public void deleteBatchUpload(Context c, String uploadId) throws Exception
    {
    	String uploadDir = null;
    	String mapFilePath = null;

		uploadDir = getImportUploadableDirectory(c.getCurrentUser()) + File.separator + uploadId;
		mapFilePath = uploadDir + File.separator + "mapfile";
	
		this.deleteItems(c, mapFilePath);
		FileDeleteStrategy.FORCE.delete(new File(uploadDir));
    }

    @Override
    public String getTempWorkDir() {
        return tempWorkDir;
    }

    @Override
    public File getTempWorkDirFile()
            throws IOException
    {
        File tempDirFile = new File(getTempWorkDir());
        if(!tempDirFile.exists()) {
            boolean success = tempDirFile.mkdirs();
            if (!success)
            {
                throw new IOException("Work directory "
                         + tempDirFile.getAbsolutePath()
                         + " could not be created.");
            }
            else log.debug("Created directory " + tempDirFile.getAbsolutePath());
        }
        else log.debug("Work directory exists:  " + tempDirFile.getAbsolutePath());
        return tempDirFile;
    }

    @Override
    public void cleanupZipTemp() {
        System.out.println("Deleting temporary zip directory: " + tempWorkDir);
        log.debug("Deleting temporary zip directory: " + tempWorkDir);
        deleteDirectory(new File(tempWorkDir));
    }

    @Override
    public void setTest(boolean isTest) {
        this.isTest = isTest;
    }

    @Override
    public void setResume(boolean isResume) {
        this.isResume = isResume;
    }

    @Override
    public void setUseWorkflow(boolean useWorkflow) {
        this.useWorkflow = useWorkflow;
    }

    @Override
    public void setUseWorkflowSendEmail(boolean useWorkflowSendEmail) {
        this.useWorkflowSendEmail = useWorkflowSendEmail;
    }

    @Override
    public void setQuiet(boolean isQuiet) {
        this.isQuiet = isQuiet;
    }
}
