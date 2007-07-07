/*
 * AuthenticatedSelector.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/04/06 15:15:46 $
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

package org.dspace.app.xmlui.aspect.general;

import java.util.Map;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * This simple selector operates on the authenticated DSpace user and selects
 * between two levels of access.
 * 
 * <map:selector name="AuthenticatedSelector" src="org.dspace.app.xmlui.AuthenticatedSelector"/>
 * 
 * 
 * 
 * <map:select type="AuthenticatedSelector"> 
 *   <map:when test="administrator">
 *     ...
 *   </map:when> 
 *   <map:when test="eperson"> 
 *     ... 
 *   </map:when> 
 *   <map:otherwise> 
 *     ...
 *   </map:otherwise> 
 * </map:select>
 * 
 * There are only two defined test expressions: "administrator" and "eperson".
 * Remember an administrator is also an eperson so if you need to check for
 * administrators distinct from epersons that select must come first.
 * 
 * @author Scott Phillips
 */

public class AuthenticatedSelector extends AbstractLogEnabled implements
        Selector
{

    private static Logger log = Logger.getLogger(AuthenticatedSelector.class);

    /** Test expressiots */
    public static final String EPERSON = "eperson";

    public static final String ADMINISTRATOR = "administrator";

    /**
     * Determine if the authenticated eperson matches the given expression.
     */
    public boolean select(String expression, Map objectModel,
            Parameters parameters)
    {
        try
        {
            Context context = ContextUtil.obtainContext(objectModel);

            EPerson eperson = context.getCurrentUser();

            if (eperson == null)
                // No one is authenticated.
                return false;

            if (EPERSON.equals(expression))
            {
                // At least someone is authenticated.
                return true;
            }
            else if (ADMINISTRATOR.equals(expression))
            {
                // Is this eperson an administrator?
                return AuthorizeManager.isAdmin(context);
            }

            // Otherwise return false;
            return false;

        }
        catch (Exception e)
        {
            // Log it and returned no match.
            log.error("Error selecting based on authentication status: "
                    + e.getMessage());

            return false;
        }
    }

}
