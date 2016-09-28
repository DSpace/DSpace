/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

/**
 *   
 *   Provides some batch editing capabilities for items in DSpace:
 *   	Metadata fields - Add, Delete
 *      Bitstreams - Add, Delete
 * 
 *   The design has been for compatibility with ItemImporter
 *      in the use of the DSpace archive format which is used to
 *      specify changes on a per item basis.  The directory names
 *      to correspond to each item are arbitrary and will only be
 *      used for logging purposes.  The reference to the item is
 *      from a required dc.identifier with the item handle to be 
 *      included in the dublin_core.xml (or similar metadata) file.
 *      
 *   Any combination of these actions is permitted in a single run of this class 
 *      The order of actions is important when used in combination.  
 *      It is the responsibility of the calling class (here, ItemUpdate)
 *      to register UpdateAction classes in the order to which they are
 *      to be performed.
 *      
 * 
 *   It is unfortunate that so much code needs to be borrowed 
 *      from ItemImport as it is not reusable in private methods, etc.
 *      Some of this has been placed into the MetadataUtilities class
 *      for possible reuse elsewhere. 
 *   
 * 
 * @author W. Hays based on a conceptual design by R. Rodgers
 *
 */
public class ItemUpdate {
	
	public static final String SUPPRESS_UNDO_FILENAME = "suppress_undo";

	public static final String CONTENTS_FILE = "contents";
	public static final String DELETE_CONTENTS_FILE = "delete_contents";

	public static String HANDLE_PREFIX = null;
	public static final Map<String, String> filterAliases = new HashMap<String, String>();
	
	public static boolean verbose = false;

    protected static final EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

	static
	{
		filterAliases.put("ORIGINAL", "org.dspace.app.itemupdate.OriginalBitstreamFilter");
		filterAliases.put("ORIGINAL_AND_DERIVATIVES", "org.dspace.app.itemupdate.OriginalWithDerivativesBitstreamFilter");
		filterAliases.put("TEXT", "org.dspace.app.itemupdate.DerivativeTextBitstreamFilter");		
		filterAliases.put("THUMBNAIL", "org.dspace.app.itemupdate.ThumbnailBitstreamFilter");		
	}
	          	                
    // File listing filter to check for folders
    static FilenameFilter directoryFilter = new FilenameFilter()
    {
        @Override
		public boolean accept(File dir, String n)
        {
            File f = new File(dir.getAbsolutePath() + File.separatorChar + n);
            return f.isDirectory();
        }
    };
 
    // File listing filter to check for files (not directories)
    static FilenameFilter fileFilter = new FilenameFilter()
    {
        @Override
		public boolean accept(File dir, String n)
        {
            File f = new File(dir.getAbsolutePath() + File.separatorChar + n);
            return (f.isFile());
        }
    };

    // instance variables
    protected ActionManager actionMgr = new ActionManager();
    protected List<String> undoActionList = new ArrayList<String>();
    protected String eperson;
   
