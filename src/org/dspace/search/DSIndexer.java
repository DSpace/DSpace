/*
 * DSIndexer.java
 *
 * $Id: DSIndexer.java,v 1.35 2005/02/08 20:43:05 rtansley Exp $
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
package org.dspace.search;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * DSIndexer contains the methods that index Items and their metadata,
 * collections, communities, etc. It is meant to either be invoked from the
 * command line (see dspace/bin/index-all) or via the indexContent() methods
 * within DSpace.
 */
public class DSIndexer
{
    private static final Logger log = Logger.getLogger(DSIndexer.class);

    /**
     * IndexItem() adds a single item to the index
     */
    public static void indexContent(Context c, DSpaceObject dso)
            throws SQLException, IOException
    {
        IndexWriter writer = openIndex(c, false);

        try
        {
            switch (dso.getType())
            {
            case Constants.ITEM:
                writeItemIndex(c, writer, (Item) dso);

                break;

            case Constants.COLLECTION:
                writeCollectionIndex(c, writer, (Collection) dso);

                break;

            case Constants.COMMUNITY:
                writeCommunityIndex(c, writer, (Community) dso);

                break;

            // FIXME: should probably default unknown type exception
            }
        }
        finally
        {
            closeIndex(c, writer);
        }
    }

    /**
     * unIndex removes an Item, Collection, or Community only works if the
     * DSpaceObject has a handle (uses the handle for its unique ID)
     * 
     * @param dso
     *            DSpace Object, can be Community, Item, or Collection
     */
    public static void unIndexContent(Context c, DSpaceObject dso)
            throws SQLException, IOException
    {
        String h = HandleManager.findHandle(c, dso);

        unIndexContent(c, h);
    }

    public static void unIndexContent(Context c, String myhandle)
            throws SQLException, IOException
    {
        String index_directory = ConfigurationManager.getProperty("search.dir");
        IndexReader ir = IndexReader.open(index_directory);

        try
        {
            if (myhandle != null)
            {
                // we have a handle (our unique ID, so remove)
                Term t = new Term("handle", myhandle);
                ir.delete(t);
            }
            else
            {
                log.warn("unindex of content with null handle attempted");

                // FIXME: no handle, fail quietly - should log failure
                //System.out.println("Error in unIndexContent: Object had no
                // handle!");
            }
        }
        finally
        {
            ir.close();
        }
    }

    /**
     * reIndexContent removes something from the index, then re-indexes it
     * 
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
     * 
     * @param context
     */
    public static void createIndex(Context c) throws SQLException, IOException
    {
        IndexWriter writer = openIndex(c, true);

        try
        {
            indexAllCommunities(c, writer);
            indexAllCollections(c, writer);
            indexAllItems(c, writer);

            // optimize the index - important to do regularly to reduce
            // filehandle
            // usage
            // and keep performance fast!
            writer.optimize();
        }
        finally
        {
            closeIndex(c, writer);
        }
    }

    /**
     * When invoked as a command-line tool, (re)-builds the whole index
     * 
     * @param args
     *            the command-line arguments, none used
     */
    public static void main(String[] args) throws Exception
    {
        Context c = new Context();

        // for testing, pass in a handle of something to remove...
        if ((args.length == 2) && (args[0].equals("remove")))
        {
            unIndexContent(c, args[1]);
        }
        else
        {
            c.setIgnoreAuthorization(true);

            createIndex(c);

            System.out.println("Done with indexing");
        }
    }

    ////////////////////////////////////
    //      Private
    ////////////////////////////////////

    /**
     * prepare index, opening writer, and wiping out existing index if necessary
     */
    private static IndexWriter openIndex(Context c, boolean wipe_existing)
            throws IOException
    {
        IndexWriter writer;

        String index_directory = ConfigurationManager.getProperty("search.dir");

        writer = new IndexWriter(index_directory, new DSAnalyzer(),
                wipe_existing);

        /* Set maximum number of terms to index if present in dspace.cfg */
        if (ConfigurationManager.getProperty("search.maxfieldlength") != null)
        {
            int maxfieldlength = ConfigurationManager
                    .getIntProperty("search.maxfieldlength");
            if (maxfieldlength == -1)
            {
                writer.maxFieldLength = Integer.MAX_VALUE;
            }
            else
            {
                writer.maxFieldLength = maxfieldlength;
            }
        }

        return writer;
    }

