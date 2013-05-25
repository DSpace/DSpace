/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sfx;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import org.dspace.content.DCPersonName;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Constants;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * XML configuration file reader for DSpace metadata fields (DC) mapping
 * to OpenURL parameters.
 * <p>
 * This class reads the [dspace]/config/sfx.xml configuration file, which
 * contains pairs of DSpace item metadata values and OpenURL parameter names.
 * Then it takes an item and constructs an OpenURL for it with values of
 * parameters filled in from the paired metadata fields.
 * </p>
 *
 * @author Stuart Lewis
 * @author Graham Triggs
 * @version $Revision$
 */

public class SFXFileReader {

    /** The SFX configuration file */
    private static Document doc;

    /** log4j logger */
    private static final Logger log = Logger.getLogger(SFXFileReader.class);


    /**
     * Loads the SFX configuraiton file
     *
     * @param fileName The name of the SFX configuration file
     * @param item The item to process, from which metadata values will be taken
     *
     * @return the SFX string
     * @throws IOException
     */
    public static String loadSFXFile(String fileName, Item item) throws IOException
    {
        // Parse XML file -> XML document will be build
        if (doc == null)
        {
            doc = parseFile(fileName);
        }

        // Return final sfx Query String
        return  doNodes(doc, item);
    }

   /** Parses XML file and returns XML document.
    * @param fileName XML file to parse
    * @return XML document or <B>null</B> if error occured. The error is caught and logged.
    */

   public static Document parseFile(String fileName) {
	log.info("Parsing XML file... " + fileName);
       DocumentBuilder docBuilder;
       Document doc = null;
       DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
       docBuilderFactory.setIgnoringElementContentWhitespace(true);
       try {
           docBuilder = docBuilderFactory.newDocumentBuilder();
       }
       catch (ParserConfigurationException e) {
	    log.error("Wrong parser configuration: " + e.getMessage());
           return null;
       }
       File sourceFile = new File(fileName);
       try {
           doc = docBuilder.parse(sourceFile);
       }
       catch (SAXException e) {
           log.error("Wrong XML file structure: " + e.getMessage());
           return null;
       }
       catch (IOException e) {
           log.error("Could not read source file: " + e.getMessage());
       }
       log.info("XML file parsed");
       return doc;
   }

    /**
     * Process the item, mapping each of its metadata fields defined in the
     * configuration file to an OpenURL parameter
     *
     * @param node DOM node of the mapping pair in the XML file (field element)
     * @param item The item to process, from which metadata values will be taken
     * @return processed fields.
     * @throws IOException
     */
   public static String doNodes(Node node, Item item) throws IOException
   {
        if (node == null)
        {
	    log.error (" Empty Node ");
            return null;
        }
        Node e = getElement(node);
        NodeList nl = e.getChildNodes();
        int len = nl.getLength();
        String sfxfield = "";
        int i = 0;

        while ((i < len) && StringUtils.isEmpty(sfxfield))
        {
            Node nd = nl.item(i);
            if ((nd == null) || isEmptyTextNode(nd))
            {
                i++;
                continue;
            }
            String tagName = nd.getNodeName();
            if (tagName.equals("query-pairs"))
            {
                sfxfield = processFields(nd, item);

            }
            i++;
        }
           log.info("Process fields : " + sfxfield);
        return sfxfield;
   }

