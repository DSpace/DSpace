/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.folderimport;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.mail.MessagingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.dspace.app.itemimport.ItemImport;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.folderimport.exception.FolderMetadataException;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.WorkflowManager;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.WorkflowException;
import org.dspace.xmlworkflow.XmlWorkflowManager;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Classe baseada em {@link ItemImport}<br>
 * Modificada para atender importação de itens via pasta, por módulo WEB
 * @author Márcio Ribeiro Gurgel do Amaral
 *
 */
public class FolderMetadataProcessor
{
    private static final Logger log = Logger.getLogger(FolderMetadataProcessor.class);

    private boolean useWorkflow = false;

    private boolean useWorkflowSendEmail = false;

    private boolean isTest = false;

    private boolean isQuiet = false;

    private PrintWriter mapOut = null;
    
    private PrintWriter errorOut = null;

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


    public void addItems(Context c, Collection[] mycollections,
            String sourceDir, String mapFile, String errorFile, boolean template, boolean isTestParam, boolean isResume, boolean useWorkflowSendEmailParam, boolean useWorkflowParam) throws Exception
    {
    	this.isTest = isTestParam;
    	this.useWorkflow = useWorkflowParam;
    	this.useWorkflowSendEmail = useWorkflowSendEmailParam;
    	
    	
        Map<String, String> skipItems = new HashMap<String, String>(); // set of items to skip if in 'resume'
        // mode

        log.info("Adding items from directory: " + sourceDir);
        log.info("Generating mapfile: " + mapFile);

        // create the mapfile
        File outFile = null;

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

            /** Writes source folder **/
            mapOut.write(sourceDir + "\n");
        }

        // open and process the source directory
        File d = new java.io.File(sourceDir);

        if (d == null || !d.isDirectory())
        {
            throw new Exception("Error, cannot open source directory " + sourceDir);
        }

        String[] dircontents = d.list(directoryFilter);
        
        Arrays.sort(dircontents);

        for (int i = 0; i < dircontents.length; i++)
        {
            String dirContent = dircontents[i];
			if (skipItems.containsKey(dirContent))
            {
                log.info("Skipping import of " + dirContent);
            }
            else
            {
            	try
            	{
            		addItem(c, mycollections, sourceDir, dirContent, mapOut, template);
            		log.info(i + " " + dirContent);
            		c.clearCache();
            	}
            	catch(Exception e)
            	{
            		log.error(e.getMessage(), e);
            		if(errorOut == null)
            		{
            			errorOut = new PrintWriter(new FileWriter(new File(errorFile), false));
            		}
            		
            		errorOut.append(sourceDir + File.separator + dirContent + "@" + e.getMessage() + "\n");
            	}
            }
        }
        
        if(mapOut != null)
        {
        	mapOut.flush();
        	mapOut.close();
        }
        
