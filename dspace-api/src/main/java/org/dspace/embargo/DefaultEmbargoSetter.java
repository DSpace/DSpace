/*
 * DefaultEmbargoSetter.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2009/07/16 01:52:59 $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
package org.dspace.embargo;

import java.sql.SQLException;
import java.io.IOException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Constants;

/**
 * Default plugin implementation of the embargo setting function.
 * The parseTerms() provides only very rudimentary terms logic - entry
 * of a configurable string (in terms field) for 'unlimited' embargo, otherwise
 * a standard ISO 8601 (yyyy-mm-dd) date is assumed. Users are encouraged 
 * to override this method for enhanced functionality.
 *
 * @author Larry Stone
 * @author Richard Rodgers
 */
public class DefaultEmbargoSetter implements EmbargoSetter
{
	private String termsOpen = null;
	
    public DefaultEmbargoSetter()
    {
        super();
        termsOpen = ConfigurationManager.getProperty("embargo.terms.open");
    }
    
    /**
     * Parse the terms into a definite date. Terms are expected to consist of
     * either: a token (value configured in 'embargo.terms.open' property) to indicate
     * indefinite embargo, or a literal lift date formatted in ISO 8601 format (yyyy-mm-dd)
     * 
     * @param context the DSpace context
     * @param item the item to embargo
     * @param terms the embargo terms
     * @return parsed date in DCDate format
     */
    public DCDate parseTerms(Context context, Item item, String terms)
        throws SQLException, AuthorizeException, IOException
    {
    	if (terms != null && terms.length() > 0)
    	{
    		if (termsOpen.equals(terms))
                return EmbargoManager.FOREVER;
            else
                return new DCDate(terms);
    	}
        return null;
    }

    /**
     * Enforce embargo by turning off all read access to bitstreams in
     * this Item.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     */
    public void setEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        for (Bundle bn : item.getBundles())
        {
            // Skip the LICENSE and METADATA bundles, they stay world-readable
            String bnn = bn.getName();
            if (!(bnn.equals(Constants.LICENSE_BUNDLE_NAME) || bnn.equals(Constants.METADATA_BUNDLE_NAME)))
            {
                AuthorizeManager.removePoliciesActionFilter(context, bn, Constants.READ);
                for (Bitstream bs : bn.getBitstreams())
                {
                    AuthorizeManager.removePoliciesActionFilter(context, bs, Constants.READ);
                }
            }
        }
    }

    /**
     * Check that embargo is properly set on Item, e.g. no read access
     * to bitstreams.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     */
    public void checkEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        for (Bundle bn : item.getBundles())
        {
            // Skip the LICENSE and METADATA bundles, they stay world-readable
            String bnn = bn.getName();
            if (!(bnn.equals(Constants.LICENSE_BUNDLE_NAME) || bnn.equals(Constants.METADATA_BUNDLE_NAME)))
            {
                // don't report on "TEXT" or "THUMBNAIL" bundles; those
                // can have READ long as the bitstreams in them do not.
                if (!(bnn.equals("TEXT") || bnn.equals("THUMBNAIL")))
                {
                    // check for ANY read policies and report them:
                    for (ResourcePolicy rp : AuthorizeManager.getPoliciesActionFilter(context, bn, Constants.READ))
                    {
                        System.out.println("CHECK WARNING: Item "+item.getHandle()+", Bundle "+bn.getName()+" allows READ by "+
                          ((rp.getEPersonID() < 0) ? "Group "+rp.getGroup().getName() :
                                                      "EPerson "+rp.getEPerson().getFullName()));
                    }
                }

                for (Bitstream bs : bn.getBitstreams())
                {
                    for (ResourcePolicy rp : AuthorizeManager.getPoliciesActionFilter(context, bs, Constants.READ))
                    {
                        System.out.println("CHECK WARNING: Item "+item.getHandle()+", Bitstream "+bs.getName()+" (in Bundle "+bn.getName()+") allows READ by "+
                          ((rp.getEPersonID() < 0) ? "Group "+rp.getGroup().getName() :
                                                      "EPerson "+rp.getEPerson().getFullName()));
                    }
                }
            }
        }
    }
}
