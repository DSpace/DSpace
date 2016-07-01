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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.log4j.Logger;
import org.dspace.app.util.LocalSchemaFilenameFilter;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.w3c.dom.Document;


/**
 *   Encapsulates the Item in the context of the DSpace Archive Format
 *
 */
public class ItemArchive {
    private static final Logger log = Logger.getLogger(ItemArchive.class); 

	public static final String DUBLIN_CORE_XML = "dublin_core.xml";

    protected static DocumentBuilder builder = null;
    protected Transformer transformer = null;

    protected List<DtoMetadata> dtomList = null;
    protected List<DtoMetadata> undoDtomList = new ArrayList<DtoMetadata>();

    protected List<UUID> undoAddContents = new ArrayList<>(); // for undo of add

    protected Item item;
    protected File dir;  // directory name in source archive for this item
    protected String dirname; //convenience

    protected HandleService handleService;
    protected ItemService itemService;

//constructors
    protected ItemArchive()
	{
        handleService = HandleServiceFactory.getInstance().getHandleService();
        itemService = ContentServiceFactory.getInstance().getItemService();
	}
	
/** factory method
 * 
 * 	Minimal requirements for dublin_core.xml for this application
 *  is the presence of dc.identifier.uri
 *  which must contain the handle for the item
 *  
 *  @param context - The DSpace context
 *  @param dir     - The directory File in the source archive
 *  @param itemField - The metadata field in which the Item identifier is located
 *                     if null, the default is the handle in the dc.identifier.uri field
 *  @return ItemArchive object
 *  @throws Exception if error
 * 
 */
	public static ItemArchive create(Context context, File dir, String itemField)
	throws Exception
	{
		ItemArchive itarch = new ItemArchive(); 
		itarch.dir = dir;
		itarch.dirname = dir.getName();
        InputStream is = null;
        try
        {
            is = new FileInputStream(new File(dir, DUBLIN_CORE_XML));
            itarch.dtomList = MetadataUtilities.loadDublinCore(getDocumentBuilder(), is);
            
            //The code to search for local schema files was copied from org.dspace.app.itemimport.ItemImportServiceImpl.java
            File file[] = dir.listFiles(new LocalSchemaFilenameFilter());
            for (int i = 0; i < file.length; i++)
            {
                is = new FileInputStream(file[i]);
                itarch.dtomList.addAll(MetadataUtilities.loadDublinCore(getDocumentBuilder(), is));
            }
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }
		ItemUpdate.pr("Loaded metadata with " + itarch.dtomList.size() + " fields");
		
		if (itemField == null)
		{
			itarch.item = itarch.itemFromHandleInput(context);  // sets the item instance var and seeds the undo list
		}
		else
		{
			itarch.item = itarch.itemFromMetadataField(context, itemField);  			
		}
		
		if (itarch.item == null)
		{
			throw new Exception("Item not instantiated: " + itarch.dirname);
		}
		
		ItemUpdate.prv("item instantiated: " + itarch.item.getHandle());

		return itarch;
	}
		