    /**
     * 
     * @param argv commandline args
     */
    public static void main(String[] argv)
    {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

     //processing basis for determining items    
        //item-specific changes with metadata in source directory with dublin_core.xml files  
        options.addOption("s", "source", true, "root directory of source dspace archive ");
      
    //actions  on items     
        options.addOption("a", "addmetadata", true, "add metadata specified for each item; multiples separated by semicolon ';'");
        options.addOption("d", "deletemetadata", true, "delete metadata specified for each item");
        
        options.addOption("A", "addbitstreams", false, "add bitstreams as specified for each item");
        
        // extra work to get optional argument
        Option delBitstreamOption = new Option("D", "deletebitstreams", true, "delete bitstreams as specified for each item");
        delBitstreamOption.setOptionalArg(true);
        delBitstreamOption.setArgName("BitstreamFilter");
        options.addOption(delBitstreamOption);
                               
//other params        
        options.addOption("e", "eperson", true, "email of eperson doing the update");
        options.addOption("i", "itemfield", true, "optional metadata field that containing item identifier; default is dc.identifier.uri");
        options.addOption("F", "filter-properties", true, "filter class name; only for deleting bitstream");  
        options.addOption("v", "verbose", false, "verbose logging");
      
//special run states        
        options.addOption("t", "test", false, "test run - do not actually import items");
        options.addOption("P", "provenance", false, "suppress altering provenance field for bitstream changes");
        options.addOption("h", "help", false, "help");
        
        int status = 0;
        boolean isTest = false;
        boolean alterProvenance = true;
        String itemField = null;
        String metadataIndexName = null;
        
        Context context = null;
        ItemUpdate iu = new ItemUpdate();	

        try
        {       
	        CommandLine line = parser.parse(options, argv);
	
	        if (line.hasOption('h'))
	        {
	            HelpFormatter myhelp = new HelpFormatter();
	            myhelp.printHelp("ItemUpdate", options);
	            pr("");
	            pr("Examples:");
	            pr("  adding metadata:     ItemUpdate -e jsmith@mit.edu -s sourcedir -a dc.contributor -a dc.subject ");
	            pr("  deleting metadata:   ItemUpdate -e jsmith@mit.edu -s sourcedir -d dc.description.other");
	            pr("  adding bitstreams:   ItemUpdate -e jsmith@mit.edu -s sourcedir -A -i dc.identifier");
	            pr("  deleting bitstreams: ItemUpdate -e jsmith@mit.edu -s sourcedir -D ORIGINAL ");
	            pr("");
	
	            System.exit(0);
	        }
	        
	        if (line.hasOption('v'))
	        {
	        	verbose = true;
	        }

 
	        if (line.hasOption('P'))
	        {
	        	alterProvenance = false;
	        	pr("Suppressing changes to Provenance field option");
	        }

	        iu.eperson = line.getOptionValue('e'); // db ID or email
	        
	        if (!line.hasOption('s')) // item specific changes from archive dir
	        {
	        	pr("Missing source archive option");
	        	System.exit(1);
	        }
	        String sourcedir = line.getOptionValue('s');
	        
        	if (line.hasOption('t'))  //test
        	{
        		isTest = true;
	            pr("**Test Run** - not actually updating items.");
       		       	
	        }
        	
        	if (line.hasOption('i'))
        	{
        		itemField = line.getOptionValue('i');      		
        	}

	        if (line.hasOption('d'))
	        {
	        	String[] targetFields = line.getOptionValues('d'); 
	        	
	        	DeleteMetadataAction delMetadataAction = (DeleteMetadataAction) iu.actionMgr.getUpdateAction(DeleteMetadataAction.class);
	        	delMetadataAction.addTargetFields(targetFields);
	        	
	        	//undo is an add 
	        	for (String field : targetFields)
	        	{
	        		iu.undoActionList.add(" -a " + field + " ");
	        	}
	        	
	        	pr("Delete metadata for fields: ");
	        	for (String s : targetFields)
	        	{
	        		pr("    " + s);
	        	}
	        }
	        
    		if (line.hasOption('a'))
    		{
    			String[] targetFields = line.getOptionValues('a'); 
        	
    			AddMetadataAction addMetadataAction = (AddMetadataAction) iu.actionMgr.getUpdateAction(AddMetadataAction.class); 
    			addMetadataAction.addTargetFields(targetFields);
        		
        		//undo is a delete followed by an add of a replace record for target fields
	        	for (String field : targetFields)
	        	{
	        		iu.undoActionList.add(" -d " + field + " ");
	        	}
    			
	        	for (String field : targetFields)
	        	{
	        		iu.undoActionList.add(" -a " + field + " ");
	        	}
    			
	        	pr("Add metadata for fields: ");
	        	for (String s : targetFields)
	        	{
	        		pr("    " + s);
	        	}
        	}
    		
	        if (line.hasOption('D'))  // undo not supported 
	        {
	        	pr("Delete bitstreams ");
	        	
	        	String[] filterNames = line.getOptionValues('D');
	        	if ((filterNames != null) && (filterNames.length > 1))
	        	{
	        		pr("Error: Only one filter can be a used at a time.");
	        		System.exit(1);
	        	}
	        	
	        	String filterName = line.getOptionValue('D');
	        	pr("Filter argument: " + filterName);
	        	
	        	if (filterName == null)  // indicates using delete_contents files
	        	{
	        		DeleteBitstreamsAction delAction = (DeleteBitstreamsAction) iu.actionMgr.getUpdateAction(DeleteBitstreamsAction.class);
		        	delAction.setAlterProvenance(alterProvenance);
	        	}
	        	else
	        	{
	        	    // check if param is on ALIAS list
	        		String filterClassname = filterAliases.get(filterName);
	        		
	        		if (filterClassname == null)
	        		{
	        			filterClassname = filterName;
	        		}
	        			        		
	        	    BitstreamFilter filter = null; 

	        	    try
	        	    {
		    			Class<?> cfilter = Class.forName(filterClassname);
		    			pr("BitstreamFilter class to instantiate: " + cfilter.toString());
		    			
		    			filter =  (BitstreamFilter) cfilter.newInstance();  //unfortunate cast, an erasure consequence
	        	    }
	        	    catch(Exception e)
	        	    {
	        	    	pr("Error:  Failure instantiating bitstream filter class: " + filterClassname);
	        	    	System.exit(1);
	        	    }
	        	    
	        	    String filterPropertiesName = line.getOptionValue('F');
	        	    if (filterPropertiesName != null)  //not always required
	        	    {
	        	    	try
	        	    	{
	        	    		// TODO try multiple relative locations, e.g. source dir
	        	    		if (!filterPropertiesName.startsWith("/"))
	        	    		{
	        	    			filterPropertiesName = sourcedir + File.separator + filterPropertiesName;
	        	    		}
	        	    		
	        	    		filter.initProperties(filterPropertiesName);
	        	    	}
	        	    	catch(Exception e)
	        	    	{
		        	    	pr("Error:  Failure finding properties file for bitstream filter class: " + filterPropertiesName);
		        	    	System.exit(1);
	        	    	}
	        	    }
	        	    
	        		DeleteBitstreamsByFilterAction delAction = 
	        			(DeleteBitstreamsByFilterAction) iu.actionMgr.getUpdateAction(DeleteBitstreamsByFilterAction.class);
		        	delAction.setAlterProvenance(alterProvenance);
		        	delAction.setBitstreamFilter(filter);
	        	    //undo not supported
	        	}        	
	        }

	        if (line.hasOption('A'))
	        {
	        	pr("Add bitstreams ");    	        	
        		AddBitstreamsAction addAction = (AddBitstreamsAction) iu.actionMgr.getUpdateAction(AddBitstreamsAction.class);
	        	addAction.setAlterProvenance(alterProvenance);
	        	
	        	iu.undoActionList.add(" -D ");  // delete_contents file will be written, no arg required	        	
	        }    	        
	        	        
	        if (!iu.actionMgr.hasActions())
	        {
                pr("Error - an action must be specified");
                System.exit(1);
	        }
	        else  
	        {
	        	pr("Actions to be performed: ");
	        	
	        	for (UpdateAction ua : iu.actionMgr)
	        	{
	        		pr("    " + ua.getClass().getName());
	        	}
	        }
	        
	        pr("ItemUpdate - initializing run on " + (new Date()).toString());
	               	
	        context = new Context();  
	        iu.setEPerson(context, iu.eperson);	
	        context.turnOffAuthorisationSystem();
	        
	    	HANDLE_PREFIX = ConfigurationManager.getProperty("handle.canonical.prefix");
	    	if (HANDLE_PREFIX == null || HANDLE_PREFIX.length() == 0)
	    	{
	    		HANDLE_PREFIX = "http://hdl.handle.net/";
	    	}
	    		        
        	iu.processArchive(context, sourcedir, itemField, metadataIndexName, alterProvenance, isTest);		        	            

	        context.complete();  // complete all transactions
        }
        catch (Exception e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            e.printStackTrace();
            pr(e.toString());
            status = 1;
        }
        finally {
        	context.restoreAuthSystemState();
        }

        if (isTest)
        {
            pr("***End of Test Run***");
        }
        else
        {
        	pr("End.");
        	
        }
        System.exit(status);
    }

