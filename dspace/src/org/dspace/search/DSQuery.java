/*
 * DSQuery.java
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

package org.dspace.search;


// java classes
import java.io.*;
import java.util.*;
import java.sql.*;

// lucene search engine classes
import org.apache.lucene.index.*;
import org.apache.lucene.document.*;
import org.apache.lucene.queryParser.*;
import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;

import org.apache.log4j.Logger;

//Jakarta-ORO classes (regular expressions)
import org.apache.oro.text.perl.Perl5Util;

// dspace classes
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;


// issues
//   need to filter query string for security
//   cmd line query needs to process args correctly (seems to split them up)

public class DSQuery
{
    // Result types
    static final String ALL		    = "999";
    static final String ITEM		= "" + Constants.ITEM;
    static final String COLLECTION	= "" + Constants.COLLECTION;
    static final String COMMUNITY	= "" + Constants.COMMUNITY;

    // cache a Lucene IndexSearcher for more efficient searches
    private static Searcher searcher;
    private static long lastModified;
    

    /** log4j logger */
    private static Logger log = Logger.getLogger(DSQuery.class);
    
    
    /** Do a query, returning a List of DSpace Handles to objects matching the query.
     *  @param query string in Lucene query syntax
     *
     *  @return HashMap with lists for items, communities, and collections
     *        (keys are strings from Constants.ITEM, Constants.COLLECTION, etc.
     */
    public static QueryResults doQuery(Context c, QueryArgs args)
        throws IOException
    {
        String querystring = args.getQuery();
        QueryResults qr    = new QueryResults();
        List hitHandles    = new ArrayList();
        List hitTypes      = new ArrayList();
        
        // set up the QueryResults object
        qr.setHitHandles( hitHandles );
        qr.setHitTypes( hitTypes );
        qr.setStart( args.getStart() );
        qr.setPageSize( args.getPageSize() );        

        // massage the query string a bit                    
        querystring = checkEmptyQuery    ( querystring );  // change nulls to an empty string
        querystring = workAroundLuceneBug( querystring );  // logicals changed to && ||, etc.
        querystring = stripHandles       ( querystring );  // remove handles from query string
        querystring = stripAsterisk      ( querystring );  // remove asterisk from beginning of string

        try
        {
            // grab a searcher, and do the search
            Searcher searcher = getSearcher( ConfigurationManager.getProperty("search.dir") );
            
            QueryParser qp = new QueryParser("default", new DSAnalyzer());

            Query myquery = qp.parse(querystring);
            Hits hits     = searcher.search(myquery);
            
            // set total number of hits
            qr.setHitCount( hits.length() );

            // We now have a bunch of hits - snip out a 'window'
            // defined in start, count and return the handles
            // from that window
            
            // first, are there enough hits?
            if( args.getStart() < hits.length() )
            {
                // get as many as we can, up to the window size
                
                // how many are available after snipping off at offset 'start'?
                int hitsRemaining = hits.length() - args.getStart(); 
                
                int hitsToProcess = ( hitsRemaining < args.getPageSize() )
                                ? hitsRemaining : args.getPageSize();
                                
                for( int i = args.getStart(); i < args.getStart() + hitsToProcess; i++ )
                {
                    Document d = hits.doc(i);

                    String handleText = d.get("handle");
                    String handletype = d.get("type"  );
                
                    hitHandles.add(handleText);
                    
                    if( handletype.equals( "" + Constants.ITEM ) )
                    {
                        hitTypes.add( new Integer( Constants.ITEM ) );
                    }
                    else if( handletype.equals( "" + Constants.COLLECTION ) )
                    {
                        hitTypes.add( new Integer( Constants.COLLECTION ) );
                    }
                    else if( handletype.equals( "" + Constants.COMMUNITY ) )
                    {
                        hitTypes.add( new Integer( Constants.COMMUNITY ) );
                    }
                    else
                    {
                        // error!  unknown type!
                    }
                }
            }
        }
        catch (NumberFormatException e)
        {
            // a bad parse means that there are no results
            // doing nothing with the exception gets you
            //   throw new SQLException( "Error parsing search results: " + e );
            // ?? quit?
        }
        catch (ParseException e)
        {
            // a parse exception - log and return null results
            log.warn(LogManager.getHeader(c,
                "Lucene Parse Exception",
                "" + e));
        }

        return qr;
    }

    static String checkEmptyQuery( String myquery )
    {
        if( myquery.equals("") )
        {
            myquery = "empty_query_string";
        }
        
        return myquery;
    }
    
    static String workAroundLuceneBug( String myquery )
    {
		// Lucene currently has a bug which breaks wildcard
		// searching when you have uppercase characters.
		// Here we substitute the boolean operators -- which 
		// have to be uppercase -- before tranforming the 
		// query string to lowercase.
        
        Perl5Util util = new Perl5Util();
        
        myquery = util.substitute("s/ AND / && /g", myquery);
        myquery = util.substitute("s/ OR / || /g", myquery);
        myquery = util.substitute("s/ NOT / ! /g", myquery);
        
        myquery = myquery.toLowerCase();

        return myquery;
    }
    

    static String stripHandles( String myquery )
    {
		// Drop beginning pieces of full handle strings
        
        Perl5Util util = new Perl5Util();
        
        myquery = util.substitute("s|^(\\s+)?http://hdl\\.handle\\.net/||", myquery);
        myquery = util.substitute("s|^(\\s+)?hdl:||", myquery);

        return myquery;
    }
    
    static String stripAsterisk( String myquery )
    {
		// query strings (or words) begining with "*" cause a null pointer error 
        
        Perl5Util util = new Perl5Util();
        
        myquery = util.substitute("s/^\\*//", myquery);
        myquery = util.substitute("s| \\*| |", myquery);

        return myquery;
    }


    /** Do a query, restricted to a collection
     * @param query
     * @param collection
     *
     * @return QueryResults same results as doQuery, restricted to a collection
     */
    public static QueryResults doQuery(Context c, QueryArgs args, Collection coll)
        throws IOException, ParseException
    {
        String querystring = args.getQuery();
        
        querystring = checkEmptyQuery( querystring );
    
        String location = "l" + (coll.getID());

        String newquery = new String("+(" + querystring + ") +location:\"" + location + "\"");

        args.setQuery( newquery );

        return doQuery(c, args);
    }


    /** Do a query, restricted to a community
     * @param querystring
     * @param community
     *
     * @return HashMap results, same as full doQuery, only hits in a Community
     */
    public static QueryResults doQuery(Context c, QueryArgs args, Community comm)
        throws IOException, ParseException
    {
        String querystring = args.getQuery();
        
        querystring = checkEmptyQuery( querystring );

        String location = "m" + (comm.getID());

        String newquery = new String("+(" + querystring + ") +location:\"" + location + "\"");

        args.setQuery( newquery );

        return doQuery(c, args);
    }

    /** return everything from a query
     * @param results hashmap from doQuery
     *
     * @return List of all objects returned by search
     */
    public static List getResults(HashMap results)
    {
    	return ((List)results.get(ALL));
    }


    /** return just the items from a query
     * @param results hashmap from doQuery
     *
     * @return List of items found by query
     */
    public static List getItemResults(HashMap results)
    {
    	return ((List)results.get(ITEM));
    }


    /** return just the collections from a query
     * @param results hashmap from doQuery
     *
     * @return List of collections found by query
     */
    public static List getCollectionResults(HashMap results)
    {
    	return ((List)results.get(COLLECTION));
    }


    /** return just the communities from a query
     * @param results hashmap from doQuery
     *
     * @return list of Communities found by query
     */
    public static List getCommunityResults(HashMap results)
    {
    	return ((List)results.get(COMMUNITY));
    }


    /** returns true if anything found
     * @param results hashmap from doQuery
     *
     * @return true if anything found, false if nothing
     */
    public static boolean resultsFound(HashMap results)
    {
		List thislist = getResults(results);
		return (!thislist.isEmpty());
	}

    /** returns true if items found
     * @param results hashmap from doQuery
     *
     * @return true if items found, false if none found
     */
    public static boolean itemsFound(HashMap results)
    {
		List thislist = getItemResults(results);
		return (!thislist.isEmpty());
	}

    /** returns true if collections found
     * @param results hashmap from doQuery
     *
     * @return true if collections found, false if none
     */
    public static boolean collectionsFound(HashMap results)
    {
		List thislist = getCollectionResults(results);
		return (!thislist.isEmpty());
	}

    /** returns true if communities found
     * @param results hashmap from doQuery
     *
     * @return true if communities found, false if none
     */
    public static boolean communitiesFound(HashMap results)
    {
		List thislist = getCommunityResults(results);
		return (!thislist.isEmpty());
	}

    /** Do a query, printing results to stdout
     *  largely for testing, but it is useful
     */
    public static void doCMDLineQuery(String query)
    {
        System.out.println("Command line query: " + query);
        System.out.println("Only reporting default-sized results list");
        
        try
        {
            Context c = new Context();

            QueryArgs args = new QueryArgs();
            args.setQuery(query);

            QueryResults results = doQuery(c, args);

            Iterator i = results.getHitHandles().iterator();
            Iterator j = results.getHitTypes().iterator();
            
            while( i.hasNext() )
            {
                String thisHandle  = (String)i.next();
                Integer thisType   = (Integer)j.next();
                String type = Constants.typeText[thisType.intValue()];
                
                // also look up type
                System.out.println( type + "\t" + thisHandle );
            }
            
        }
        catch (Exception e)
        {
            System.out.println("Exception caught: " + e);
        }
    }


    public static void main(String[] args)
    {
        DSQuery q = new DSQuery();

        if (args.length > 0)
        {
            q.doCMDLineQuery(args[0]);
        }
    }


    /*---------  private methods ----------*/
    
    /**
     * get an IndexSearcher, hopefully a cached one
     *  (gives much better performance.) checks to see
     *  if the index has been modified - if so, it
     *  creates a new IndexSearcher
     */
    private static synchronized Searcher getSearcher( String indexDir )
        throws IOException
    {
        if( lastModified != IndexReader.lastModified( indexDir ) )
        {
            // there's a new index, open it
            lastModified = IndexReader.lastModified( indexDir );
            searcher = new IndexSearcher( indexDir );
        }
        
        return searcher;
    }
}






 
// it's now up to the display page to do the right thing displaying
// items & communities & collections

