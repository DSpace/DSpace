/*
 * ItemArchive.java
 *
 * Version: $Revision: 3984 $
 *
 * Date: $Date: 2009-06-29 22:33:25 -0400 (Mon, 29 Jun 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.itemupdate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;

import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.content.ItemIterator;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import org.w3c.dom.Document;


/**
 *   Encapsulates the Item in the context of the DSpace Archive Format
 *
 */
public class ItemArchive {

	static public final String DUBLIN_CORE_XML = "dublin_core.xml"; 
	
    private static DocumentBuilder builder = null;
    private static Transformer transformer = null;
   
	private List<DtoMetadata> dtomList = null; 
	private List<DtoMetadata> undoDtomList = new ArrayList<DtoMetadata>();
	
	private List<Integer> undoAddContents = new ArrayList<Integer>(); // for undo of add
	
	private Item item;
	private File dir;  // directory name in source archive for this item
	private String dirname; //convenience
	
//constructors
	private ItemArchive() 
	{
		// nothing
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
 * 
 */
	public static ItemArchive create(Context context, File dir, String itemField)
	throws Exception
	{
		ItemArchive itarch = new ItemArchive(); 
		itarch.dir = dir;
		itarch.dirname = dir.getName();
		InputStream is = new FileInputStream(new File(dir, DUBLIN_CORE_XML));
		itarch.dtomList = MetadataUtilities.loadDublinCore(getDocumentBuilder(), is);  
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
		
	private static DocumentBuilder getDocumentBuilder()
	throws ParserConfigurationException
	{
		if (builder == null)
		{
		    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		return builder;
	}
	
	private static Transformer getTransformer()
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
	 * @param dtom
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
	 * @param bitstreamId
	 */
	public void addUndoDeleteContents(int bitstreamId)
	{
		this.undoAddContents.add(bitstreamId);
	}
	
	
    /**
	 *   Obtain item from DSpace based on handle
     *   This is the default implementation
     *   that uses the dc.identifier.uri metadatafield 
     *   that contains the item handle as its value  
     *   
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
    		
		DSpaceObject dso = HandleManager.resolveToObject(context, handle);  
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
     * @throws SQLException
     * @throws Exception
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
    	
	    ItemIterator itr = Item.findByMetadataField(context, dtom.schema, dtom.element, dtom.qualifier, dtom.value);
		int count = 0;
		while (itr.hasNext())
		{
			item = itr.next();
			count++;
		}
		
		itr.close(); 
		
		ItemUpdate.prv("items matching = " + count );
		
		if (count != 1)
		{
			throw new Exception ("" + count + " items matching item identifier: " + dtom.value);
		}
        
    	return item;    	
    }  
    
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
     * 
     * @param undoDir - the root directory of the undo archive
     */
	public void writeUndo(File undoDir)
	throws IOException, ParserConfigurationException, TransformerConfigurationException, 
	       TransformerException, FileNotFoundException
	{
		// create directory for item
		File dir = new File(undoDir, dirname);
		dir.mkdir();
		
		OutputStream out = new FileOutputStream(new File(dir, "dublin_core.xml"));
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
	        	for (Integer i : undoAddContents)
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
	
} //end class
