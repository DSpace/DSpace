/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package gr.ekt.repositories.dspace.export;


import gr.ekt.repositories.dspace.utils.CitationFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;

import net.sf.json.JSONObject;


/**
 * Tool for exporting DSpace metadata in cls format
 * 
 */
public class CslJsonExport
{
    private static Properties repoTocslMetadata;
    private static Properties repoTocslTypes;
               
    private static final List<String> cslNameList = Arrays.asList("author","editor","translator","recipient","interviewer","publisher","composer","original-publisher","original-author","container-author","collection-editor");
    private static final List<String> cslDateList = Arrays.asList("accessed","container","event-date","issued","original-date","submitted");
    
    private String itemHandle;
    
    public CslJsonExport(String itemHandle) throws Exception
    {
    	this.itemHandle = itemHandle;
    }
    
    public String generateJson() throws SQLException, IOException{
    	Context context = new Context();

        init(context);
        
        DSpaceObject o = HandleManager.resolveToObject(context, this.itemHandle);
        String jsonStr = "";
        if ((o != null) && o instanceof Item)
        {
        	jsonStr = createCslJsonMetadata(context, (Item) o).toString();   
        }
        else
        {
            System.err.println(this.itemHandle
                    + " is not a valid item Handle");
            System.exit(1);
        }
        
        context.abort();
        
        return jsonStr;
        
    }
    
    public static void main(String[] args) throws Exception
    {
        Context context = new Context();

        init(context);

        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("c", "collection", true,
                "Handle of collection to export");
        options.addOption("i", "item", true, "Handle of item to export");
        options.addOption("a", "all", false, "Export all items in the archive");
        options.addOption("d", "destination", true, "Destination directory");
        options.addOption("h", "help", false, "Help");
        options.addOption("f", "format", true, "format");
        options.addOption("o", "citeOutputFormat", true, "citeOutputFormat");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("jsonexport", options);
            System.out.println("\nExport a collection:  jsonexport -c hdl:123.456/789");
            System.out.println("Export an item:       jsonexport -i hdl:123.456/890");
            System.out.println("Export everything:    jsonexport -a");
            System.out.println("Export everything:    jsonexport -a");
            System.out.println("Export citations in text output format:    jsonexport -o text");
            System.out.println("Export citations in html output format:    jsonexport -o html");
            System.out.println("Export citations in ieee format:    jsonexport -f ieee");

            System.exit(0);
        }

        String dest = "";
        String format = "ieee";
        String citeOutputFormat = "text";
        
        
        if (line.hasOption('f'))
        {
            format = line.getOptionValue('f');
            System.out.println("F: " + format);
        }
        
        if (line.hasOption('o'))
        {
            citeOutputFormat = line.getOptionValue('o');
            
            System.out.println("O: " + citeOutputFormat);
        }
        
        if(!"text".equals(citeOutputFormat) && !"html".equals(citeOutputFormat)){
            citeOutputFormat = "text";
        }

        if (line.hasOption('d'))
        {
            dest = line.getOptionValue('d');

            // Make sure it ends with a file separator
            if (!dest.endsWith(File.separator))
            {
                dest = dest + File.separator;
            }
        }

        if (line.hasOption('i'))
        {
            String handle = getHandleArg(line.getOptionValue('i'));

            // Exporting a single item
            DSpaceObject o = HandleManager.resolveToObject(context, handle);

            if ((o != null) && o instanceof Item)
            {
                String dir = createDir(dest, o.getHandle());
                CitationFormat ct = new CitationFormat((Item) o, format, citeOutputFormat, citeOutputFormat);
		ct.postToCiteProc();
                System.out.println("Exporting item hdl:" + o.getHandle());
                writeJSON(ct.getJsonInput(), ct.getExport(), dir, citeOutputFormat);
                System.exit(0);
            }
            else
            {
                System.err.println(line.getOptionValue('i')
                        + " is not a valid item Handle");
                System.exit(1);
            }
        }

        ItemIterator items = null;
        try
        {
            String handle = "collection";
            if (line.hasOption('c'))
            {
                handle = getHandleArg(line.getOptionValue('c'));
                System.out.println("C: " + line.getOptionValue('c'));
                // Exporting a collection's worth of items
                DSpaceObject o = HandleManager.resolveToObject(context, handle);

                if ((o != null) && o instanceof Collection)
                {
                    items = ((Collection) o).getItems();
                }
                else
                {
                    System.err.println(line.getOptionValue('c')
                            + " is not a valid collection Handle");
                    System.exit(1);
                }
            }

            if (line.hasOption('a'))
            {
                items = Item.findAll(context);
            }

            if (items == null)
            {
                System.err.println("Nothing to export specified!");
                System.exit(1);
            }

            String dir = createDir(dest, handle);
            
            ArrayList<Item> collectionItemsList = new ArrayList<Item>();
    
            while (items.hasNext()){
                Item currentItem = items.next();

                if(currentItem.getName() != null && currentItem.getHandle() != null)            
                    collectionItemsList.add(currentItem);
            }

            Item[] collectionItemsArray = (Item[]) collectionItemsList.toArray(new Item[collectionItemsList.size()]);

            CitationFormat ct = new CitationFormat(collectionItemsArray, format, citeOutputFormat, citeOutputFormat);
            ct.postToCiteProc();            
            writeJSON(ct.getJsonInput(), ct.getExport(), dir, citeOutputFormat);
                             
        }
        finally
        {
            if (items != null)
            {
                items.close();
            }
        }
        
