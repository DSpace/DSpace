/*
 * ItemLoader.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
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

package org.dspace.app.itemimport;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;
import org.apache.xpath.XPathAPI;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.InstallItem;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;


/**
* The Item importer does exactly that - imports items into
*  the repository.
*/

public class ItemImport
{
    public static void main(String argv[])
    {
        String usage = "Usage: ItemImport EPersonID collectionID directoryname";
        Context c = null;

        ItemImport myloader = new ItemImport();

        if( argv.length < 3 )
        {
            System.out.println( usage );
            System.exit( 1 );
        }

        try
        {
            // get a context, collectionID, dirname
            int collectionID = Integer.parseInt( argv[1] );
            int epersonID    = Integer.parseInt( argv[0] );

            c = new Context();

            EPerson ep = EPerson.find(c, epersonID);

            c.setCurrentUser( ep );
            c.setIgnoreAuthorization( true );

            Collection mycollection = Collection.find( c, collectionID );

            System.out.println("Importing directory " + argv[2]
             + " to collection " + collectionID );

            myloader.processDirectory( c, mycollection, argv[2] );

            // complete all transactions
            c.complete();
        }
        catch( Exception e )
        {
            // abort all operations
            c.abort();
            e.printStackTrace();
            System.out.println( e );
        }
    }

    
    private void processDirectory( Context c, Collection mycollection, String dirname )
    {
        System.out.println( "Processing directory: " + dirname );

        String [] dircontents = new java.io.File( dirname ).list();

        for( int i = 0; i < dircontents.length; i++ )
        {
            processItem( c, mycollection, dirname, dircontents[ i ] );
            System.out.println( i + " " + dircontents[ i ] );
        }
    }


    // item?  try and add it to the archive
    private void processItem( Context c, Collection mycollection, String path, String itemname )
    {
        Item myitem = null;

        try
        {
            // create workspace item
            WorkspaceItem wi = WorkspaceItem.create(c, mycollection, false);

            myitem = wi.getItem();

            // now fill out dublin core for item
            loadDublinCore( c, myitem, path + "/" + itemname + "/" + "dspace_meta.xml" );

            // and the bitstreams from the contents file
            // process contents file, add bistreams and bundles
            processContentsFile( c, myitem, path + "/" + itemname, "contents" );

            // put item in system
            InstallItem.installItem(c, wi);
        }
        catch( Exception e )
        {
            e.printStackTrace();
            System.out.println( e );
        }
    }


    private void loadDublinCore(Context c, Item myitem, String filename)
        throws SQLException, IOException, ParserConfigurationException,
            SAXException, TransformerException //, AuthorizeException
    {
        Document document = loadXML(filename);

        // Get the nodes corresponding to formats
        NodeList dcNodes = XPathAPI.selectNodeList(document,
            "/barton_record/dcvalue");

        System.out.println("Nodelist has # elements: " + dcNodes.getLength() );

        // Add each one as a new format to the registry
        for (int i=0; i < dcNodes.getLength(); i++)
        {
            Node n = dcNodes.item(i);
            addDCValue(myitem, n);
        }
    }


    private void addDCValue(Item i, Node n)
        throws TransformerException
    {
        String value     = getStringValue(n); //n.getNodeValue(); //getElementData(n, "element");
        String element   = getAttributeValue(n, "element");
        String qualifier = getAttributeValue(n, "qualifier"); //NodeValue(); //getElementData(n, "qualifier");

        System.out.println("Element: " + element + " Qualifier: " + qualifier +
        " Value: " + value );

        if( qualifier.equals("none") ) qualifier = null;

        i.addDC(element, qualifier, "en", value);
    }


    /**
    * Return the String value of a Node
    */
    public String getStringValue(Node node)
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
     * Given a contents file and an item, stuffing it with bitstreams from the
     *  contents file
     */
    private void processContentsFile( Context c, Item i, String path, String filename )
    {
        String contentspath = path + "/" + filename;
        String line = "";

        System.out.println( "Processing contents file: " + contentspath );

        try
        {
            BufferedReader is = new BufferedReader( new FileReader( contentspath ) );
            while( ( line = is.readLine() ) != null )
            {
                System.out.println( "Bitstream: " + line );
                processContentFileEntry( c, i, path, line );
            }
            is.close();
        }
        catch( Exception e )
        {
            e.printStackTrace();
            System.out.println( "Caught exception: " + e );
        }
    }


    // each entry represents a bitstream....
    public void processContentFileEntry( Context c, Item i, String path, String name)
        throws SQLException, IOException, AuthorizeException
    {
        String fullpath = path + "/" + name;

        // first, make sure we have an item
        //if (item == null)
        //  throw new DSpaceException("Item is null");

        // get an input stream
        BufferedInputStream bis = new BufferedInputStream( new FileInputStream( fullpath ) );

        // add it to the item in a bundle
        Bitstream bs = i.createSingleBitstream(bis);

        bs.setName( name );
        bs.update();
    }


    // XML utility methods
    public String getAttributeValue(Node n, String myattributename)
    {
        String myvalue = "";

        NamedNodeMap nm = n.getAttributes();

        for (int i = 0; i < nm.getLength(); i++ )
        {
            Node node = nm.item(i);
            String name  = node.getNodeName();
            String value = node.getNodeValue();

            if(myattributename.equals(name))
            {
                return value;
            }
        }

        return myvalue;
    }


    // XML utility methods stolen from administer.

    /**
     * Get the CDATA of a particular element.  For example, if the XML document
     * contains:
     * <P>
     * <code>
     * &lt;foo&gt;&lt;mimetype&gt;application/pdf&lt;/mimetype&gt;&lt;/foo&gt;
     * </code>
     * passing this the <code>foo</code> node and <code>mimetype</code> will
     * return <code>application/pdf</code>.</P>
     * Why this isn't a core part of the XML API I do not know...
     *
     * @param parentElement  the element, whose child element you want
     *                       the CDATA from
     * @param childName      the name of the element you want the CDATA from
     *
     * @return  the CDATA as a <code>String</code>
     */
    private String getElementData(Node parentElement, String childName)
        throws TransformerException
    {
        // Grab the child node
        Node childNode = XPathAPI.selectSingleNode(parentElement,
            childName);

        if (childNode == null)
        {
            // No child node, so no values
            return null;
        }

        // Get the #text
        Node dataNode = childNode.getFirstChild();

        if (dataNode==null)
        {
            return null;
        }

        // Get the data
        String value = dataNode.getNodeValue().trim();

        return value;
    }


    /**
     * Load in the XML from file.
     *
     * @param filename  the filename to load from
     *
     * @return  the DOM representation of the XML file
     */
    private static Document loadXML(String filename)
        throws IOException, ParserConfigurationException, SAXException
    {
        DocumentBuilder builder =
            DocumentBuilderFactory.newInstance().newDocumentBuilder();

        return builder.parse(new File(filename));
    }
}


/*
issues

javadocs - even though it's not an API
allow re-importing
begin export module thoughts
rename to ItemLoader
if no epersonID found, check for username?
list of collections would be nice too

*/
