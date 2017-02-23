/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.xpath.XPathAPI;

import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;


/**
 * 	Miscellaneous methods for metadata handling that build on the API
 *      which might have general utility outside of the specific use
 *      in context in ItemUpdate.
 *      
 *      The XML methods were based on those in ItemImport
 * 
 *
 */
public class MetadataUtilities {

    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**      
     * 
     *  Working around Item API to delete a value-specific Metadatum
  For a given element/qualifier/lang:
      get all DCValues
      clear (i.e. delete) all of these DCValues
     *      add them back, minus the one to actually delete
     *  
     * @param context DSpace Context
     * @param item Item Object
     * @param dtom metadata field
     * @param isLanguageStrict whether strict or not
     * @throws SQLException if database error
     * @return true if metadata field is found with matching value and was deleted
     */
    public static boolean deleteMetadataByValue(Context context, Item item, DtoMetadata dtom, boolean isLanguageStrict) throws SQLException {
    	List<MetadataValue> ar = null;
    	
    	if (isLanguageStrict)
    	{   // get all for given type
    		ar = itemService.getMetadata(item, dtom.schema, dtom.element, dtom.qualifier, dtom.language);
    	}
    	else
    	{
    		ar = itemService.getMetadata(item, dtom.schema, dtom.element, dtom.qualifier, Item.ANY);
    	}
    	
    	boolean found = false;
    	
    	//build new set minus the one to delete
    	List<String> vals = new ArrayList<String>();
    	for (MetadataValue dcv : ar)
    	{
    		if (dcv.getValue().equals(dtom.value))
    		{
    			found = true;
    		}
    		else
    		{
    			vals.add(dcv.getValue());
    		}
    	}
    	
    	if (found)  //remove all for given type  ??synchronize this block??
    	{   
        	if (isLanguageStrict)
        	{           		
                itemService.clearMetadata(context, item, dtom.schema, dtom.element, dtom.qualifier, dtom.language);
        	}
        	else
        	{
                itemService.clearMetadata(context, item, dtom.schema, dtom.element, dtom.qualifier, Item.ANY);
        	}
    	
            itemService.addMetadata(context, item, dtom.schema, dtom.element, dtom.qualifier, dtom.language, vals);
    	}
		return found;
    }

    /**
     *   Append text to value metadata field to item
     *   
     * @param context DSpace Context
     * @param item DSpace Item
     * @param dtom metadata field
     * @param isLanguageStrict if strict
     * @param textToAppend text to append
     * @throws IllegalArgumentException  - When target metadata field is not found
     * @throws SQLException if database error
     */
    public static void appendMetadata(Context context, Item item, DtoMetadata dtom, boolean isLanguageStrict,
    		String textToAppend)
            throws IllegalArgumentException, SQLException {
    	List<MetadataValue> ar = null;
    	
    	// get all values for given element/qualifier
    	if (isLanguageStrict)  // get all for given element/qualifier
    	{   
    		ar = itemService.getMetadata(item, dtom.schema, dtom.element, dtom.qualifier, dtom.language);
    	}
    	else
    	{
    		ar = itemService.getMetadata(item, dtom.schema, dtom.element, dtom.qualifier, Item.ANY);
    	}
    	
    	if (ar.size() == 0)
    	{
    		throw new IllegalArgumentException("Metadata to append to not found");
    	}
    	
    	int idx = 0;  //index of field to change
    	if (ar.size() > 1)  //need to pick one, can't be sure it's the last one
    	{
    		// TODO maybe get highest id ?
    	}
    	
    	//build new set minus the one to delete
    	List<String> vals = new ArrayList<String>();
    	for (int i=0; i < ar.size(); i++)
    	{
    		if (i == idx)
    		{
    			vals.add(ar.get(i).getValue() + textToAppend);
    		}
    		else
    		{
    			vals.add(ar.get(i).getValue());
    		}
    	}

    	if (isLanguageStrict)
    	{           		
            itemService.clearMetadata(context, item, dtom.schema, dtom.element, dtom.qualifier, dtom.language);
    	}
    	else
    	{
            itemService.clearMetadata(context, item, dtom.schema, dtom.element, dtom.qualifier, Item.ANY);
    	}
	
        itemService.addMetadata(context, item, dtom.schema, dtom.element, dtom.qualifier, dtom.language, vals);
    }
 
