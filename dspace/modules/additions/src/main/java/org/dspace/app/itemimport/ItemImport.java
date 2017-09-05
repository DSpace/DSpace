/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.mail.MessagingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.ItemDataset;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSIndexer;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowManager;
import org.dspace.xmlworkflow.XmlWorkflowManager;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.TransformationEngine;
import gr.ekt.bte.core.TransformationResult;
import gr.ekt.bte.core.TransformationSpec;
import gr.ekt.bte.dataloader.FileDataLoader;
import gr.ekt.bteio.generators.DSpaceOutputGenerator;
import gr.ekt.bteio.loaders.OAIPMHDataLoader;
import uk.ac.edina.datashare.db.DbUpdate;
import uk.ac.edina.datashare.utils.DSpaceUtils;
import uk.ac.edina.datashare.utils.MetaDataUtil;
import uk.ac.edina.datashare.utils.MetadataChecker;
import uk.ac.edina.datashare.utils.VirusChecker;


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
public class ItemImport
{
    private static final Logger log = Logger.getLogger(ItemImport.class);

    private static boolean useWorkflow = false;

    private static boolean useWorkflowSendEmail = false;

    private static boolean isTest = false;

    private static boolean isResume = false;

    private static boolean isQuiet = false;

    private static boolean template = false;

    private static PrintWriter mapOut = null;

    private static final String tempWorkDir = ConfigurationManager.getProperty("org.dspace.app.batchitemimport.work.dir");