	protected static DocumentBuilder getDocumentBuilder()
	throws ParserConfigurationException
	{
		if (builder == null)
		{
		    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		return builder;
	}

    /**
     * Getter for Transformer
     * @return Transformer
     * @throws TransformerConfigurationException if config error
     */
    protected Transformer getTransformer()
	throws TransformerConfigurationException
	{
		if (transformer == null)
		{
		    transformer = TransformerFactory.newInstance().newTransformer();
		}
		return transformer;
	}

	/**
	 *   Getter for the DSpace item referenced in the archive
	 * @return DSpace item 
	 */
	public Item getItem()
	{
		return item;
	}
	
	/**
	 *   Getter for directory in archive on disk
	 * @return directory in archive
	 */
	public File getDirectory()
	{
		return dir;
	}
	
	/**
	 *   Getter for directory name in archive
	 * @return directory name in archive
	 */
	public String getDirectoryName()
	{
		return dirname;
	}
	
	/**
	 *   Add metadata field to undo list
	 * @param dtom DtoMetadata (represents metadata field)
	 */
	public void addUndoMetadataField(DtoMetadata dtom)
	{
		this.undoDtomList.add(dtom);
	}
	
	/**
	 *    Getter for list of metadata fields 
	 * @return list of metadata fields
	 */
	public List<DtoMetadata> getMetadataFields()
	{
		return dtomList;
	}
	
	/**
	 *   Add bitstream id to delete contents file
	 * @param bitstreamId bitstream ID
	 */
	public void addUndoDeleteContents(UUID bitstreamId)
	{
		this.undoAddContents.add(bitstreamId);
	}
	
	
    /**
	 *   Obtain item from DSpace based on handle
     *   This is the default implementation
     *   that uses the dc.identifier.uri metadatafield 
     *   that contains the item handle as its value  
     *   @param context DSpace Context
     *   @throws SQLException if database error
     *   @throws Exception if error
     */
    private Item itemFromHandleInput(Context context)
    throws SQLException, Exception
    {
    	DtoMetadata dtom = getMetadataField("dc.identifier.uri");
    	if (dtom == null)
    	{
    		throw new Exception("No dc.identier.uri field found for handle"); 
    	}
    	
    	this.addUndoMetadataField(dtom);  //seed the undo list with the uri
    	
		String uri = dtom.value;
		
    	if (!uri.startsWith(ItemUpdate.HANDLE_PREFIX))
    	{
    		throw new Exception("dc.identifier.uri for item " + uri 
    				+ " does not begin with prefix: " + ItemUpdate.HANDLE_PREFIX);
    	}
    	
    	String handle = uri.substring(ItemUpdate.HANDLE_PREFIX.length());
    		
		DSpaceObject dso = handleService.resolveToObject(context, handle);
		if (dso instanceof Item)
		{
			item =  (Item) dso;
		}
		else
		{
			ItemUpdate.pr("Warning: item not instantiated");
			throw new IllegalArgumentException("Item " + handle + " not instantiated.");
		}	
		return item;
    }
    
    /**
     * 	 Find and instantiate Item from the dublin_core.xml based
     *   on the specified itemField for the item identifier,
     *   
     * 
     * @param context   - the DSpace context
     * @param itemField - the compound form of the metadata element <schema>.<element>.<qualifier>
     * @throws SQLException if database error
     * @throws Exception if error
     */
    private Item itemFromMetadataField(Context context, String itemField)
    throws SQLException, AuthorizeException, Exception
    {
    	DtoMetadata dtom = getMetadataField(itemField);
    	
    	Item item = null;
    	
    	if (dtom == null)
    	{
    		throw new IllegalArgumentException("No field found for item identifier field: " + itemField);
    	}  
    	ItemUpdate.prv("Metadata field to match for item: " + dtom.toString());

    	this.addUndoMetadataField(dtom);  //seed the undo list with the identifier field
    	
	    Iterator<Item> itr = itemService.findByMetadataField(context, dtom.schema, dtom.element, dtom.qualifier, dtom.value);
		int count = 0;
		while (itr.hasNext())
		{
			item = itr.next();
			count++;
		}

		ItemUpdate.prv("items matching = " + count );
		
		if (count != 1)
		{
			throw new Exception ("" + count + " items matching item identifier: " + dtom.value);
		}
        
    	return item;    	
    }  
    /**
     * Get DtoMetadata field
     * @param compoundForm compound form
     * @return DtoMetadata field
     */
    private DtoMetadata  getMetadataField(String compoundForm)
    {
    	for (DtoMetadata dtom : dtomList)
    	{
			if (dtom.matches(compoundForm, false))
			{
				return dtom;
			}
    	}
    	return null;
    }    

    /**
     * write undo directory and files to Disk in archive format
     * 
     * @param undoDir - the root directory of the undo archive
     * @throws IOException if IO error
     * @throws ParserConfigurationException if config error
     * @throws TransformerConfigurationException if transformer config error
     * @throws TransformerException if transformer error
     * @throws FileNotFoundException if file not found
     */
	public void writeUndo(File undoDir)
	throws IOException, ParserConfigurationException, TransformerConfigurationException, 
	       TransformerException, FileNotFoundException
	{
		// create directory for item
		File dir = new File(undoDir, dirname);
		if (!dir.exists() && !dir.mkdir())
        {
            log.error("Unable to create undo directory");
        }
		
		OutputStream out = null;

        try
        {
            out = new FileOutputStream(new File(dir, "dublin_core.xml"));
            Document doc = MetadataUtilities.writeDublinCore(getDocumentBuilder(), undoDtomList);
            MetadataUtilities.writeDocument(doc, getTransformer(), out);

            // if undo has delete bitstream
            if (undoAddContents.size() > 0)
            {
                PrintWriter pw = null;
                try
                {
                    File f = new File(dir, ItemUpdate.DELETE_CONTENTS_FILE);
                    pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
                    for (UUID i : undoAddContents)
                    {
                        pw.println(i);
                    }
                }
                finally
                {
                    pw.close();
                }
            }
        }
        finally
        {
            if (out != null)
            {
                out.close();
            }
        }
	}
	
} //end class
