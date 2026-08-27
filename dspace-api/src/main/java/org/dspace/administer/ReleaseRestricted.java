/*
 * ReleaseRestricted.java
 * 
 * Version: $Revision: 1.1.1.1 $
 *
 * Date: $Date: 2014/08/12 19:47:51 $
 *
 * Copyright (c) 2003, The University of Edinburgh.  All rights reserved.
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
 * - Neither the name of the University of Edinburgh, or the names of the
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
 * 
 */

package org.dspace.administer;

import org.dspace.app.util.Restrict;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

/**
 * This class checks to see if there are any items that are due to be
 * reinstated into the archive and performs the actions necessary to do so.
 *
 * @author  Richard Jones
 */
public class ReleaseRestricted {
    
    public static void main(String [] argv)
        throws Exception
    {

        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        String usage = "Usage: " + ReleaseRestricted.class.getName() +
            " administrator-email-address";
        
        Context c = null;
        
        try 
        {
            c = new Context();

            //EPerson ep = EPerson.findByEmail(c, argv[0]);
            EPerson ep = ePersonService.findByEmail(c, argv[0]);        

    
            if (ep != null)
            {
                c.setCurrentUser(ep);
            }
            else
            {
                System.err.println("Invalid Email Address, please enter the" +
                                    "address of the system administrator");
            }
        
            // super user
            //c.setIgnoreAuthorization(true);
        
            Item[] items = Restrict.getAvailable(c);
            System.out.println("Number of items to be de-restricted: " 
                            + items.length);
            
            Restrict.release(c, items);
            

            c.commit();
            c.complete();
        }
        catch (ArrayIndexOutOfBoundsException ae)
        {
            System.err.println(usage);

            if (c != null)
            {
                c.abort();
            }

            System.exit(1);
        }
    }
    
}