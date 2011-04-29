/*
 * LogManager.java
 *
 * Version: $Revision: 3762 $
 *
 * Date: $Date: 2009-05-07 00:36:47 -0400 (Thu, 07 May 2009) $
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
 * - Neither the name of the DSpace Foundation nor the names of its
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
 * @version $Revision: 3762 $
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
        

        StringBuilder result = new StringBuilder();
        // Escape everthing but the extra context info because for some crazy reason two fields
        // are generated inside this entry one for the session id, and another for the ip 
        // address. Everything else should be escaped.
        result.append(escapeLogField(email)).append(":").append(contextExtraInfo).append(":").append(escapeLogField(action)).append(":").append(escapeLogField(extrainfo));
        return result.toString();
    }
    
    
    /**
     * If any string within the log line contains a field seperator (:) they need to be escaped so as the 
     * line may be parsed and analysed later. This method will escape a log field.
     * 
     * Single slashes and colons will be escaped so that colons no longer appear in the logs
     * 
     * @param field The unescaped log field
     * @return An escaped log field
     */
    public static String escapeLogField(String field)
    {
        if (field != null)
        {
        	field = field.replaceAll("\\\\", "\\\\\\\\;");
        	field = field.replaceAll(":","\\\\colon;");
        }
        return field;
    }
    
    /**
     * Unescape a log field.
     * 
     * @param field The escaped log field
     * @return the original log field
     */
    public static String unescapeLogField(String field)
    {
    	
    	if (field != null)
        {
        	field = field.replaceAll("\\\\colon;", ":");
        	field = field.replaceAll("\\\\\\\\;","\\\\");
        }
        return field;
    }
}