    /**
     *  Modification of method from ItemImporter.loadDublinCore 
     *  as a Factory method
     * 
     * @param docBuilder  DocumentBuilder
     * @param is - InputStream of dublin_core.xml
     * @return list of DtoMetadata representing the metadata fields relating to an Item
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws ParserConfigurationException if parser config error
     * @throws SAXException if XML error
     * @throws TransformerException if transformer error
     * @throws AuthorizeException if authorization error
     */
    public static List<DtoMetadata> loadDublinCore(DocumentBuilder docBuilder, InputStream is)
    throws SQLException, IOException, ParserConfigurationException,
           SAXException, TransformerException, AuthorizeException
	{    	
		Document document = docBuilder.parse(is);
		
		List<DtoMetadata> dtomList = new ArrayList<DtoMetadata>();
		
		// Get the schema, for backward compatibility we will default to the
		// dublin core schema if the schema name is not available in the import file		
		String schema = null;
		NodeList metadata = XPathAPI.selectNodeList(document, "/dublin_core");		
		Node schemaAttr = metadata.item(0).getAttributes().getNamedItem("schema");
		if (schemaAttr == null)
		{
		    schema = MetadataSchema.DC_SCHEMA;
		}
		else
		{
		    schema = schemaAttr.getNodeValue();
		}

		// Get the nodes corresponding to formats
		NodeList dcNodes = XPathAPI.selectNodeList(document, "/dublin_core/dcvalue");
		
		for (int i = 0; i < dcNodes.getLength(); i++)
		{
		    Node n = dcNodes.item(i);		    
	        String value = getStringValue(n).trim(); 
	        // compensate for empty value getting read as "null", which won't display
	        if (value == null)
	        {
	            value = "";
	        }
	        String element = getAttributeValue(n, "element");
	        if (element != null)
	        {
	        	element = element.trim();
	        }
	        String qualifier = getAttributeValue(n, "qualifier"); 
	        if (qualifier != null)
	        {
	        	qualifier = qualifier.trim();
	        }
	        String language = getAttributeValue(n, "language");
	        if (language != null)
	        {
	        	language = language.trim();
	        }

	        if ("none".equals(qualifier) || "".equals(qualifier))
	        {
	            qualifier = null;
	        }
	        
	        // a goofy default, but consistent with DSpace treatment elsewhere  
	        if (language == null)
	        {
	            language = "en";
	        }
	        else if ("".equals(language))
	        {
	            language = ConfigurationManager.getProperty("default.language");
	        }
	        
		    DtoMetadata dtom = DtoMetadata.create(schema, element, qualifier, language, value);
		    ItemUpdate.pr(dtom.toString());
		    dtomList.add(dtom);
		}
		return dtomList;
	}

    /**
     *    Write dublin_core.xml 
     * 
     * @param docBuilder DocumentBuilder
     * @param dtomList List of metadata fields
     * @return xml document
     * @throws ParserConfigurationException if parser config error
     * @throws TransformerConfigurationException if transformer config error
     * @throws TransformerException if transformer error
     */
	public static Document writeDublinCore(DocumentBuilder docBuilder, List<DtoMetadata> dtomList)
	throws ParserConfigurationException, TransformerConfigurationException, TransformerException
	{		
        Document doc = docBuilder.newDocument();
        Element root = doc.createElement("dublin_core");
        doc.appendChild(root);
    
        for (DtoMetadata dtom : dtomList)
        {
        	Element mel = doc.createElement("dcvalue");
        	mel.setAttribute("element", dtom.element);
        	if (dtom.qualifier == null)
        	{
        		mel.setAttribute("qualifier", "none");
        	}
        	else
        	{
        		mel.setAttribute("qualifier", dtom.qualifier);
        	}
 
        	if (StringUtils.isEmpty(dtom.language))
        	{
        		mel.setAttribute("language", "en");
        	}
        	else
        	{
        		mel.setAttribute("language", dtom.language);
        	}
        	mel.setTextContent(dtom.value);
        	root.appendChild(mel);
        }
        
        return doc;       
	}
	
    /**
     *   write xml document to output stream
     * @param doc XML Document
     * @param transformer XML Transformer
     * @param out OutputStream
     * @throws IOException if IO Error
     * @throws TransformerException if Transformer error
     */
	public static void writeDocument(Document doc, Transformer transformer, OutputStream out)
	throws IOException, TransformerException
	{
        Source src = new DOMSource(doc); 
        Result dest = new StreamResult(out); 
        transformer.transform(src, dest); 
	}
    
    
    