    static {
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

    // DATASHARE - start
    private MetadataChecker mdChecker;
    
    public ItemImport(){
        this.mdChecker = new MetadataChecker();
    }
    // DATASHARE - end

    // File listing filter to look for metadata files
    private static FilenameFilter metadataFileFilter = new FilenameFilter()
    {
        public boolean accept(File dir, String n)
        {
            return n.startsWith("metadata_");
        }
    };

    // File listing filter to check for folders
    private static FilenameFilter directoryFilter = new FilenameFilter()
    {
        public boolean accept(File dir, String n)
        {
            File item = new File(dir.getAbsolutePath() + File.separatorChar + n);
            return item.isDirectory();
        }
    };


    public static void main(String[] argv) throws Exception
    {
        DSIndexer.setBatchProcessingMode(true);
        Date startTime = new Date();
        int status = 0;

        try
        {
            // create an options object and populate it
            CommandLineParser parser = new PosixParser();

            Options options = new Options();

            options.addOption("a", "add", false, "add items to DSpace");
            options.addOption("b", "add-bte", false, "add items to DSpace via Biblio-Transformation-Engine (BTE)");
            options.addOption("r", "replace", false, "replace items in mapfile");
            options.addOption("d", "delete", false,
                    "delete items listed in mapfile");
            options.addOption("i", "inputtype", true, "input type in case of BTE import");
            options.addOption("s", "source", true, "source of items (directory)");
            options.addOption("z", "zip", true, "name of zip file");
            options.addOption("c", "collection", true,
                    "destination collection(s) Handle or database ID");
            options.addOption("m", "mapfile", true, "mapfile items in mapfile");
            options.addOption("e", "eperson", true,
                    "email of eperson doing importing");
            options.addOption("w", "workflow", false,
                    "send submission through collection's workflow");
            options.addOption("n", "notify", false,
                            "if sending submissions through the workflow, send notification emails");
            options.addOption("t", "test", false,
                    "test run - do not actually import items");
            options.addOption("p", "template", false, "apply template");
            options.addOption("R", "resume", false,
                    "resume a failed import (add only)");
            options.addOption("q", "quiet", false, "don't display metadata");

            options.addOption("h", "help", false, "help");

            CommandLine line = parser.parse(options, argv);

            String command = null; // add replace remove, etc
            String bteInputType = null; //ris, endnote, tsv, csv, bibtex
            String sourcedir = null;
            String mapfile = null;
            String eperson = null; // db ID or email
            String[] collections = null; // db ID or handles

            if (line.hasOption('h'))
            {
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("ItemImport\n", options);
                System.out
                       .println("\nadding items:    ItemImport -a -e eperson -c collection -s sourcedir -m mapfile");
                System.out
                        .println("\nadding items from zip file:    ItemImport -a -e eperson -c collection -s sourcedir -z filename.zip -m mapfile");
                System.out
                        .println("replacing items: ItemImport -r -e eperson -c collection -s sourcedir -m mapfile");
                System.out
                        .println("deleting items:  ItemImport -d -e eperson -m mapfile");
                System.out
                        .println("If multiple collections are specified, the first collection will be the one that owns the item.");

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

            if (line.hasOption('b'))
            {
                command = "add-bte";
            }

            if (line.hasOption('i'))
            {
                bteInputType = line.getOptionValue('i');
            }

            if (line.hasOption('w'))
            {
                useWorkflow = true;
                if (line.hasOption('n'))
                {
                    useWorkflowSendEmail = true;
                }
            }

            if (line.hasOption('t'))
            {
                isTest = true;
                System.out.println("**Test Run** - not actually importing items.");
            }

            if (line.hasOption('p'))
            {
                template = true;
            }

            if (line.hasOption('s')) // source
            {
                sourcedir = line.getOptionValue('s');
            }

            if (line.hasOption('m')) // mapfile
            {
                mapfile = line.getOptionValue('m');
            }

            if (line.hasOption('e')) // eperson
            {
                eperson = line.getOptionValue('e');
            }

            if (line.hasOption('c')) // collections
            {
                collections = line.getOptionValues('c');
            }

            if (line.hasOption('R'))
            {
                isResume = true;
                System.out
                        .println("**Resume import** - attempting to import items not already imported");
            }

            if (line.hasOption('q'))
            {
                isQuiet = true;
            }

            boolean zip = false;
            String zipfilename = "";
            if (line.hasOption('z'))
            {
                zip = true;
                zipfilename = sourcedir + System.getProperty("file.separator") + line.getOptionValue('z');
            }

            //By default assume collections will be given on the command line
            boolean commandLineCollections = true;
            // now validate
            // must have a command set
            if (command == null)
            {
                System.out
                        .println("Error - must run with either add, replace, or remove (run with -h flag for details)");
                System.exit(1);
            }
            else if ("add".equals(command) || "replace".equals(command))
            {
                if (sourcedir == null)
                {
                    System.out
                            .println("Error - a source directory containing items must be set");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }

                if (mapfile == null)
                {
                    System.out
                            .println("Error - a map file to hold importing results must be specified");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }

                if (eperson == null)
                {
                    System.out
                            .println("Error - an eperson to do the importing must be specified");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }

                if (collections == null)
                {
                    System.out.println("No collections given. Assuming 'collections' file inside item directory");
                    commandLineCollections = false;
                }
            }
            else if ("add-bte".equals(command))
            {
            	//Source dir can be null, the user can specify the parameters for his loader in the Spring XML configuration file

                if (mapfile == null)
                {
                    System.out
                            .println("Error - a map file to hold importing results must be specified");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }

                if (eperson == null)
                {
                    System.out
                            .println("Error - an eperson to do the importing must be specified");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }

                if (collections == null)
                {
                    System.out.println("No collections given. Assuming 'collections' file inside item directory");
                    commandLineCollections = false;
                }

                if (bteInputType == null)
                {
                    System.out
                            .println("Error - an input type (tsv, csv, ris, endnote, bibtex or any other type you have specified in BTE Spring XML configuration file) must be specified");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }
            }
            else if ("delete".equals(command))
            {
                if (eperson == null)
                {
                    System.out
                            .println("Error - an eperson to do the importing must be specified");
                    System.exit(1);
                }

                if (mapfile == null)
                {
                    System.out.println("Error - a map file must be specified");
                    System.exit(1);
                }
            }

            // can only resume for adds
            if (isResume && !"add".equals(command) && !"add-bte".equals(command))
            {
                System.out
                        .println("Error - resume option only works with the --add or the --add-bte commands");
                System.exit(1);
            }

            // do checks around mapfile - if mapfile exists and 'add' is selected,
            // resume must be chosen
            File myFile = new File(mapfile);

            if (!isResume && "add".equals(command) && myFile.exists())
            {
                System.out.println("Error - the mapfile " + mapfile
                        + " already exists.");
                System.out
                        .println("Either delete it or use --resume if attempting to resume an aborted import.");
                System.exit(1);
            }

            ItemImport myloader = new ItemImport();

            // create a context
            Context c = new Context();

            // find the EPerson, assign to context
            EPerson myEPerson = null;

            if (eperson.indexOf('@') != -1)
            {
                // @ sign, must be an email
                myEPerson = EPerson.findByEmail(c, eperson);
            }
            else
            {
                myEPerson = EPerson.find(c, Integer.parseInt(eperson));
            }

            if (myEPerson == null)
            {
                System.out.println("Error, eperson cannot be found: " + eperson);
                System.exit(1);
            }

            c.setCurrentUser(myEPerson);

            // find collections
            Collection[] mycollections = null;

            // don't need to validate collections set if command is "delete"
            // also if no collections are given in the command line
            if (!"delete".equals(command) && commandLineCollections)
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
                                .resolveToObject(c, collections[i]);

                        // resolved, now make sure it's a collection
                        if ((mycollections[i] == null)
                                || (mycollections[i].getType() != Constants.COLLECTION))
                        {
                            mycollections[i] = null;
                        }
                    }
                    // not a handle, try and treat it as an integer collection
                    // database ID
                    else if (collections[i] != null)
                    {
                        mycollections[i] = Collection.find(c, Integer
                                .parseInt(collections[i]));
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
                // If this is a zip archive, unzip it first
                if (zip) {
                    sourcedir = unzip(sourcedir, zipfilename);
                }


                c.turnOffAuthorisationSystem();

                if ("add".equals(command))
                {
                    myloader.addItems(c, mycollections, sourcedir, mapfile, template);
                }
                else if ("replace".equals(command))
                {
                    myloader.replaceItems(c, mycollections, sourcedir, mapfile, template);
                }
                else if ("delete".equals(command))
                {
                    myloader.deleteItems(c, mapfile);
                }
                else if ("add-bte".equals(command))
                {
                    myloader.addBTEItems(c, mycollections, sourcedir, mapfile, template, bteInputType, null);
                }

                // complete all transactions
                c.complete();
            }
            catch (Exception e)
            {
                c.abort();
                e.printStackTrace();
                System.out.println(e);
                status = 1;
            }

            // Delete the unzipped file
            try
            {
                if (zip)
                {
                    System.gc();
                    System.out.println("Deleting temporary zip directory: " + tempWorkDir);
                    ItemImport.deleteDirectory(new File(tempWorkDir));
                }
            }
            catch (Exception ex)
            {
                System.out.println("Unable to delete temporary zip archive location: " + tempWorkDir);
            }



            if (isTest)
            {
                System.out.println("***End of Test Run***");
            }
        }
        finally
        {
            DSIndexer.setBatchProcessingMode(false);
            Date endTime = new Date();
            System.out.println("Started: " + startTime.getTime());
            System.out.println("Ended: " + endTime.getTime());
            System.out.println("Elapsed time: " + ((endTime.getTime() - startTime.getTime()) / 1000) + " secs (" + (endTime.getTime() - startTime.getTime()) + " msecs)");
        }

        log.info("exit batch");
        System.exit(status);
    }

    /**
     * In this method, the BTE is instantiated. THe workflow generates the DSpace files
     * necessary for the upload, and the default item import method is called
     * @param c The contect
     * @param mycollections The collections the items are inserted to
     * @param sourceDir The filepath to the file to read data from
     * @param mapFile The filepath to mapfile to be generated
     * @param template
     * @param inputType The type of the input data (bibtex, csv, etc.)
     * @param workingDir The path to create temporary files (for command line or UI based)
     * @throws Exception
     */
    private void addBTEItems(Context c, Collection[] mycollections,
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
        	ItemImport myloader = new ItemImport();
        	myloader.addItems(c, mycollections, outputFolder, mapFile, template);
        }
    }

