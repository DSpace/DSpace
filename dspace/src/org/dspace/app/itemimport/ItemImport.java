/*
 * ItemImport.java
 *
 * $Id$
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
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
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InstallItem;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/*
issues

javadocs - even though it's not an API
allow re-importing
list of collections to choose from would be nice too

*/


/**
* The Item importer does exactly that - imports items into
*  the repository.
*/

public class ItemImport
{        
    public static void main(String argv[])
        throws Exception
    {
        String usage = "Itemimport has three modes of operation:\n" +
                       "  add     = import items from directories within items_dir, and create mapfile\n" +
                       "  replace = use mapfile from previously imported items and replace\n" +
                       "  remove  = use mapfile from previously imported items and delete them\n" +
                       "\n" +
                       "ItemImport add EPersonID collectionID items_dir mapfile\n" +
                       "ItemImport replace EPersonID collectionID items_dir mapfile\n" +
                       "ItemImport remove EPersonID mapfile\n\n" +
                       "see DSpace documentation for format of item directories\n";

        Context c = null;

        ItemImport myloader = new ItemImport();

        int collectionID = -1;
        int epersonID    = -1;
        String sourceDir = null;
        String mapFile   = null;
        Collection mycollection = null;
        
        if( argv.length < 3 )
        {
            System.out.println( usage );
            System.exit( 1 );
        }

        // now get the args
        if( argv[0].equals( "remove" ) && (argv.length == 3) )
        {
            epersonID    = Integer.parseInt( argv[1] );
            mapFile      = argv[2];
        }
        else if( argv[0].equals( "add" ) && (argv.length == 5) )
        {
            epersonID    = Integer.parseInt( argv[1] );
            collectionID = Integer.parseInt( argv[2] );
            sourceDir    = argv[3];
            mapFile      = argv[4];
        }
        else if( argv[0].equals( "replace" ) && (argv.length == 5) )
        {
            epersonID    = Integer.parseInt( argv[1] );
            collectionID = Integer.parseInt( argv[2] );
            sourceDir    = argv[3];
            mapFile      = argv[4];
        }
        else
        {
            System.out.println( usage );
            System.exit( 1 );
        }

        try
        {
            c = new Context();

            if( epersonID != -1 )
            {
                EPerson ep = EPerson.find(c, epersonID);
                c.setCurrentUser( ep );
            }

            c.setIgnoreAuthorization( true );

            
            if( argv[0].equals( "add" ) )
            {
                mycollection = Collection.find( c, collectionID );
                myloader.addItems( c, mycollection, sourceDir, mapFile );
            }
            else if( argv[0].equals( "replace" ) )
            {
                mycollection = Collection.find( c, collectionID );
                myloader.replaceItems( c, mycollection, sourceDir, mapFile );
            }
            else if( argv[0].equals( "remove" ) )
            {
                myloader.removeItems( c, mycollection, mapFile );
            }

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

    
    private void addItems( Context c, Collection mycollection, String sourceDir, String mapFile )
        throws Exception
    {
        System.out.println( "Adding items from directory: " + sourceDir );
        System.out.println( "Generating mapfile: "          + mapFile   );
        
        // create the mapfile
        File outFile = new File( mapFile );
        PrintWriter mapOut = new PrintWriter( new FileWriter( outFile ) );
        
        // now process the source directory
        String [] dircontents = new java.io.File( sourceDir ).list();

        for( int i = 0; i < dircontents.length; i++ )
        {
            addItem( c, mycollection, sourceDir, dircontents[ i ], null, mapOut );
            System.out.println( i + " " + dircontents[ i ] );
        }
        
        mapOut.close();
    }


    private void replaceItems( Context c, Collection mycollection, String sourceDir, String mapFile )
        throws Exception
    {
        // read in HashMap first, to get list of handles & source dirs
        HashMap myhash = readMapFile( mapFile );
        
        // for each handle, re-import the item, discard the new handle
        // and re-assign the old handle
        Iterator i = myhash.keySet().iterator();
        ArrayList itemsToDelete = new ArrayList();
        
        while( i.hasNext() )
        {
            // get the old handle
            String newItemName = (String)i.next();
            String oldHandle   = (String)myhash.get(newItemName);

            System.out.println("\tReplacing:  " + oldHandle);

            // add new item, locate old one
            Item oldItem = (Item)HandleManager.resolveToObject(c, oldHandle);
            Item newItem = addItem(c, mycollection, sourceDir, newItemName, oldHandle, null);

/*   obsolete - but undeleted just to be safe....
            String newHandle = HandleManager.findHandle(c, newItem);

            // discard the new handle - FIXME: database hack
            String myquery =
                "DELETE FROM handle WHERE resource_type_id=" +
                Constants.ITEM + " AND resource_id=" + newItem.getID();
            DatabaseManager.updateQuery(c, myquery );

            // re-assign the old handle to the new item
            myquery = "UPDATE handle set resource_id=" +
                        newItem.getID() +
                        " WHERE handle.handle LIKE '" + oldHandle + "'";
            DatabaseManager.updateQuery(c, myquery );
*/
            // schedule item for demolition
            itemsToDelete.add( oldItem );
              
        }
                
        // now run through again, deleting items (do this last to avoid disasters!)
        // (this way deletes only happen if there have been no errors previously) 
        i = itemsToDelete.iterator();
        
        while( i.hasNext() )
        {
            removeItem(c, (Item)i.next());
        }
    }


    private void removeItems( Context c, Collection mycollection, String mapFile )
        throws Exception
    {
        System.out.println( "Deleting items listed in mapfile: " + mapFile );
        
        // read in the mapfile
        HashMap myhash = readMapFile( mapFile );

        // now delete everything that appeared in the mapFile
        Iterator i = myhash.keySet().iterator();
        
        while( i.hasNext() )
        {
            String myhandle = (String)myhash.get( i.next() );
            System.out.println("Deleting item " + myhandle);
            removeItem(c, myhandle);
        }
    }



    /** item?  try and add it to the archive
     *   c
     *   mycollection
     *   path
     *   itemname 
     *   handle - non-null means we have a pre-defined handle already
     *   mapOut - mapfile we're writing
     */   
    private Item addItem( Context c, Collection mycollection, String path, String itemname, String handle, PrintWriter mapOut )
        throws Exception
    {
        Item myitem = null;

        System.out.println("Adding item from directory " + itemname );

        // create workspace item
        WorkspaceItem wi = WorkspaceItem.create(c, mycollection, false);

        myitem = wi.getItem();

        // now fill out dublin core for item        
        loadDublinCore( c, myitem, path + "/" + itemname + "/" + "dublin_core.xml" );

        // and the bitstreams from the contents file
        // process contents file, add bistreams and bundles
        processContentsFile( c, myitem, path + "/" + itemname, "contents" );

        String myhandle = processHandleFile( c, myitem, path + "/" + itemname,
            "handle" );

        // put item in system
        InstallItem.installItem(c, wi, myhandle);

        // find the handle, and output to map file
        myhandle = HandleManager.findHandle(c, myitem);
                
        if(mapOut!=null)
        {
            mapOut.println( itemname + " " + myhandle );       
        }

        return myitem;
    }

    // remove, given the actual item
    private void removeItem( Context c, Item myitem )
        throws Exception
    {
        Collection[] collections = myitem.getCollections();

        // Remove item from all the collections it's in
        for (int i = 0; i < collections.length; i++)
        {
           	collections[i].removeItem(myitem);
        }
    }

    // remove, given a handle
    private void removeItem( Context c, String myhandle )
        throws Exception
    {
        // bit of a hack - to remove an item, you must remove it
        // from all collections it's a part of, then it will be removed
        Item myitem = (Item)HandleManager.resolveToObject(c, myhandle);

        removeItem( c, myitem );
    }

    ////////////////////////////////////
    // utility methods
    ////////////////////////////////////

    // read in the map file and generate a hashmap of (file,handle) pairs
    private HashMap readMapFile( String filename )
        throws Exception
    {
        HashMap myhash = new HashMap();

        BufferedReader is = new BufferedReader( new FileReader( filename     ) );
        String line;

        while( ( line = is.readLine() ) != null )
        {
            String myfile;
            String myhandle;
            
            // a line should be archive filename<whitespace>handle
            StringTokenizer st = new StringTokenizer( line );
            if( st.hasMoreTokens() )
            {
                myfile = st.nextToken();    
            }
            else throw new Exception("Bad mapfile line:\n" + line );
            
            if( st.hasMoreTokens() )
            {
                myhandle = st.nextToken();
            }
            else throw new Exception("Bad mapfile line:\n" + line );

            myhash.put( myfile, myhandle );
        }
        is.close();

        return myhash;
    }

    private void loadDublinCore(Context c, Item myitem, String filename)
        throws SQLException, IOException, ParserConfigurationException,
            SAXException, TransformerException //, AuthorizeException
    {
        Document document = loadXML(filename);

        // Get the nodes corresponding to formats
        NodeList dcNodes = XPathAPI.selectNodeList(document,
            "/dublin_core/dcvalue");

        System.out.println("\tLoading dublin core from " + filename );

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
        String element   = getAttributeValue(n, "element"  );
        String qualifier = getAttributeValue(n, "qualifier"); //NodeValue(); //getElementData(n, "qualifier");
        String language  = getAttributeValue(n, "language" );

        System.out.println("\tElement: " + element + " Qualifier: " + qualifier +
        " Value: " + value );

        if( qualifier.equals("none") ) qualifier = null;

        // if language isn't set, use the system's default value
        if( language.equals("") )
        {
            language = ConfigurationManager.getProperty("default.language");
        }

        // a goofy default, but there it is
        if( language == null ) { language = "en"; }


        i.addDC(element, qualifier, language, value);
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
     * Read in the handle file or return null if empty or doesn't exist
     */
    private String processHandleFile( Context c, Item i, String path, String filename )
    {
        String filePath = path + "/" + filename;
        String line     = "";
        String result   = null;
        
        System.out.println( "Processing handle file: " + filename );

        try
        {
            BufferedReader is = new BufferedReader( new FileReader( filePath ) );

            // result gets contents of file, or null            
            result = is.readLine();
            
            System.out.println( "read handle: '" + result + "'");
            
            is.close();
        }
        catch( Exception e )
        {
            // probably no handle file, just return null
            System.out.println( "It appears there is no handle file" );
        }
        
        return result;
    }


    /**
     * Given a contents file and an item, stuffing it with bitstreams from the
     *  contents file
     */
    private void processContentsFile( Context c, Item i, String path, String filename )
    {
        String contentspath = path + "/" + filename;
        String line = "";

        System.out.println( "\tProcessing contents file: " + contentspath );

        try
        {
            BufferedReader is = new BufferedReader( new FileReader( contentspath ) );
            while( ( line = is.readLine() ) != null )
            {
                System.out.println( "\tBitstream: " + line );
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

        // get an input stream
        BufferedInputStream bis = new BufferedInputStream( new FileInputStream( fullpath ) );

        // add it to the item in a bundle
        Bitstream bs = i.createSingleBitstream(bis);

        bs.setName( name );

        // Identify the format
        BitstreamFormat bf = FormatIdentifier.guessFormat(c, bs);
        bs.setFormat(bf);

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
    private String getElementData( Node parentElement, String childName )
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
    private static Document loadXML( String filename )
        throws IOException, ParserConfigurationException, SAXException
    {
        DocumentBuilder builder =
            DocumentBuilderFactory.newInstance().newDocumentBuilder();

        return builder.parse(new File(filename));
    }
}