    /**
     * close up the indexing engine
     */
    private static void closeIndex(Context c, IndexWriter writer)
            throws IOException
    {
        if (writer != null)
        {
            writer.close();
        }
    }

    private static String buildItemLocationString(Context c, Item myitem)
            throws SQLException
    {
        // build list of community ids
        Community[] communities = myitem.getCommunities();

        // build list of collection ids
        Collection[] collections = myitem.getCollections();

        // now put those into strings
        String location = "";
        int i = 0;

        for (i = 0; i < communities.length; i++)
            location = new String(location + " m" + communities[i].getID());

        for (i = 0; i < collections.length; i++)
            location = new String(location + " l" + collections[i].getID());

        return location;
    }

    private static String buildCollectionLocationString(Context c,
            Collection target) throws SQLException
    {
        // build list of community ids
        Community[] communities = target.getCommunities();

        // now put those into strings
        String location = "";
        int i = 0;

        for (i = 0; i < communities.length; i++)
            location = new String(location + " m" + communities[i].getID());

        return location;
    }

    /**
     * iterate through the communities, and index each one
     */
    private static void indexAllCommunities(Context c, IndexWriter writer)
            throws SQLException, IOException
    {
        Community[] targets = Community.findAll(c);

        int i;

        for (i = 0; i < targets.length; i++)
            writeCommunityIndex(c, writer, targets[i]);
    }

    /**
     * iterate through collections, indexing each one
     */
    private static void indexAllCollections(Context c, IndexWriter writer)
            throws SQLException, IOException
    {
        Collection[] targets = Collection.findAll(c);

        int i;

        for (i = 0; i < targets.length; i++)
            writeCollectionIndex(c, writer, targets[i]);
    }

    /**
     * iterate through all items, indexing each one
     */
    private static void indexAllItems(Context c, IndexWriter writer)
            throws SQLException, IOException
    {
        ItemIterator i = Item.findAll(c);

        while (i.hasNext())
        {
            Item target = (Item) i.next();

            writeItemIndex(c, writer, target);
        }
    }

    /**
     * write index record for a community
     */
    private static void writeCommunityIndex(Context c, IndexWriter writer,
            Community target) throws SQLException, IOException
    {
        // build a hash for the metadata
        HashMap textvalues = new HashMap();

        // get the handle
        String myhandle = HandleManager.findHandle(c, target);

        // and populate it
        String name = target.getMetadata("name");

        //        String description = target.getMetadata("short_description");
        //        String intro_text = target.getMetadata("introductory_text");
        textvalues.put("name", name);

        //        textvalues.put("description", description);
        //        textvalues.put("intro_text", intro_text );
        textvalues.put("handletext", myhandle);

        writeIndexRecord(writer, Constants.COMMUNITY, myhandle, textvalues, "");
    }

    /**
     * write an index record for a collection
     */
    private static void writeCollectionIndex(Context c, IndexWriter writer,
            Collection target) throws SQLException, IOException
    {
        String location_text = buildCollectionLocationString(c, target);

        // get the handle
        String myhandle = HandleManager.findHandle(c, target);

        // build a hash for the metadata
        HashMap textvalues = new HashMap();

        // and populate it
        String name = target.getMetadata("name");

        //        String description = target.getMetadata("short_description");
        //        String intro_text = target.getMetadata("introductory_text");
        textvalues.put("name", name);

        //        textvalues.put("description",description );
        //        textvalues.put("intro_text", intro_text );
        textvalues.put("location", location_text);
        textvalues.put("handletext", myhandle);

        writeIndexRecord(writer, Constants.COLLECTION, myhandle, textvalues, "");
    }