    public void addItemsAtomic(Context c, Collection[] mycollections, String sourceDir, String mapFile, boolean template) throws Exception {
        try {
            addItems(c, mycollections, sourceDir, mapFile, template);
        } catch (Exception addException) {
            log.error("AddItems encountered an error, will try to revert. Error: " + addException.getMessage());
            deleteItems(c, mapFile);
            c.commit();
            log.info("Attempted to delete partial (errored) import");
            throw addException;
        }
    }

    public void addItems(Context c, Collection[] mycollections,
            String sourceDir, String mapFile, boolean template) throws Exception
    {
        // create the mapfile
        File outFile = null;
        
        // DATASHARE - start
        List<Item> deposits = null;
        // DATASHARE - end
                
        try {
            Map<String, String> skipItems = new HashMap<String, String>(); // set of items to skip if in 'resume'
            // mode

            System.out.println("Adding items from directory: " + sourceDir);
            System.out.println("Generating mapfile: " + mapFile);

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
            
            // DATASHARE - do virus check
            deposits = new ArrayList<Item>(1000);
            if(!new VirusChecker(d).isVirusFree()){
                System.out.println(d + "has failed virus check");
                System.exit(1);
            }
            // DATASHARE end
            
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
                Collection [] clist;
                if (directoryFileCollections) {
                    String path = sourceDir + File.separatorChar + dircontents[i];
                    try {
                        Collection[] cols = processCollectionFile(c, path, "collections");
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

				// DATASHARE
                Item item = addItem(c, mycollections, sourceDir, dircontents[i], mapOut, template);
                System.out.println(i + " " + dircontents[i]);
                c.clearCache();
                
                
                deposits.add(item);
                // DATASHARE end
            }
        }

        } finally {
            if(mapOut!=null) {
                mapOut.flush();
                mapOut.close();
            }
            // DATASHARE - add batch import index and send license email
            if (!isTest){
                // wait for all dataset to be created
                for (Item item : deposits) {
                    Thread th = new ItemDataset(c, item).monitorDataset();
                    try{
                        th.join();
                    }
                    catch(InterruptedException ex){
                        System.out.println(ex);
                    }
                }                
                
                log.info("insertBatchImport " + mapFile);
                long batchId = DbUpdate.insertBatchImport(c, mapFile);
                
                StringBuffer items = new StringBuffer();
                for (Item item : deposits) {
                    items.append(MetaDataUtil.getTitle(item) + "\n"); 
                }

                Email mail = Email.getEmail(
                        I18nUtil.getEmailFilename(c.getCurrentLocale(), "batch_ingest"));
                mail.addArgument(items.toString());
                EPerson user = c.getCurrentUser();
                String url = ConfigurationManager.getProperty("dspace.url") +
                        "/deposit-agree?batch=" + batchId;
                mail.addArgument(url);
                mail.addRecipient(user.getEmail());
                mail.send();
                log.info("send email");
            }
            // DATASHARE end
        }
    }