    // XML utility methods
    /**
     * Lookup an attribute from a DOM node.
     * @param n Node
     * @param name name
     * @return attribute value
     */
    private static String getAttributeValue(Node n, String name)
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
    private static String getStringValue(Node node)
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
     * Rewrite of ItemImport's functionality
     * but just the parsing of the file, not the processing of its elements.
     *      
     * @param f file
     * @return list of ContentsEntry
     * @throws FileNotFoundException if file doesn't exist
     * @throws IOException if IO error
     * @throws ParseException if parse error
     */
    public static List<ContentsEntry> readContentsFile(File f)
    throws FileNotFoundException, IOException, ParseException
    {
    	List<ContentsEntry> list = new ArrayList<ContentsEntry>();
    	
    	BufferedReader in = null;
    	
    	try
    	{
	    	in = new BufferedReader(new FileReader(f));
	    	String line = null;
	    	
	    	while ((line = in.readLine()) != null)
	    	{
	    		line = line.trim();
	            if ("".equals(line))
	            {
	                continue;
	            }
	            ItemUpdate.pr("Contents entry: " + line);	            
	    		list.add(ContentsEntry.parse(line));	 
	    	}
    	}
    	finally
    	{
    		try
    		{
    			in.close();
    		}
    		catch(IOException e)
    		{
    			//skip
    		}
    	}
    	
    	return list;
    }

    /**
     * 
     * @param f file
     * @return list of lines as strings
     * @throws FileNotFoundException if file doesn't exist
     * @throws IOException if IO Error
     */
    public static List<String> readDeleteContentsFile(File f)
    throws FileNotFoundException, IOException
    {
    	List<String> list = new ArrayList<>();
    	
    	BufferedReader in = null;
    	
    	try
    	{
	    	in = new BufferedReader(new FileReader(f));
	    	String line = null;
	    	
	    	while ((line = in.readLine()) != null)
	    	{
	    		line = line.trim();
	            if ("".equals(line))
	            {
	                continue;
	            }
	            
                list.add(line);
	    	}
    	}
    	finally
    	{
    		try
    		{
    			in.close();
    		}
    		catch(IOException e)
    		{
    			//skip
    		}
    	}
    	
    	return list;
    }

    /**
     *    Get display of Metadatum    
	 *
     * @param dcv MetadataValue
     * @return string displaying elements of the Metadatum
     */
    public static String getDCValueString(MetadataValue dcv)
    {
        MetadataField metadataField = dcv.getMetadataField();
        MetadataSchema metadataSchema = metadataField.getMetadataSchema();
        return "schema: " + metadataSchema.getName() + "; element: " + metadataField.getElement() + "; qualifier: " + metadataField.getQualifier() +
    	       "; language: " + dcv.getLanguage() + "; value: " + dcv.getValue();
    }

	/**
         * Return compound form of a metadata field (i.e. schema.element.qualifier)
	 * @param schema schema
         * @param element element
         * @param qualifier qualifier
	 * @return a String representation of the two- or three-part form of a metadata element
	 *         e.g. dc.identifier.uri
	 */
	public static  String getCompoundForm(String schema, String element, String qualifier)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(schema).append(".").append(element);
		
		if (qualifier != null)
		{
			sb.append(".").append(qualifier);
		}
		return sb.toString();
	}
	
	/**
	 *    Parses metadata field given in the form {@code <schema>.<element>[.<qualifier>|.*]}
	 *    checks for correct number of elements (2 or 3) and for empty strings
	 *    
         *    @param compoundForm compound form of metadata field
	 *    @return String Array
	 *    @throws ParseException if validity checks fail
	 *    
	 */
	public static String[] parseCompoundForm(String compoundForm)
	throws ParseException
	{
		String[] ar = compoundForm.split("\\s*\\.\\s*");  //trim ends
				
		if ("".equals(ar[0]))
		{
			throw new ParseException("schema is empty string: " + compoundForm, 0);
		}
		
		if ((ar.length < 2) || (ar.length > 3) || "".equals(ar[1]))
		{
			throw new ParseException("element is malformed or empty string: " + compoundForm, 0);
		}
		
		return ar;
	}
	
} 