        context.abort();
        System.exit(0);
    }

    /**
     * Initialise various variables, read in config etc.
     * 
     * @param context
     *            DSpace context
     */
    private static void init(Context context) throws SQLException, IOException
    {
        // get path to repo->CSL metadata map info file
        String configMetadataMappingFile = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator + "repo2csl-metadata.cfg";
        
        // get path to repo->CSL types map info file
        String configTypesMappingFile = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator + "repo2csl-types.cfg";

        // Read it in
        InputStream ismtdt  = null;
        InputStream istp  = null;
        
        try
        {
            ismtdt = new FileInputStream(configMetadataMappingFile);
            repoTocslMetadata = new Properties();
            repoTocslMetadata.load(ismtdt);
            
            istp = new FileInputStream(configTypesMappingFile);
            repoTocslTypes = new Properties();
            repoTocslTypes.load(istp);
        }
        finally
        {
            if (ismtdt != null)
            {
                try
                {
                	ismtdt.close();
                }
                catch (IOException ioe)
                {
                }
            }
            
            if (istp != null)
            {
                try
                {
                	istp.close();
                }
                catch (IOException ioe)
                {
                }
            }
        }
    }

    /**
     * Write out the AIP for the given item to the given directory. A new
     * directory will be created with the Handle (URL-encoded) as the directory
     * name, and inside, a mets.xml file written, together with the bitstreams.
     * 
     * @param context
     *            DSpace context to use
     * @param item
     *            Item to write
     * @param dest
     *            destination directory
     * @throws Exception 
     */
    public static String createDir(String dest, String folderName)
           throws Exception
    {
        
        // Create aip directory
        java.io.File aipDir = new java.io.File(dest + URLEncoder.encode("hdl:" + folderName, "UTF-8"));

        if (!aipDir.mkdir())
        {
            // Couldn't make the directory for some reason
            throw new IOException("Couldn't create " + aipDir.toString());
        }
        
        return aipDir.toString();       

    }

    /**
     * Write JSON metadata corresponding to the metadata for an item
     * 
     * @param context
     *            DSpace context
     * @param item
     *            DSpace item to create JSON file for
     * @param os
     *            A stream to write JSON package to (UTF-8 encoding will be used)
     * @throws Exception 
     */
    public static void writeJSON(String json, String citation, String dir, String citeOutputFormat)
            throws Exception
    {
        // Write the JSON file
        FileOutputStream outJson = null;
        FileOutputStream outCitation = null;
        
        try
        {
            try
            {
                outJson = new FileOutputStream(dir + java.io.File.separator + "jsonExport.json");
                if("text".equals(citeOutputFormat)){
                    outCitation = new FileOutputStream(dir + java.io.File.separator + "citationExport.txt");
                }
                if("html".equals(citeOutputFormat)){
                    outCitation = new FileOutputStream(dir + java.io.File.separator + "citationExport.html");
                }
                            
                //export the cls json format
                outJson.write(json.toString().getBytes());
                
                //export the metadata in the defined format
                outCitation.write(citation.getBytes());

            }
            catch (IOException e)
            {
                // We don't pass up a MetsException, so callers don't need to
                // know the details of the METS toolkit
                e.printStackTrace();
                throw new IOException(e.getMessage(), e);
            }
        }
        finally
        {
            if (outJson != null)
            {
                outJson.close();
            }
            
            if (outCitation != null)
            {
                outCitation.close();
            }
        }
    }
    

    /**
     * Create JSON file from the item metadata
     * 
     * @param item
     *            the item
     * @param xmlData
     *            xmlData to add MODS to.
     * @throws SQLException 
     */
    private static String createCslJsonMetadata(Context context, Item item) throws SQLException
    {

    	// Get all existing Schemas
        MetadataSchema[] schemas = MetadataSchema.findAll(context);
        String items = "";
        JSONObject itemobj = new JSONObject();
        for (int i = 0; i < schemas.length; i++)
        {   
            String schemaName = schemas[i].getName();
            // Get all fields for the given schema
            MetadataField[] fields = MetadataField.findAllInSchema(context, schemas[i].getSchemaID());
            String ID = item.getHandle();
                        
            itemobj.put("id", ID);
            for (int j = 0; j < fields.length; j++)
            {                
                DCValue[] metadatavalues = item.getMetadata(schemaName, fields[j].getElement(), fields[j].getQualifier(), Item.ANY);

                for (int k = 0; k < metadatavalues.length; k++)
                {
                    String propName = schemaName + "." + ((metadatavalues[k].qualifier == null) ? metadatavalues[k].element
                            : (metadatavalues[k].element + "." + metadatavalues[k].qualifier));

                    // Get the corresponding csl property name according to the mapping with the properties of the repository
                    String clsMetadataMapping = repoTocslMetadata.getProperty(propName);
                    System.err.println("Property: " + propName + " clsMetadataMapping: " + clsMetadataMapping);

                    if (clsMetadataMapping == null)
                    {
                        System.err.println("WARNING: No CSL metadata mapping for " + propName);
                    }
                    else
                    {
                    	String value = metadatavalues[k].value;
                    	
                    	if("type".equals(clsMetadataMapping)){
                    		String searchValue = value.replaceAll(" ", "_");
                        	String cslTypeMapping = repoTocslTypes.getProperty(searchValue);
                        	
                        	if (cslTypeMapping == null)
                        		System.err.println("WARNING: No CSL type mapping for " + value);
                        	else{
                        		value = cslTypeMapping;
                        		
                        	}
                        }
            
                        // Replace all $'s with \$ so it doesn't trip up the replaceAll!
                        if (value != null && value.length() > 0)
                        {
                            // RegExp note: Yes, there really does need to be this many backslashes!
                            // To have \$ inserted in the replacement, both the backslash and the dollar
                            // have to be escaped (backslash) - so the replacemenet string has to be
                            // passed as \\\$. All of those backslashes then have to escaped in the literal
                            // for them to be in string used!!!
                            value = value.replaceAll("\\$", "\\\\\\$");
                        }
                        
                        //Specific format for name variables as defined by the csl specifiacations -- http://citationstyles.org/downloads/specification.html#name-variables
                        if(cslNameList.contains(clsMetadataMapping)){
                            JSONObject names = new JSONObject();
                            String[] values = getFamilyGivenNames(value);
                            names.put("family", values[0].trim());
                            if(values.length > 1){
                            	names.put("given", values[1].trim());
                            }
                            if (k==0)
                            	itemobj.accumulate(clsMetadataMapping, "[" + names + "]");
                            else
                            	itemobj.accumulate(clsMetadataMapping, names);
                        }
                       
                        //Specific format for date variables as defined by the csl specifiacations -- http://citationstyles.org/downloads/specification.html#date-variables
                        else if (cslDateList.contains(clsMetadataMapping)) {
                        	JSONObject dateParts = new JSONObject();
                        	String datePartsWrapper = "[[";
                        	String[] values = getDateParts(value);
                        	for (int l=0; l<values.length; l++){
                        		datePartsWrapper = datePartsWrapper + values[l];
                        		if(l<values.length -1){
                        			datePartsWrapper = datePartsWrapper + ", ";
                        		}
                        	}
                        	datePartsWrapper = datePartsWrapper + "]]";
                        	
                        	dateParts.accumulate("date-parts", datePartsWrapper);
                        	itemobj.accumulate(clsMetadataMapping, dateParts);
						}
                        else
                        	itemobj.accumulate(clsMetadataMapping, Utils.addEntities(value));
                    }
                    
                    if(!cslNameList.contains(clsMetadataMapping) && !cslDateList.contains(clsMetadataMapping))
                        break;
                }
            }
            
            items = "\"" + ID + "\"" + ": " + itemobj;
        }
        return items;
    }

    /**
     * Get the handle from the command line in the form 123.456/789. Doesn't
     * matter if incoming handle has 'hdl:' or 'http://hdl....' before it.
     * 
     * @param original
     *            Handle as passed in by user
     * @return Handle as can be looked up in our table
     */
    private static String getHandleArg(String original)
    {
        if (original.startsWith("hdl:"))
        {
            return original.substring(4);
        }

        if (original.startsWith("http://hdl.handle.net/"))
        {
            return original.substring(22);
        }

        return original;
    }
    
    // Handle names values according to csl specifications
    private static String[] getFamilyGivenNames(String FullName)
    {
    	String[] names = null;
    	if(FullName.contains(",")){
    		
    		names = FullName.split(",");
	    }
    	else{
    		names = new String[1];
    		names[0] = FullName;
    	}
    	return names;
    }
   
    //Handles dates formats YYYY-MM-DD or YYYY-MM-DDThourZ YYYY according to csl specifications
    private static String[] getDateParts(String date)
    {
    	String[] dateParts = null;
    	if(date.contains("T")){
    		date = date.substring(0,date.indexOf("T"));
    	}
    	
    	if(date.contains("-")){
    		
    		dateParts = date.split("-");
	    }
    	else{
    		dateParts = new String[1];
    		dateParts[0] = date;
    	}
    	return dateParts;
    }
}