    /**
     * process an archive
     * @param context DSpace Context
     * @param sourceDirPath source path
     * @param itemField item field
     * @param metadataIndexName index name
     * @param alterProvenance whether to alter provenance
     * @param isTest test flag
     * @throws Exception if error
     */
    protected void processArchive(Context context, String sourceDirPath, String itemField,
    		String metadataIndexName, boolean alterProvenance, boolean isTest)
    throws Exception
    {
        // open and process the source directory
        File sourceDir = new File(sourceDirPath);

        if ((sourceDir == null) || !sourceDir.exists() || !sourceDir.isDirectory())
        {
            pr("Error, cannot open archive source directory " + sourceDirPath);
            throw new Exception("error with archive source directory " + sourceDirPath);
        }

        String[] dircontents = sourceDir.list(directoryFilter);  //just the names, not the path
        Arrays.sort(dircontents);
       
        //Undo is suppressed to prevent undo of undo
        boolean suppressUndo = false;
        File fSuppressUndo = new File(sourceDir, SUPPRESS_UNDO_FILENAME);
        if (fSuppressUndo.exists())
        {
        	suppressUndo = true;
        }

        File undoDir = null;  //sibling directory of source archive
        
    	if (!suppressUndo && !isTest) 
    	{
    		undoDir = initUndoArchive(sourceDir);	
    	}
        
    	int itemCount = 0;
    	int successItemCount = 0;
    	
        for (String dirname : dircontents)
        {
        	itemCount++;
    		pr("");
    		pr("processing item " + dirname);
    		
    		try
    		{
	    		ItemArchive itarch = ItemArchive.create(context, new File(sourceDir, dirname), itemField);
	    		
	    		for (UpdateAction action : actionMgr)
	    		{
	    			pr("action: " + action.getClass().getName());
	    			action.execute(context, itarch, isTest, suppressUndo);
	    			if (!isTest && !suppressUndo)
                    {
                        itarch.writeUndo(undoDir);
                    }
	    		}
	    		if (!isTest)
	    		{
    				Item item = itarch.getItem();
                    itemService.update(context, item);  //need to update before commit
	    		}
    			ItemUpdate.pr("Item " + dirname + " completed");
    			successItemCount++;
    		}
    		catch(Exception e)
    		{
    			pr("Exception processing item " + dirname + ": " + e.toString());
                e.printStackTrace();
    		}
        }  
        
        if (!suppressUndo && !isTest)
        {        	     	
        	StringBuilder sb = new StringBuilder("dsrun org.dspace.app.itemupdate.ItemUpdate ");
        	sb.append(" -e ").append(this.eperson);
        	sb.append(" -s ").append(undoDir);
        	
        	if (itemField != null)
        	{
        		sb.append(" -i ").append(itemField);
        	}
        	
         	if (!alterProvenance)
        	{
        		sb.append(" -P ");
        	}
        	if (isTest)
        	{
        		sb.append(" -t ");
        	}
        	
        	for (String actionOption : undoActionList)
        	{
        		sb.append(actionOption);
        	}   
        	
        	PrintWriter pw = null;
        	try
        	{
               	File cmdFile = new File (undoDir.getParent(), undoDir.getName() + "_command.sh");
	        	pw = new PrintWriter(new BufferedWriter(new FileWriter(cmdFile)));
	        	pw.println(sb.toString());
        	}
        	finally
        	{
        		pw.close();
        	}
        }
        
        pr("");
        pr("Done processing.  Successful items: " + successItemCount + " of " + itemCount + " items in source archive");
        pr("");
    }
           
    
    /**
     * 
     * to avoid overwriting the undo source tree on repeated processing
     * sequence numbers are added and checked 
     * 
     * @param sourceDir - the original source directory
     * @return the directory of the undo archive
     * @throws FileNotFoundException if file doesn't exist
     * @throws IOException if IO error
     */
    protected File initUndoArchive(File sourceDir)
	throws FileNotFoundException, IOException
	{				
		File parentDir = sourceDir.getCanonicalFile().getParentFile();
		if (parentDir == null)
		{
			throw new FileNotFoundException("Parent directory of archive directory not found; unable to write UndoArchive; no processing performed");    			
		}
		
		String sourceDirName = sourceDir.getName();
		int seqNo = 1;
		
		File undoDir = new File(parentDir, "undo_" + sourceDirName + "_" + seqNo);
		while (undoDir.exists())
		{
    		undoDir = new File(parentDir, "undo_" + sourceDirName+ "_" + ++seqNo); //increment
		}
		
		// create root directory
		if (!undoDir.mkdir())
		{
			pr("ERROR creating  Undo Archive directory " + undoDir.getCanonicalPath());
			throw new IOException("ERROR creating  Undo Archive directory " + undoDir.getCanonicalPath());
		}
		
        //Undo is suppressed to prevent undo of undo
        File fSuppressUndo = new File(undoDir, ItemUpdate.SUPPRESS_UNDO_FILENAME);
        try
        {
        	fSuppressUndo.createNewFile();
        }
        catch(IOException e)
        {
        	pr("ERROR creating Suppress Undo File " + e.toString());
        	throw e;
        }
        return undoDir;		
	}
	
