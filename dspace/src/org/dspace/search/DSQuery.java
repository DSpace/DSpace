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

    /** log4j logger */
    private static Logger log = Logger.getLogger(DSQuery.class);
    

    /** Do a query, returning a List of DSpace Handles to objects matching the query.
     *  @param query string in Lucene query syntax
     *
     *  @return HashMap with lists for items, communities, and collections
     *        (keys are strings from Constants.ITEM, Constants.COLLECTION, etc.
     */
    public static synchronized HashMap doQuery(Context c, String querystring)
        throws IOException
    {
        querystring = checkEmptyQuery( querystring );
        querystring = workAroundLuceneBug( querystring );
        querystring = stripHandles( querystring );
                        
        ArrayList resultlist= new ArrayList();
        ArrayList itemlist 	= new ArrayList();
        ArrayList commlist 	= new ArrayList();
        ArrayList colllist 	= new ArrayList();
        HashMap   metahash 	= new HashMap();

        // initial results are empty
        metahash.put(ALL,       resultlist);
        metahash.put(ITEM,      itemlist  );
        metahash.put(COLLECTION,colllist  );
        metahash.put(COMMUNITY, commlist  );

        try
        {
            IndexSearcher searcher = new IndexSearcher(
                ConfigurationManager.getProperty("search.dir"));

            QueryParser qp = new QueryParser("default", new DSAnalyzer());

            Query myquery = qp.parse(querystring);
            Hits hits = searcher.search(myquery);

            for (int i = 0; i < hits.length(); i++)
            {
                Document d = hits.doc(i);

                String handletext = d.get("handle");
                String handletype = d.get("type");
                
                resultlist.add(handletext);
                
                if (handletype.equals(ITEM)) 
                { 
                	itemlist.add(handletext); 
                } 
                else if (handletype.equals(COLLECTION))
                {
                    colllist.add(handletext);
                }
                else if (handletype.equals(COMMUNITY))
                {	
                    commlist.add(handletext); break;
                }
            }

            // close the IndexSearcher - and all its filehandles
            searcher.close();

            // store all of the different types of hits in the hash            
            metahash.put(ALL,       resultlist);
            metahash.put(ITEM,      itemlist  );
            metahash.put(COLLECTION,colllist  );
            metahash.put(COMMUNITY, commlist  );
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

        return metahash;
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

    /** Do a query, restricted to a collection
     * @param query
     * @param collection
     *
     * @return HashMap same results as doQuery, restricted to a collection
     */
    public static HashMap doQuery(Context c, String querystring, Collection coll)
        throws IOException, ParseException
    {
        querystring = checkEmptyQuery( querystring );
    
        String location = "l" + (coll.getID());

        String newquery = new String("+(" + querystring + ") +location:\"" + location + "\"");

        return doQuery(c, newquery);
    }


    /** Do a query, restricted to a community
     * @param querystring
     * @param community
     *
     * @return HashMap results, same as full doQuery, only hits in a Community
     */
    public static HashMap doQuery(Context c, String querystring, Community comm)
        throws IOException, ParseException
    {
        querystring = checkEmptyQuery( querystring );

        String location = "m" + (comm.getID());

        String newquery = new String("+(" + querystring + ") +location:\"" + location + "\"");

        return doQuery(c, newquery);
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

        try
        {
            Context c = new Context();
            HashMap results = doQuery(c, query);
            
            List itemlist = getItemResults(results);
            List colllist = getCollectionResults(results);
            List commlist = getCommunityResults(results);
            
            if (communitiesFound(results)) 
            {
	            System.out.println("\n" + "Communities: ");
    	        Iterator i = commlist.iterator();
            
    	        while (i.hasNext())
    	        {
    	        	Object thishandle = i.next();
    	            System.out.println("\t" + thishandle.toString());
    	        }
    	    }

            if (collectionsFound(results)) 
            {
            	System.out.println("\n" + "Collections: ");
            	Iterator j = colllist.iterator();
            
            	while (j.hasNext())
	            {
    	        	Object thishandle = j.next();
    	            System.out.println("\t" + thishandle.toString());
    	        }
    	    }
    	    

   	        System.out.println("\n" + "Items: ");
            Iterator k = itemlist.iterator();
            
            while (k.hasNext())
            {
            	Object thishandle = k.next();
                System.out.println("\t" + thishandle.toString());
            }
            
            if (!itemsFound(results))
            { 
            	System.out.println ("\tNo items found!");
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
}
