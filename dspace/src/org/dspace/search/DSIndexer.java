/*
 * DSIndexer.java
 *
 * $Id$
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;


// issues:
//   need to use in_archive field
//   only does author and title

public class DSIndexer
{
    /** IndexItem() adds a single item to the index
     */
    public static void indexContent(Context c, DSpaceObject dso)
        throws SQLException, IOException
    {
        IndexWriter writer = openIndex(c, false);

        switch( dso.getType() )
        {
            case Constants.ITEM:
                writeItemIndex(c, writer, (Item)dso);
                break;

            case Constants.COLLECTION:
                writeCollectionIndex(c, writer, (Collection)dso);
                break;

            case Constants.COMMUNITY:
                writeCommunityIndex(c, writer, (Community)dso);
                break;
            // FIXME: should probably default unknown type exception
        }

        closeIndex(c, writer);
    }


    /** unIndex removes an Item, Collection, or Community
     *  only works if the DSpaceObject has a handle
     *  (uses the handle for its unique ID)
     *
     * @param dso DSpace Object, can be Community, Item, or Collection
     */
    public static void unIndexContent(Context c, DSpaceObject dso)
        throws SQLException, IOException
    {
        String index_directory = ConfigurationManager.getProperty("search.dir");
        IndexReader ir = IndexReader.open(index_directory);

        String h = HandleManager.findHandle(c, dso);

        if( h != null )
        {
            // we have a handle (our unique ID, so remove)
            Term t = new Term("handle", h);
            ir.delete(t);
            ir.close();
        }
        else
        {
            // FIXME: no handle, fail quietly - should log failure
        }
    }


    /** reIndexContent removes something from the index, then re-indexes it
     * @param context
     * @param DSpaceObject
     */
    public static void reIndexContent(Context c, DSpaceObject dso)
        throws SQLException, IOException
    {
        unIndexContent(c, dso);
        indexContent(c, dso);   
    }


    /**
     * create full index - wiping old index
     * @param context
     */
    public static void createIndex(Context c)
        throws SQLException, IOException
    {
        IndexWriter writer = openIndex(c, true);

        indexAllCommunities(c, writer);
        indexAllCollections(c, writer);
        indexAllItems(c, writer);

        closeIndex(c, writer);
    }


    /**
     * When invoked as a command-line tool, (re)-builds the whole index
     *
     * @param args   the command-line arguments, none used
     */
    public static void main(String[] args) throws Exception
    {
        Context c = new Context();
        c.setIgnoreAuthorization(true);

        createIndex(c);

        System.out.println("Done with indexing");
    }


    ////////////////////////////////////
    //      Private
    ////////////////////////////////////

    /** prepare index, opening writer, and wiping out existing index
     * if necessary
     */
    private static IndexWriter openIndex(Context c, boolean wipe_existing)
        throws IOException
    {
        IndexWriter writer;

        String index_directory = ConfigurationManager.getProperty("search.dir");

        writer = new IndexWriter(index_directory, new DSAnalyzer(), wipe_existing);

        return writer;
    }


    /** close up the indexing engine
     */
    private static void closeIndex(Context c, IndexWriter writer)
        throws IOException
    {
        writer.close();
    }


    private static String buildItemLocationString(Context c, Item myitem)
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


    private static String buildCollectionLocationString(Context c, Collection target)
        throws SQLException
    {
        // build list of community ids
        Community [] communities = target.getCommunities();

        // now put those into strings
        String location = "";
        int i = 0;

        for(i=0; i<communities.length; i++ ) location = new String(location + " m" + communities[i].getID() );

        return location;
    }


    /**
     * iterate through the communities, and index each one
     */
    private static void indexAllCommunities(Context c, IndexWriter writer)
        throws SQLException, IOException
    {
        Community [] targets = Community.findAll(c);

        int i;

        for(i=0; i<targets.length; i++)
            writeCommunityIndex(c, writer, targets[i]);
    }


    /**
     * iterate through collections, indexing each one
     */
    private static void indexAllCollections(Context c, IndexWriter writer)
        throws SQLException, IOException
    {
        Collection [] targets = Collection.findAll(c);

        int i;

        for(i=0; i<targets.length; i++)
            writeCollectionIndex(c, writer, targets[i]);
    }


    /**
     * iterate through all items, indexing each one
     */
    private static void indexAllItems(Context c, IndexWriter writer)
        throws SQLException, IOException
    {
        ItemIterator i = Item.findAll(c);

        while(i.hasNext())
        {
            Item target = (Item)i.next();

            writeItemIndex(c, writer, target);
        }
    }


    /**
     * write index record for a community
     */
    private static void writeCommunityIndex(Context c, IndexWriter writer, Community target)
        throws SQLException, IOException
    {
        // build a hash for the metadata
        HashMap textvalues = new HashMap();

        // and populate it
        String name        = target.getMetadata("name");
        String description = target.getMetadata("short_description");
        String intro_text  = target.getMetadata("introductory_text");

        textvalues.put("name",        name       );
        textvalues.put("description", description);
        textvalues.put("intro_text",  intro_text );

        // get the handle
        String myhandle = HandleManager.findHandle(c, target);

        writeIndexRecord(writer, Constants.COMMUNITY, target.getID(), myhandle, textvalues);
    }


    /**
     * write an index record for a collection
     */
    private static void writeCollectionIndex(Context c, IndexWriter writer, Collection target)
        throws SQLException, IOException
    {
        String location_text = buildCollectionLocationString(c, target);

        // build a hash for the metadata
        HashMap textvalues = new HashMap();

        // and populate it
        String name        = target.getMetadata("name");
        String description = target.getMetadata("short_description");
        String intro_text  = target.getMetadata("introductory_text");

        textvalues.put("name",       name         );
        textvalues.put("description",description  );
        textvalues.put("intro_text", intro_text   );
        textvalues.put("location",   location_text);

        // get the handle
        String myhandle = HandleManager.findHandle(c, target);

        writeIndexRecord(writer, Constants.COLLECTION, target.getID(), myhandle, textvalues);
    }


    /**
     * writes an index record - the index record is a set of name/value
     * hashes, which are sent to Lucene.
     */
    private static void writeItemIndex(Context c, IndexWriter writer, Item myitem )
        throws SQLException, IOException
    {

        // get the location string (for searching by collection & community)
        String location_text = buildItemLocationString(c, myitem);

        // extract metadata (ANY is wildcard from Item class)
        DCValue [] authors  = myitem.getDC( "contributor", Item.ANY,   Item.ANY );
        DCValue [] titles   = myitem.getDC( "title",       Item.ANY,   Item.ANY );
        DCValue [] keywords = myitem.getDC( "subject",     Item.ANY,   Item.ANY );
        DCValue [] abstracts= myitem.getDC( "description", "abstract", Item.ANY );


        // put them all from an array of strings to one string for writing out
        int j = 0;
        String author_text  = "";
        String title_text   = "";
        String keyword_text = "";
        String abstract_text= "";

        // pack all of the arrays of DCValues into plain text strings for the indexer
        for(j=0; j<authors.length; j++)
        {
            author_text  = new String(author_text  + authors [j].value + " ");
        }
        for(j=0; j<titles.length;  j++)
        {
            title_text   = new String(title_text   + titles  [j].value + " ");
        }
        for(j=0; j<keywords.length;  j++)
        {
            keyword_text = new String(keyword_text + keywords[j].value + " ");
        }
        for(j=0; j<abstracts.length; j++)
        {
            abstract_text= new String(abstract_text+ abstracts[j].value + " ");
        }

        // build a hash
        HashMap textvalues = new HashMap();

        textvalues.put("author",    author_text  );
        textvalues.put("title",     title_text   );
        textvalues.put("keyword",   keyword_text );
        textvalues.put("location",  location_text);

//      delayed until we can assign relative weights
//        textvalues.put("abstract",  abstract_text);

        // lastly, get the handle
        String itemhandle = HandleManager.findHandle(c, myitem);

        // write out the metatdata (for scalability, using hash instead of individual strings)
        writeIndexRecord(writer, Constants.ITEM, myitem.getID(), itemhandle, textvalues);
    }


    /** writeIndexRecord() creates a document from its args
     *  and writes it out to the index that is opened
     */
    private static void writeIndexRecord(IndexWriter iw, int type, int id, String handle,
                                            HashMap textvalues )
        throws IOException
    {
        Document doc     = new Document();
        Integer  ti      = new Integer(id);
        Integer  ty      = new Integer(type);
        String   fulltext= "";

        // do id, type, handle first
        doc.add(Field.UnIndexed("id",       ti.toString() ));
        doc.add(Field.UnIndexed("type",     ty.toString() ));
        doc.add(Field.UnIndexed("handle",   handle        ));

        // now iterate through the hash, building full text string
        // and index all values
        Iterator i = textvalues.keySet().iterator();

        while(i.hasNext())
        {
            String key   = (String)i.next();
            String value = (String)textvalues.get(key);

            fulltext = fulltext + " " + value;

            doc.add(Field.Text(key, value));
        }

        // add the full text
        doc.add( Field.Text("default", fulltext) );

        // index the document
        iw.addDocument(doc);
    }
}