	//private void write

    /**
     * Set EPerson doing import
     * @param context DSpace Context
     * @param eperson EPerson obj
     * @throws Exception if error
     */
    protected void setEPerson(Context context, String eperson)
    throws Exception
    {
        if (eperson == null)
        {
            pr("Error - an eperson to do the importing must be specified");
            pr(" (run with -h flag for details)");
            throw new Exception("EPerson not specified.");        }

        EPerson myEPerson = null;

        if (eperson.indexOf('@') != -1)
        {
            // @ sign, must be an email
            myEPerson = epersonService.findByEmail(context, eperson);
        }
        else
        {
            myEPerson = epersonService.find(context, UUID.fromString(eperson));
        }

        if (myEPerson == null)
        {
            pr("Error, eperson cannot be found: " + eperson);
            throw new Exception("Invalid EPerson");
        }

        context.setCurrentUser(myEPerson);
    }
    
    /**
     *   poor man's logging
	 *   As with ItemImport, API logging goes through log4j to the DSpace.log files
	 *   whereas the batch logging goes to the console to be captured there.
     * @param s String
     */
    static void pr(String s)
    {
    	System.out.println(s);
    }
    
    /**
     *  print if verbose flag is set
     * @param s String
     */
    static void prv(String s)
    {
    	if (verbose) 
    	{
        	System.out.println(s);
    	}
    }
    
} //end of class

