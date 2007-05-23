/*
 * FixDefaultPolicies.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

//import org.dspace.browse.Browse;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * Command line tool to locate collections without default item and bitstream
 * read policies, and assign them some. (They must be there for submitted items
 * to inherit.)
 * 
 * @author dstuve
 * @version $Revision$
 */
public class FixDefaultPolicies
{
    /**
     * Command line interface to setPolicies - run to see arguments
     */
    public static void main(String[] argv) throws Exception
    {
        Context c = new Context();

        // turn off authorization
        c.setIgnoreAuthorization(true);

        //////////////////////
        // carnage begins here
        //////////////////////
        Collection[] collections = Collection.findAll(c);

        for (int i = 0; i < collections.length; i++)
        {
            Collection t = collections[i];

            System.out.println("Collection " + t + " " + t.getMetadata("name"));

            // check for READ
            if (checkForPolicy(c, t, Constants.READ))
            {
                System.out.println("\tFound READ policies!");
            }
            else
            {
                System.out.println("\tNo READ policy found, adding anonymous.");
                addAnonymousPolicy(c, t, Constants.READ);
            }

            if (checkForPolicy(c, t, Constants.DEFAULT_ITEM_READ))
            {
                System.out.println("\tFound DEFAULT_ITEM_READ policies!");
            }
            else
            {
                System.out
                        .println("\tNo DEFAULT_ITEM_READ policy found, adding anonymous.");
                addAnonymousPolicy(c, t, Constants.DEFAULT_ITEM_READ);
            }

            if (checkForPolicy(c, t, Constants.DEFAULT_BITSTREAM_READ))
            {
                System.out.println("\tFound DEFAULT_BITSTREAM_READ policies!");
            }
            else
            {
                System.out
                        .println("\tNo DEFAULT_BITSTREAM_READ policy found, adding anonymous.");
                addAnonymousPolicy(c, t, Constants.DEFAULT_BITSTREAM_READ);
            }
        }

        // now ensure communities have READ policies
        Community[] communities = Community.findAll(c);

        for (int i = 0; i < communities.length; i++)
        {
            Community t = communities[i];

            System.out.println("Community " + t + " " + t.getMetadata("name"));

            // check for READ
            if (checkForPolicy(c, t, Constants.READ))
            {
                System.out.println("\tFound READ policies!");
            }
            else
            {
                System.out.println("\tNo READ policy found, adding anonymous.");
                addAnonymousPolicy(c, t, Constants.READ);
            }
        }

        c.complete();
    }

    /**
     * check to see if a collection has any policies for a given action
     */
    private static boolean checkForPolicy(Context c, DSpaceObject t,
            int myaction) throws SQLException
    {
        // check to see if any policies exist for this action
        List policies = AuthorizeManager
                .getPoliciesActionFilter(c, t, myaction);
        Iterator i = policies.iterator();

        return i.hasNext();
    }

    /**
     * add an anonymous group permission policy to the collection for this
     * action
     */
    private static void addAnonymousPolicy(Context c, DSpaceObject t,
            int myaction) throws SQLException, AuthorizeException
    {
        // group 0 is the anonymous group!
        Group anonymousGroup = Group.find(c, 0);

        // now create the default policies for submitted items
        ResourcePolicy myPolicy = ResourcePolicy.create(c);
        myPolicy.setResource(t);
        myPolicy.setAction(myaction);
        myPolicy.setGroup(anonymousGroup);
        myPolicy.update();
    }
}