    /**
     * Process the field nodes, mapping each metadata field defined in the
     * configuration file to an OpenURL parameter
     *
     * @param e DOM node of the mapping pair in the XML file (field element)
     * @param item The item to process, from which metadata values will be taken
     * @return assembled OpenURL query.
     * @throws IOException
     */
   private static String processFields(Node e, Item item) throws IOException
   {
        NodeList cl = e.getChildNodes();
        int lench = cl.getLength();
        String  myquery = "";

        for (int j = 0; j < lench; j++)
        {
            Node nch = cl.item(j);
            String querystring = "";
            String schema = "";
            String qualifier = "";
            String element = "";

            if (nch.getNodeName().equals("field"))
            {
                NodeList pl = nch.getChildNodes();
                int plen = pl.getLength();
                int finish = 0;
                for (int k = 0; k < plen; k++)
                {
                    Node vn= pl.item(k);
                    String vName = vn.getNodeName();
                    if (vName.equals("querystring"))
                    {
                        querystring = getValue(vn);
                        finish ++;
                    }
                    else if (vName.equals("dc-schema"))
                    {
                        schema = getValue(vn);
                        finish ++;
                        }
                        else if (vName.equals("dc-element"))
                        {
                            element = getValue(vn);
                            finish ++;
                        }
                        else if (vName.equals("dc-qualifier"))
                        {
                            qualifier = getValue(vn);
                            finish ++;
                            if (StringUtils.isEmpty(qualifier))
                            {
                                qualifier = null;
                            }
                        }
                        if (finish == 4)
                        {
                        DCValue[] dcvalue = item.getMetadata(schema, element, qualifier, Item.ANY);
                        if (dcvalue.length > 0)
                            {
                            // Issued Date
                                if (element.equals("date") && qualifier.equals("issued"))
                            {
                            String fullDate = dcvalue[0].value;
                            // Remove the time if there is one - day is greatest granularity for SFX
                                if (fullDate.length() > 10)
                                {
                                     fullDate = fullDate.substring(0, 10);
                                }
                                if (myquery.equals(""))
                                { myquery = querystring + URLEncoder.encode(fullDate, Constants.DEFAULT_ENCODING); }
                                else
                                { myquery =  myquery + "&" + querystring + URLEncoder.encode(fullDate, Constants.DEFAULT_ENCODING); }
                            }
                            else
                            {
                                // Contributor Author
                                if (element.equals("contributor") && qualifier.equals("author"))
                                    {
                                    DCPersonName dpn = new DCPersonName(dcvalue[0].value);
                                        String dpnName = dcvalue[0].value;

                                        if (querystring.endsWith("aulast="))  { dpnName = dpn.getLastName(); }
                                        else { if (querystring.endsWith("aufirst=")) { dpnName = dpn.getFirstNames(); }}

                                    if (myquery.equals(""))
                                    { myquery = querystring + URLEncoder.encode(dpnName, Constants.DEFAULT_ENCODING); }
                                            else
                                    { myquery =  myquery + "&" + querystring + URLEncoder.encode(dpnName, Constants.DEFAULT_ENCODING); }
                                }
                                else
                                {
                                    if (myquery.equals(""))
                                    { myquery =  querystring + URLEncoder.encode(dcvalue[0].value, Constants.DEFAULT_ENCODING);}
                                    else
                                        { myquery =  myquery + "&" + querystring + URLEncoder.encode(dcvalue[0].value, Constants.DEFAULT_ENCODING);}
                                    }
                                }
                            } // if dc.length > 0

                    finish = 0;
                        querystring = "";
                    schema = "";
                        element = "";
                    qualifier = "";
                    } // if finish == 4
                } //for k
            } // if field
        } // for j
        return myquery;
   }

   /** Returns element node
    * @param node element (it is XML tag)
    * @return Element node otherwise null
    */
  public static Node getElement(Node node)
  {
       NodeList child = node.getChildNodes();
       int length = child.getLength();
       for (int i = 0; i < length; i++)
       {
   	    Node kid = child.item(i);
   	    if (kid.getNodeType() == Node.ELEMENT_NODE)
   	    {
   	    	return kid;
         }
       }
       return null;
  }

		/** Is Empty text Node **/
   public static boolean isEmptyTextNode(Node nd)
   {
   	boolean isEmpty = false;
   	if (nd.getNodeType() == Node.TEXT_NODE)
   	{
   		String text = nd.getNodeValue().trim();
   		if (text.length() == 0)
   		{
   			isEmpty = true;
   		}
   	}
   	return isEmpty;
   }

   /**
    * Returns the value of the node's attribute named <name>
   **/
   public static String getAttribute(Node e, String name)
   {
   	NamedNodeMap attrs = e.getAttributes();
   	int len = attrs.getLength();
   	if (len > 0)
   	{
   		for (int i = 0; i < len; i++)
   		{
   			Node attr = attrs.item(i);
   			if (name.equals(attr.getNodeName()))
   			{
   				return attr.getNodeValue().trim();
   			}
   		}
   	}
   	//no such attribute
   	return null;
   }

   /**
    * Returns the value found in the Text node (if any) in the
    * node list that's passed in.
   */
   public static String getValue(Node node)
   {
   	NodeList child = node.getChildNodes();
   	for (int i = 0; i < child.getLength(); i++)
   	{
   		Node kid = child.item(i);
   		short type = kid.getNodeType();
   		if (type == Node.TEXT_NODE)
   		{
		    return kid.getNodeValue().trim();
   		}
   	}
   	// Didn't find a text node
   	return null;
   }
}
