/*
 * PolicySet.java - Command line hack for setting all of the
 *  policies for a collection's items
 *
 * $Id$
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

package org.dspace.authorize;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.browse.Browse;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSIndexer;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.DatabaseManager;


/**
 * Was Hack/Tool to set policies for items, bundles, and bitstreams.
 *  Now has helpful method, setPolicies();
 *
 * @author   dstuve
 * @version  $Revision$
 */
public class PolicySet
{
    /**
     *  Old code, kept around in case there's a need to bring a collection's
     *   contents' policies in line with the collections'
     * @param context
     * @param collection
     */
    public static void syncCollection_obsolete(Context c, Collection collection)
        throws SQLException, AuthorizeException
    {
/*        // find all items in a collection, and clone the collection's read policy
        ItemIterator items = collection.getItems();

        int mycount = 0;

        List policies = AuthorizeManager.getPoliciesActionFilter(
            c, collection, Constants.READ);
        
        if( policies.isEmpty() )
        {
            System.out.println( "Error: collection has no READ policies" );
            return;
        }       
                        
        while( items.hasNext() )
        {
            mycount++;

            Item i = items.next();
	        System.out.println(mycount + ": Replacing policies for item " + i.getID() );

            i.replaceAllPolicies(policies);
        }

        // now do the logo bitstream also (great variable name :-)
        Bitstream bs = collection.getLogo();
        AuthorizeManager.removeAllPolicies(c, bs);
        AuthorizeManager.addPolicies(c, policies, bs);
*/
    }



    /**
     * Command line interface to setPolicies - run to see arguments
     */
    public static void main( String [] argv )
        throws Exception
    {
        if( argv.length < 6 )
        {
            System.out.println("Args: containerType containerID contentType actionID groupID command");
            System.out.println("container=COLLECTION command = ADD|REPLACE");
            return;
        }

        int containertype= Integer.parseInt(argv[0]);
        int containerID  = Integer.parseInt(argv[1]);
        int contenttype  = Integer.parseInt(argv[2]);
        int actionID     = Integer.parseInt(argv[3]);
        int groupID      = Integer.parseInt(argv[4]);
        
        boolean isReplace = false;
        String command   = argv[5];

        if( command.equals("REPLACE") )
        {
            isReplace = true;
        }

        Context c = new Context();

        // turn off authorization
        c.setIgnoreAuthorization(true);

        //////////////////////
        // carnage begins here
        //////////////////////

        setPolicies(c, containertype, containerID, contenttype, actionID, groupID, isReplace, false);
 
        c.complete();
    }


    /**
     * Useful policy wildcard tool.  Can set entire collections' contents'
     *  policies
     * @param context
     * @param container type, Constants.ITEM or Constants.COLLECTION
     * @param container ID
     * @param content type (BUNDLE, ITEM, or BITSTREAM)
     * @param replace, removing old policies, or just add to existing policies
     * @param just delete all policies for matching objects
     */
    public static void setPolicies(Context c, int containerType, int containerID,
                             int contentType, int actionID, int groupID,
                             boolean isReplace, boolean clearOnly)
        throws SQLException, AuthorizeException
    {
        if( containerType == Constants.COLLECTION )
        {
            Collection collection = Collection.find(c, containerID);
            Group group = Group.find(c, groupID);
            
            ItemIterator i = collection.getItems();
            
            if( contentType == Constants.ITEM )
            {
                // build list of all items in a collection
                while( i.hasNext() )
                {
                    Item myitem = i.next();
                    
                    // is this a replace? delete policies first
                    if( isReplace || clearOnly )
                    {
                         AuthorizeManager.removeAllPolicies(c, myitem);
                    }
                   
                    if( !clearOnly )
                    { 
                        // now add the policy
                        ResourcePolicy rp = ResourcePolicy.create(c);
                        
                        rp.setResource( myitem   );
                        rp.setAction  ( actionID );
                        rp.setGroup   ( group    );
                        
                        rp.update();
                    }
                }
            }
            else if( contentType == Constants.BUNDLE    )
            {
                // build list of all items in a collection
                // build list of all bundles in those items
                
                while( i.hasNext() )
                {
                    Item myitem = i.next();
                    
                    Bundle[] bundles = myitem.getBundles();
                    
                    for( int j = 0; j < bundles.length; j++ )
                    {
                        Bundle t = bundles[j]; // t for target
                        
                        // is this a replace? delete policies first
                        if( isReplace || clearOnly )
                        {
                            AuthorizeManager.removeAllPolicies(c, t);
                        }

                        if( !clearOnly )
                        { 
                            // now add the policy
                            ResourcePolicy rp = ResourcePolicy.create(c);
                        
                            rp.setResource( t        );
                            rp.setAction  ( actionID );
                            rp.setGroup   ( group    );
                        
                            rp.update();
                        }
                    }
                }
                

            }
            else if( contentType == Constants.BITSTREAM )
            {
                // build list of all bitstreams in a collection
                // iterate over items, bundles, get bitstreams
                while( i.hasNext() )
                {
                    Item myitem = i.next();
                    System.out.println("Item " + myitem.getID() );
                    
                    Bundle[] bundles = myitem.getBundles();
                    
                    for( int j = 0; j < bundles.length; j++ )
                    {
                       System.out.println("Bundle " + bundles[j].getID() );

                        Bitstream [] bitstreams = bundles[j].getBitstreams();
                        
                        for(int k = 0; k < bitstreams.length; k++ )
                        {
                            Bitstream t = bitstreams[k];  // t for target
                            
                            // is this a replace? delete policies first
                            if( isReplace || clearOnly )
                            {
                                AuthorizeManager.removeAllPolicies(c, t);
                            }
                       
                            if( !clearOnly ) 
                            {
                                // now add the policy
                                ResourcePolicy rp = ResourcePolicy.create(c);
                        
                                rp.setResource( t        );
                                rp.setAction  ( actionID );
                                rp.setGroup   ( group    );
                        
                                rp.update();
                            }
                        }
                    }
                }
            }
        }
    }
}
