/*
 * DSIndexer.java
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

package org.dspace.search;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

//import org.dspace.administer.DCType;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.ItemIterator;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import java.sql.SQLException;
import java.io.IOException;

// issues:
//   need to use in_archive field
//   only does author and title

public class DSIndexer
{
    /** IndexItem() adds a single item to the index
     */

    public static void indexItem(Context c, Item myitem)
    {
        indexItems(c, myitem);
    }

    /** createIndex() creates an entirely new index in /tmp
     */

    public static void createIndex(Context c)
    {
//        System.out.println("Beginning indexing");
        indexItems(c, null);
    }


    /** indexItems() does the actual database query to generate the metadata
     *  file for freeWAIS to index
     * @param item_id if -1 means generate a full index
     */

    private static synchronized void indexItems(Context c, Item target_item)
    {
        // actually create the index

        try
        {
            // Set up the Lucene index engine
            IndexWriter writer;

            String index_directory = ConfigurationManager.getProperty("search.dir");

            if (target_item != null)
            {
                // does not clear the index (target_item is set)
    	        writer = new IndexWriter(index_directory, new DSAnalyzer(), false);
            }
            else
            {
                // create an entirely new index
	            writer = new IndexWriter(index_directory, new DSAnalyzer(), true);
            }

            if( target_item != null )
            {
                // If only one item, find it and index
                indexSingleItem(c, writer, target_item);
            }
            else
            {
                // If all items, find all and index
                ItemIterator myitems = Item.findAll(c);

                while( myitems.hasNext() )
                {
                    Item myitem = (Item)myitems.next();
                    indexSingleItem(c, writer, myitem);
                }
            }

            writer.close();
        }
        catch (SQLException e)
        {
            System.out.println("SQL Exception: " + e);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e);
        }
    }

    private static String buildLocationString(Context c, Item myitem)
        throws SQLException
    {
        // build list of community ids
        Community [] communities = myitem.getCommunities();

        // build list of collection ids
        Collection [] collections = myitem.getCollections();

        // now put those into strings
        String location = "";
        int i = 0;

        for(i=0; i<communities.length; i++ ) location = new String(location + " m" + communities[i].getID() );
        for(i=0; i<collections.length; i++ ) location = new String(location + " l" + collections[i].getID() );

        return location;
    }


    private static void indexSingleItem(Context c, IndexWriter writer, Item myitem )
        throws SQLException, IOException
    {

        // get the location string (for searching by collection & community)
        String location_text = buildLocationString(c, myitem);

        // extract metadata (ANY is wildcard from Item class)
        DCValue [] authors = myitem.getDC( "contributor","author", Item.ANY );
        DCValue [] titles  = myitem.getDC( "title",      Item.ANY,     Item.ANY );
        DCValue [] keywords= myitem.getDC( "subject",    Item.ANY,     Item.ANY );

        // put them all from an array of strings to one string for writing out
        int j = 0;
        String author_text = "";
        String title_text  = "";
        String keyword_text= "";

        // pack all of the arrays of DCValues into plain text strings for the indexer
        for(j=0; j<authors.length;  j++ ) author_text = new String( author_text  + authors [j].value + " " );
        for(j=0; j<titles.length;   j++ ) title_text  = new String( title_text   + titles  [j].value + " " );
        for(j=0; j<keywords.length; j++ ) keyword_text= new String( keyword_text + keywords[j].value + " " );

        // write out the metatdata (for scalability, should probably be a hash instead of single strings)
        writeIndexRecord(writer, myitem.getID(), title_text, author_text, keyword_text, location_text);
    }


    /** writeIndexRecord() creates a document from its args
     *  and writes it out to the index that is opened
     */

    private static void writeIndexRecord(IndexWriter iw, int id,
        String title, String authors, String keywords, String location)
        throws IOException
    {

        Document doc = new Document();
        Integer  ti  = new Integer(id);

        String defaulttext = authors + " " + title + " " + keywords;

        doc.add(Field.UnIndexed("id", ti.toString()));

        if(authors  != null)    doc.add(Field.Text("author",   authors  ));
        if(title    != null)    doc.add(Field.Text("title",    title    ));
        if(keywords != null)    doc.add(Field.Text("keyword",  keywords ));
        if(location != null)    doc.add(Field.Text("location", location ));
        if(defaulttext != null) doc.add(Field.Text("default",  defaulttext));

        iw.addDocument(doc);
    }

/*
    FIXME - need to produce a Context object (anonymous
    public static void main(String[] args)
    {
        createIndex();
        System.out.println("Done with indexing");
    }
*/
}