    private void replaceItems(Context c, Collection[] mycollections,
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
                oldItem = (Item) HandleManager.resolveToObject(c, oldHandle);
            }
            else
            {
                oldItem = Item.find(c, Integer.parseInt(oldHandle));
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
            addItem(c, mycollections, sourceDir, newItemName, null, template);
            c.clearCache();
        }
    }

    private void deleteItems(Context c, String mapFile) throws Exception
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
                Item myitem = Item.find(c, Integer.parseInt(itemID));
                System.out.println("Deleting item " + itemID);
                deleteItem(c, myitem);
            }
            c.clearCache();
        }
    }

    /**
     * item? try and add it to the archive.
     * @param mycollections - add item to these Collections.
     * @param path - directory containing the item directories.
     * @param itemname handle - non-null means we have a pre-defined handle already
     * @param mapOut - mapfile we're writing
     */
    private Item addItem(Context c, Collection[] mycollections, String path,
            String itemname, PrintWriter mapOut, boolean template) throws Exception
    {
        String mapOutputString = null;

        System.out.println("Adding item from directory " + itemname);

        // create workspace item
        Item myitem = null;
        WorkspaceItem wi = null;

        if (!isTest)
        {
            wi = WorkspaceItem.create(c, mycollections[0], template);
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
                if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow")) {
                    if (useWorkflowSendEmail) {
                        XmlWorkflowManager.start(c, wi);
                    } else {
                        XmlWorkflowManager.startWithoutNotify(c, wi);
                    }
                } else {
                    if (useWorkflowSendEmail) {
                        WorkflowManager.start(c, wi);
                    }
                    else
                    {
                        WorkflowManager.startWithoutNotify(c, wi);
                    }
                }
                
                // DATASHARE - check metadata is valid
                this.mdChecker.isValid(c, myitem, mycollections[0]);
                // DATASHARE end
                
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
                    InstallItem.installItem(c, wi, myhandle);
                } catch (Exception e) {
                    wi.deleteAll();
                    log.error("Exception after install item, try to revert...", e);
                    throw e;
                }

                // find the handle, and output to map file
                myhandle = HandleManager.findHandle(c, myitem);

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
        if (mycollections.length > 1)
        {
            for (int i = 1; i < mycollections.length; i++)
            {
                if (!isTest)
                {
                    mycollections[i].addItem(myitem);
                }
            }
        }

        // made it this far, everything is fine, commit transaction
        if (mapOut != null)
        {
            mapOut.println(mapOutputString);
        }

        // DATASHARE - do datashare complete
        if (!isTest){
            DSpaceUtils.completeDeposit(c, myitem, ItemImport.class.toString());
            myitem.update();
        }
        // DATASHARE end

        c.commit();

        return myitem;
    }

    // remove, given the actual item
    private void deleteItem(Context c, Item myitem) throws Exception
    {
        if (!isTest)
        {
            Collection[] collections = myitem.getCollections();

            // Remove item from all the collections it's in
            for (int i = 0; i < collections.length; i++)
            {
                collections[i].removeItem(myitem);
            }
        }
    }

    // remove, given a handle
    private void deleteItem(Context c, String myhandle) throws Exception
    {
        // bit of a hack - to remove an item, you must remove it
        // from all collections it's a part of, then it will be removed
        Item myitem = (Item) HandleManager.resolveToObject(c, myhandle);

        if (myitem == null)
        {
            System.out.println("Error - cannot locate item - already deleted?");
        }
        else
        {
            deleteItem(c, myitem);
        }
    }

    ////////////////////////////////////
    // utility methods
    ////////////////////////////////////
    // read in the map file and generate a hashmap of (file,handle) pairs
    // DATASHARE - public static for batch import agreement
    public static Map<String, String> readMapFile(String filename) throws Exception
    {
        Map<String, String> myHash = new HashMap<String, String>();

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
    private void loadMetadata(Context c, Item myitem, String path)
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

    private void loadDublinCore(Context c, Item myitem, String filename)
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

    private void addDCValue(Context c, Item i, String schema, Node n) throws TransformerException, SQLException, AuthorizeException
    {
        String value = getStringValue(n); //n.getNodeValue();
        // compensate for empty value getting read as "null", which won't display
        if (value == null)
        {
            value = "";
        }
        // //getElementData(n, "element");
        
        // DATASHARE - start
        String altSchema = getAttributeValue(n, "schema");
        if(altSchema != null && altSchema.length() > 0){
            schema = altSchema;
        }
        // DATASHARE - end
        
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

        if (!isTest)
        {
            // DATASHARE - start
            if(this.mdChecker.isAllowedMetadataField(c, i, schema, element, qualifier)){
                i.addMetadata(schema, element, qualifier, language, value);
            }
            else{
                System.out.println("Metadata field " + DSpaceUtils.getMdString(element, qualifier) + " not allowed.");
            }
            // DATASHARE - end
        }
        else
        {
            // If we're just test the import, let's check that the actual metadata field exists.
        	MetadataSchema foundSchema = MetadataSchema.find(c,schema);

        	if (foundSchema == null)
        	{
        		System.out.println("ERROR: schema '"+schema+"' was not found in the registry.");
        		return;
        	}

        	int schemaID = foundSchema.getSchemaID();
        	MetadataField foundField = MetadataField.findByElement(c, schemaID, element, qualifier);

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
     */

    private Collection[] processCollectionFile(Context c, String path, String filename) throws IOException, SQLException
    {
        File file = new File(path + File.separatorChar + filename);
        ArrayList<Collection> collections = new ArrayList<Collection>();
        Collection[] result = null;
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
                        obj = HandleManager.resolveToObject(c, line);
                        if (obj == null || obj.getType() != Constants.COLLECTION)
                        {
                            obj = null;
                        }
                    }
                    else
                    {
                        obj = Collection.find(c, Integer.parseInt(line));
                    }

                    if (obj == null) {
                        throw new IllegalArgumentException("Cannot resolve " + line + " to a collection.");
                    }
                    collections.add((Collection)obj);

                }

                result = new Collection[collections.size()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = collections.get(i);
                }
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
     * Read in the handle file or return null if empty or doesn't exist
     */
    private String processHandleFile(Context c, Item i, String path, String filename)
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
     */
    private List<String> processContentsFile(Context c, Item i, String path,
            String filename) throws SQLException, IOException,
            AuthorizeException
    {
        File contentsFile = new File(path + File.separatorChar + filename);
        String line = "";
        List<String> options = new ArrayList<String>();

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
            String[] dirListing = new File(path).list();
            for (String fileName : dirListing)
            {
                if (!"dublin_core.xml".equals(fileName) && !fileName.equals("handle") && !fileName.startsWith("metadata_"))
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
     * @param c
     * @param i
     * @param path
     * @param fileName
     * @param bundleName
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    private void processContentFileEntry(Context c, Item i, String path,
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

            // now add the bitstream
            bs = targetBundle.createBitstream(bis);

            bs.setName(fileName);

            // Identify the format
            // FIXME - guessing format guesses license.txt incorrectly as a text
            // file format!
            BitstreamFormat bf = FormatIdentifier.guessFormat(c, bs);
            bs.setFormat(bf);

            // Is this a the primary bitstream?
            if (primary)
            {
                targetBundle.setPrimaryBitstreamID(bs.getID());
                targetBundle.update();
            }

            bs.update();
        }

        bis.close();
    }

    /**
     * Register the bitstream file into DSpace
     *
     * @param c
     * @param i
     * @param assetstore
     * @param bitstreamPath the full filepath expressed in the contents file
     * @param bundleName
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    private void registerBitstream(Context c, Item i, int assetstore,
            String bitstreamPath, String bundleName, String description )
        	throws SQLException, IOException, AuthorizeException
    {
        // TODO validate assetstore number
        // TODO make sure the bitstream is there

        Bitstream bs = null;
        String newBundleName = bundleName;

        if (bundleName == null)
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
	        Bundle[] bundles = i.getBundles(newBundleName);
	        Bundle targetBundle = null;

	        if( bundles.length < 1 )
	        {
	            // not found, create a new one
	            targetBundle = i.createBundle(newBundleName);
	        }
	        else
	        {
	            // put bitstreams into first bundle
	            targetBundle = bundles[0];
	        }

	        // now add the bitstream
	        bs = targetBundle.registerBitstream(assetstore, bitstreamPath);

	        // set the name to just the filename
	        int iLastSlash = bitstreamPath.lastIndexOf('/');
	        bs.setName(bitstreamPath.substring(iLastSlash + 1));

	        // Identify the format
	        // FIXME - guessing format guesses license.txt incorrectly as a text file format!
	        BitstreamFormat bf = FormatIdentifier.guessFormat(c, bs);
	        bs.setFormat(bf);
	        bs.setDescription(description);

	        bs.update();
        }
    }

    /**
     *
     * Process the Options to apply to the Item. The options are tab delimited
     *
     * Options:
     *      48217870-MIT.pdf        permissions: -r 'MIT Users'     description: Full printable version (MIT only)
     *      permissions:[r|w]-['group name']
     *      description: 'the description of the file'
     *
     *      where:
     *          [r|w] (meaning: read|write)
     *          ['MIT Users'] (the group name)
     *
     * @param c
     * @param myItem
     * @param options
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void processOptions(Context c, Item myItem, List<String> options)
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
                    myGroup = Group.findByName(c, groupName);
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
                Bitstream[] bitstreams = myItem.getNonInternalBitstreams();
                for (int j = 0; j < bitstreams.length && notfound; j++)
                {
                    if (bitstreams[j].getName().equals(bitstreamName))
                    {
                        bs = bitstreams[j];
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
                    bs.setDescription(thisDescription);
                    bs.update();
                }
            }
        }
    }

    /**
     * Set the Permission on a Bitstream.
     *
     * @param c
     * @param g
     * @param actionID
     * @param bs
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void setPermission(Context c, Group g, int actionID, Bitstream bs)
            throws SQLException, AuthorizeException
    {
        if (!isTest)
        {
            // remove the default policy
            AuthorizeManager.removeAllPolicies(c, bs);

            // add the policy
            ResourcePolicy rp = ResourcePolicy.create(c);

            rp.setResource(bs);
            rp.setAction(actionID);
            rp.setGroup(g);

            rp.update();
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
     * @param n
     * @param name
     * @return
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
     * @param node
     * @return
     */
    private String getStringValue(Node node)
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
     */
    private static Document loadXML(String filename) throws IOException,
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
    private static boolean deleteDirectory(File path)
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

    public static String unzip(File zipfile) throws IOException {
    	return unzip(zipfile, null);
    }
    
    public static String unzip(File zipfile, String destDir) throws IOException {
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
                if (!new File(zipDir + entry.getName()).mkdir())
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
                        if(sourceDirForZip == sourcedir) {
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
        
        if(sourceDirForZip != sourcedir) {
            sourcedir = sourceDirForZip;
            System.out.println("Set sourceDir using path inside of Zip: " + sourcedir);
            log.info("Set sourceDir using path inside of Zip: " + sourcedir);
        }

        return sourcedir;
    }

    public static String unzip(String sourcedir, String zipfilename) throws IOException {
        File zipfile = new File(sourcedir + File.separator + zipfilename);
        return unzip(zipfile);
    }
    
    /**
     * Generate a random filename based on current time
     * @param hidden: add . as a prefix to make the file hidden
     * @return the filename
     */
    private static String generateRandomFilename(boolean hidden)
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
     * @throws Exception
     */
    public static void processUIImport(String filepath, Collection owningCollection, String[] otherCollections, String resumeDir, String inputType, Context context) throws Exception
	{
		final EPerson oldEPerson = context.getCurrentUser();
		final String[] theOtherCollections = otherCollections;
		final Collection theOwningCollection = owningCollection;
		final String theFilePath = filepath;
		final String theInputType = inputType;
		final String theResumeDir = resumeDir;
		
		Thread go = new Thread()
		{
			public void run()
			{
				Context context = null;

				String importDir = null;
				EPerson eperson = null;
				
				try {
					
					// create a new dspace context
					context = new Context();
					eperson = EPerson.find(context, oldEPerson.getID());
					context.setCurrentUser(eperson);
					context.setIgnoreAuthorization(true);
					
					boolean isResume = theResumeDir!=null;
					
					List<Collection> collectionList = new ArrayList<Collection>();
	    			if (theOtherCollections != null){
	    				for (String colID : theOtherCollections){
	    					int colId = Integer.parseInt(colID);
	    					if (colId != theOwningCollection.getID()){
	    						Collection col = Collection.find(context, colId);
	    						if (col != null){
	    							collectionList.add(col);
	    						}
	    					}
	    				}
	    			}
	    			Collection[] otherCollections = collectionList.toArray(new Collection[collectionList.size()]);
	    			
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
					
					Collection[] finalCollections = null;
					if (theOwningCollection != null){
						finalCollections = new Collection[otherCollections.length + 1];
						finalCollections[0] = theOwningCollection;
						for (int i=0; i<otherCollections.length; i++){
							finalCollections[i+1] = otherCollections[i];
						}
					}
					
					ItemImport myloader = new ItemImport();
					myloader.isResume = isResume;
					
					if (theInputType.equals("saf") || theInputType.equals("safupload")){ //In case of Simple Archive Format import
						myloader.addItems(context, finalCollections, dataDir, mapFilePath, template);
					}
					else { // For all other imports (via BTE)
						myloader.addBTEItems(context, finalCollections, theFilePath, mapFilePath, template, theInputType, dataDir);
					}
					
					// email message letting user know the file is ready for
                    // download
                    emailSuccessMessage(context, eperson, mapFilePath);
                    
					context.complete();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					String exceptionString = ExceptionUtils.getStackTrace(e);
					
					// abort all operations
	                if (mapOut != null)
	                {
	                    mapOut.close();
	                }

	                mapOut = null;
	                
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
                    // close the mapfile writer
                    if (mapOut != null)
                    {
                        mapOut.close();
                    }

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

    /**
     * Since the BTE batch import is done in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplish this
     * communication with email instead. Send a success email once the batch
     * import is complete
     *
     * @param context
     *            - the current Context
     * @param eperson
     *            - eperson to send the email to
     * @param fileName
     *            - the filepath to the mapfile created by the batch import
     * @throws MessagingException
     */
    public static void emailSuccessMessage(Context context, EPerson eperson,
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

    /**
     * Since the BTE batch import is done in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplis this
     * communication with email instead. Send an error email if the batch
     * import fails
     *
     * @param eperson
     *            - EPerson to send the error message to
     * @param error
     *            - the error message
     * @throws MessagingException
     */
    public static void emailErrorMessage(EPerson eperson, String error)
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
    
    
    public static List<BatchUpload> getImportsAvailable(EPerson eperson)
            throws Exception
    {
        File uploadDir = new File(getImportUploadableDirectory(eperson.getID()));
        if (!uploadDir.exists() || !uploadDir.isDirectory())
        {
            return null;
        }

        Map<String, BatchUpload> fileNames = new TreeMap<String, BatchUpload>();

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
            return new ArrayList<BatchUpload>(fileNames.values());
        }

        return null;
    }
    
    public static String getImportUploadableDirectory(int ePersonID)
            throws Exception
    {
        String uploadDir = ConfigurationManager.getProperty("org.dspace.app.batchitemimport.work.dir");
        if (uploadDir == null)
        {
            throw new Exception(
                    "A dspace.cfg entry for 'org.dspace.app.batchitemimport.work.dir' does not exist.");
        }

        return uploadDir + File.separator + "batchuploads" + File.separator + ePersonID;

    }
    
    public void deleteBatchUpload(Context c, String uploadId) throws Exception
    {
    	String uploadDir = null;
    	String mapFilePath = null;

		uploadDir = ItemImport.getImportUploadableDirectory(c.getCurrentUser().getID()) + File.separator + uploadId;
		mapFilePath = uploadDir + File.separator + "mapfile";
	
		this.deleteItems(c, mapFilePath);
		// complete all transactions
        c.commit();
        
		FileDeleteStrategy.FORCE.delete(new File(uploadDir));
    }

    public static String getTempWorkDir() {
        return tempWorkDir;
    }

    public static File getTempWorkDirFile() {
        File tempDirFile = new File(getTempWorkDir());
        if(!tempDirFile.exists()) {
            tempDirFile.mkdirs();
        }
        return tempDirFile;
    }

    public static void cleanupZipTemp() {
        System.out.println("Deleting temporary zip directory: " + tempWorkDir);
        ItemImport.deleteDirectory(new File(tempWorkDir));
    }
}