        if(errorOut != null)
        {
        	errorOut.flush();
        	errorOut.close();
        }
    }

    /**
     * item? try and add it to the archive.
     * @param mycollections - add item to these Collections.
     * @param path - directory containing the item directories.
     * @param itemname handle - non-null means we have a pre-defined handle already 
     * @param mapOut - mapfile we're writing
     * @throws IOException 
     * @throws SQLException 
     * @throws AuthorizeException 
     * @throws TransformerException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws WorkflowException 
     * @throws MessagingException 
     * @throws WorkflowConfigurationException 
     * @throws FolderMetadataException 
     */
    private Item addItem(Context c, Collection[] mycollections, String path,
            String itemname, PrintWriter mapOut, boolean template) throws AuthorizeException, SQLException, IOException, ParserConfigurationException, SAXException, TransformerException, WorkflowConfigurationException, MessagingException, WorkflowException, FolderMetadataException 
    {
        String mapOutput = null;

        log.info("Adding item from directory " + itemname);

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

                // send ID to the mapfile
                mapOutput = itemname + " " + myitem.getID();
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
                InstallItem.installItem(c, wi, myhandle);

                // find the handle, and output to map file
                myhandle = HandleManager.findHandle(c, myitem);

                mapOutput = itemname + " " + myhandle;
            }

            // set permissions if specified in contents file
            if (options.size() > 0)
            {
                log.info("Processing options");
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
            mapOut.println(mapOutput);
        }

        c.commit();

        return myitem;
    }

    ////////////////////////////////////
    // utility methods
    ////////////////////////////////////
    // read in the map file and generate a hashmap of (file,handle) pairs
    private Map<String, String> readMapFile(String filename) throws Exception
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
            log.info("\tLoading dublin core from " + filename);
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
            log.info("\tSchema: " + schema + " Element: " + element + " Qualifier: " + qualifier
                    + " Value: " + value);
        }

        if ("none".equals(qualifier) || "".equals(qualifier))
        {
            qualifier = null;
        }

        if (!isTest)
        {
            i.addMetadata(schema, element, qualifier, language != null && language.isEmpty() ? null : language, value);
        }
        else
        {
            // If we're just test the import, let's check that the actual metadata field exists.
        	MetadataSchema foundSchema = MetadataSchema.find(c,schema);
        	
        	if (foundSchema == null)
        	{
        		log.info("ERROR: schema '"+schema+"' was not found in the registry.");
        		return;
        	}
        	
        	int schemaID = foundSchema.getSchemaID();
        	MetadataField foundField = MetadataField.findByElement(c, schemaID, element, qualifier);
        	
        	if (foundField == null)
        	{
        		log.info("ERROR: Metadata field: '"+schema+"."+element+"."+qualifier+"' was not found in the registry.");
        		return;
            }		
        }
    }

    /**
     * Read in the handle file or return null if empty or doesn't exist
     */
    private String processHandleFile(Context c, Item i, String path, String filename)
    {
        File file = new File(path + File.separatorChar + filename);
        String result = null;

        log.info("Processing handle file: " + filename);
        if (file.exists())
        {
            BufferedReader is = null;
            try
            {
                is = new BufferedReader(new FileReader(file));

                // result gets contents of file, or null
                result = is.readLine();

                log.info("read handle: '" + result + "'");

            }
            catch (FileNotFoundException e)
            {
                // probably no handle file, just return null
                log.info("It appears there is no handle file -- generating one");
            }
            catch (IOException e)
            {
                // probably no handle file, just return null
                log.info("It appears there is no handle file -- generating one");
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
                    	log.error(e1.getMessage(), e1);
                    }
                }
            }
        }
        else
        {
            // probably no handle file, just return null
            log.info("It appears there is no handle file -- generating one");
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

        log.info("\tProcessing contents file: " + contentsFile);

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
                            log.error("\tERROR: invalid contents file line");
                            log.error("\t\tSkipping line: "
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
                        log.info("\tRegistering Bitstream: " + sFilePath
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
                        log.info("\tBitstream: " + line);
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
                            log.info("\tBitstream: " + bitstreamName +
                                               "\tBundle: " + bundleName +
                                               primaryStr);
                        }
                        else
                        {
                            processContentFileEntry(c, i, path, bitstreamName, null, primary);
                            log.info("\tBitstream: " + bitstreamName + primaryStr);
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

            log.info("No contents file found - but only metadata files found. Assuming metadata only.");
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
     * @throws FolderMetadataException 
     */
    private void processOptions(Context c, Item myItem, List<String> options)
            throws SQLException, AuthorizeException, FolderMetadataException
    {
        for (String line : options)
        {
            log.debug("\tprocessing " + line);

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
                    log.error("SQL Exception finding group name: "
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
                log.info("\tdefault permissions set for "
                        + bitstreamName);
            }
            else if (!isTest)
            {
                if (permissionsExist)
                {
                    if (myGroup == null)
                    {
                        log.info("\t" + groupName
                                + " not found, permissions set to default");
                    }
                    else if (actionID == -1)
                    {
                        throw new FolderMetadataException("jsp.dspace-admin.foldermetadataimport.errors.folderpermission");
                    }
                    else
                    {
                        log.info("\tSetting special permissions for "
                                + bitstreamName);
                        setPermission(c, myGroup, actionID, bs);
                    }
                }

                if (descriptionExists)
                {
                    log.info("\tSetting description for "
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
                log.info("\t\tpermissions: READ for " + g.getName());
            }
            else if (actionID == Constants.WRITE)
            {
                log.info("\t\tpermissions: WRITE for " + g.getName());
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
    public static String getStringValue(Node node)
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
    public static Document loadXML(String filename) throws IOException,
            ParserConfigurationException, SAXException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();

        return builder.parse(new File(filename));
    }


}
