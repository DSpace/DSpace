/*
 * LogManager.java
 *
 * Version: $Revision: 1.8 $
 *
 * Date: $Date: 2004/12/22 17:48:45 $
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
package org.dspace.core;

import org.dspace.eperson.EPerson;

/**
 * Class for generating standard log header
 * 
 * @author David Stuve
 * @author Robert Tansley
 * @version $Revision: 1.8 $
 */
public class LogManager
{
    /**
     * Generate the log header
     * 
     * @param context
     *            the current Context - safe to pass in <code>null</code>
     * @param action
     *            string describing the action
     * @param extrainfo
     *            string with extra information, like parameters
     * 
     * @return the filled out log header
     */
    public static String getHeader(Context context, String action,
            String extrainfo)
    {
        String email = "anonymous";
        String contextExtraInfo;

        if (context != null)
        {
            EPerson e = context.getCurrentUser();

            if (e != null)
            {
                email = e.getEmail();
            }

            contextExtraInfo = context.getExtraLogInfo();
        }
        else
        {
            contextExtraInfo = "no_context";
        }

        String result = new String(email + ":" + contextExtraInfo + ":"
                + action + ":" + extrainfo);

        return result;
    }
}