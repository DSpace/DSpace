/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.dspace.eperson.EPerson;

/**
 * Class for generating standard log header
 * 
 * @author David Stuve
 * @author Robert Tansley
 * @version $Revision$
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
     * If any string within the log line contains a field separator (:) they need to be escaped so as the 
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