    /**
     * writes an index record - the index record is a set of name/value hashes,
     * which are sent to Lucene.
     */
    private static void writeItemIndex(Context c, IndexWriter writer,
            Item myitem) throws SQLException, IOException
    {
        // get the location string (for searching by collection & community)
        String location_text = buildItemLocationString(c, myitem);

        // read in indexes from the config
        ArrayList indexes = new ArrayList();

        // read in search.index.1, search.index.2....
        for (int i = 1; ConfigurationManager.getProperty("search.index." + i) != null; i++)
        {
            indexes.add(ConfigurationManager.getProperty("search.index." + i));
        }

        int j;
        int k = 0;

        // initialize hash to be built
        HashMap textvalues = new HashMap();

        if (indexes.size() > 0)
        {
            ArrayList fields = new ArrayList();
            ArrayList content = new ArrayList();
            DCValue[] mydc;

            for (int i = 0; i < indexes.size(); i++)
            {
                String index = (String) indexes.get(i);

                String[] dc = index.split(":");
                String myindex = dc[0];

                String[] elements = dc[1].split("\\.");
                String element = elements[0];
                String qualifier = elements[1];

                // extract metadata (ANY is wildcard from Item class)
                if (qualifier.equals("*"))
                {
                    mydc = myitem.getDC(element, Item.ANY, Item.ANY);
                }
                else
                {
                    mydc = myitem.getDC(element, qualifier, Item.ANY);
                }

                // put them all from an array of strings to one string for
                // writing
                // out
                // pack all of the arrays of DCValues into plain text strings
                // for
                // the indexer
                String content_text = "";

                for (j = 0; j < mydc.length; j++)
                {
                    content_text = new String(content_text + mydc[j].value
                            + " ");
                }

                // arranges content with fields in ArrayLists with same index to
                // put
                // into hash later
                k = fields.indexOf(myindex);

                if (k < 0)
                {
                    fields.add(myindex);
                    content.add(content_text);
                }
                else
                {
                    content_text = new String(content_text
                            + (String) content.get(k) + " ");
                    content.set(k, content_text);
                }
            }

            // build the hash
            for (int i = 0; i < fields.size(); i++)
            {
                textvalues.put((String) fields.get(i), (String) content.get(i));
            }

            textvalues.put("location", location_text);
        }
        else
        // if no search indexes found in cfg file, for backward compatibility
        {
            // extract metadata (ANY is wildcard from Item class)
            DCValue[] authors = myitem.getDC("contributor", Item.ANY, Item.ANY);
            DCValue[] creators = myitem.getDC("creator", Item.ANY, Item.ANY);
            DCValue[] titles = myitem.getDC("title", Item.ANY, Item.ANY);
            DCValue[] keywords = myitem.getDC("subject", Item.ANY, Item.ANY);

            DCValue[] abstracts = myitem.getDC("description", "abstract",
                    Item.ANY);
            DCValue[] sors = myitem.getDC("description",
                    "statementofresponsibility", Item.ANY);
            DCValue[] series = myitem.getDC("relation", "ispartofseries",
                    Item.ANY);
            DCValue[] tocs = myitem.getDC("description", "tableofcontents",
                    Item.ANY);
            DCValue[] mimetypes = myitem.getDC("format", "mimetype", Item.ANY);
            DCValue[] sponsors = myitem.getDC("description", "sponsorship",
                    Item.ANY);
            DCValue[] identifiers = myitem.getDC("identifier", Item.ANY,
                    Item.ANY);

            // put them all from an array of strings to one string for writing
            // out
            String author_text = "";
            String title_text = "";
            String keyword_text = "";

            String abstract_text = "";
            String sor_text = "";
            String series_text = "";
            String mime_text = "";
            String sponsor_text = "";
            String id_text = "";

            // pack all of the arrays of DCValues into plain text strings for
            // the
            // indexer
            for (j = 0; j < authors.length; j++)
            {
                author_text = new String(author_text + authors[j].value + " ");
            }

            for (j = 0; j < creators.length; j++) //also authors
            {
                author_text = new String(author_text + creators[j].value + " ");
            }

            for (j = 0; j < sors.length; j++) //also authors
            {
                author_text = new String(author_text + sors[j].value + " ");
            }

            for (j = 0; j < titles.length; j++)
            {
                title_text = new String(title_text + titles[j].value + " ");
            }

            for (j = 0; j < keywords.length; j++)
            {
                keyword_text = new String(keyword_text + keywords[j].value
                        + " ");
            }

            for (j = 0; j < abstracts.length; j++)
            {
                abstract_text = new String(abstract_text + abstracts[j].value
                        + " ");
            }

            for (j = 0; j < tocs.length; j++)
            {
                abstract_text = new String(abstract_text + tocs[j].value + " ");
            }

            for (j = 0; j < series.length; j++)
            {
                series_text = new String(series_text + series[j].value + " ");
            }

            for (j = 0; j < mimetypes.length; j++)
            {
                mime_text = new String(mime_text + mimetypes[j].value + " ");
            }

            for (j = 0; j < sponsors.length; j++)
            {
                sponsor_text = new String(sponsor_text + sponsors[j].value
                        + " ");
            }

            for (j = 0; j < identifiers.length; j++)
            {
                id_text = new String(id_text + identifiers[j].value + " ");
            }

            // build the hash
            textvalues.put("author", author_text);
            textvalues.put("title", title_text);
            textvalues.put("keyword", keyword_text);
            textvalues.put("location", location_text);
            textvalues.put("abstract", abstract_text);

            textvalues.put("series", series_text);
            textvalues.put("mimetype", mime_text);
            textvalues.put("sponsor", sponsor_text);
            textvalues.put("identifier", id_text);
        }

        // now get full text of any bitstreams in the TEXT bundle
        String extractedText = "";

        // trundle through the bundles
        Bundle[] myBundles = myitem.getBundles();

        for (int i = 0; i < myBundles.length; i++)
        {
            if ((myBundles[i].getName() != null)
                    && myBundles[i].getName().equals("TEXT"))
            {
                // a-ha! grab the text out of the bitstreams
                Bitstream[] myBitstreams = myBundles[i].getBitstreams();

                for (j = 0; j < myBitstreams.length; j++)
                {
                    try
                    {
                        InputStreamReader is = new InputStreamReader(
                                myBitstreams[j].retrieve()); // get input stream
                        StringBuffer sb = new StringBuffer();
                        char[] charBuffer = new char[1024];

                        while (true)
                        {
                            int bytesIn = is.read(charBuffer);

                            if (bytesIn == -1)
                            {
                                break;
                            }

                            if (bytesIn > 0)
                            {
                                sb.append(charBuffer, 0, bytesIn);
                            }
                        }

                        // now sb has the full text - tack on to fullText string
                        extractedText = extractedText.concat(new String(sb));

                        //                        System.out.println("Found extracted text!\n" + new
                        // String(sb));
                    }
                    catch (AuthorizeException e)
                    {
                        // this will never happen, but compiler is now happy.
                    }
                }
            }
        }

        // lastly, get the handle
        String itemhandle = HandleManager.findHandle(c, myitem);
        textvalues.put("handletext", itemhandle);

        // write out the metatdata (for scalability, using hash instead of
        // individual strings)
        writeIndexRecord(writer, Constants.ITEM, itemhandle, textvalues,
                extractedText);
    }

    /**
     * writeIndexRecord() creates a document from its args and writes it out to
     * the index that is opened
     */
    private static void writeIndexRecord(IndexWriter iw, int type,
            String handle, HashMap textvalues, String extractedText)
            throws IOException
    {
        Document doc = new Document();
        Integer ty = new Integer(type);
        String fulltext = "";

        // do id, type, handle first
        doc.add(Field.UnIndexed("type", ty.toString()));

        // want to be able to search for handle, so use keyword
        // (not tokenized, but it is indexed)
        if (handle != null)
        {
            doc.add(Field.Keyword("handle", handle));
        }

        // now iterate through the hash, building full text string
        // and index all values
        Iterator i = textvalues.keySet().iterator();

        while (i.hasNext())
        {
            String key = (String) i.next();
            String value = (String) textvalues.get(key);

            fulltext = fulltext + " " + value;

            if (value != null)
            {
                doc.add(Field.Text(key, value));
            }
        }

        fulltext = fulltext.concat(extractedText);

        //        System.out.println("Full Text:\n" + fulltext + "------------\n\n");
        // add the full text
        doc.add(Field.Text("default", fulltext));

        // index the document
        iw.addDocument(doc);
    }
}