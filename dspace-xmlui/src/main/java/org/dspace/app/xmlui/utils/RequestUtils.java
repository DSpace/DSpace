/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.cocoon.environment.Request;

/**
 * General utilities for common operations on Request objects.
 * 
 * @author Scott Phillips
 */

public class RequestUtils {

	
	/**
	 * Get values for a field from a DRI multi value field, since a field may
	 * have multiple values this method will check for fieldName + "_n" until
	 * it does not find any more values. The result is a list of all values 
	 * for a field.
	 * 
	 * If the value has been selected to be removed then it is removed from 
	 * the list.
	 * 
	 * @param request
	 * 			The request containing the form information
	 * @param compositeFieldName
	 * 			The fieldName of the composite field 
	 * @param componentFieldName
	 * 			The fieldName of the component field
	 * @return a List of Strings
	 */
	public static List<String> getCompositeFieldValues(Request request, String compositeFieldName, String componentFieldName)
	{
		List<String> values = new ArrayList<String>();

		int i = -1;

		// Iterate through the values in the form.
		valueLoop: while (true)
		{
			i++;
			String value = null;

			// Get the component field's name
			if (i == 0)
            {
                value = request.getParameter(componentFieldName);
            }
			else
            {
                value = request.getParameter(componentFieldName + "_" + i);
            }

			// If this is null then it's the last one.
			if (value == null)
            {
                break valueLoop;
            }


			// Check to make sure that this value is not selected to be removed.
			String[] selected = request.getParameterValues(compositeFieldName + "_selected");

			if (selected != null)
			{
				for (String select : selected)
				{
					if (select.equals(compositeFieldName + "_" + i))
					{
						// Found, the user has requested that this value be deleted.
						continue valueLoop;
					}
				}
			}

			// Only add non-blank items to the list
			if (value.length() == 0)
            {
                continue valueLoop;
            }
			
			// Finally, add it to the list
			values.add(value.trim());
		}

		return values;
	}


	/**
	 * Get values from a DRI multi value field, since a field may have multiple 
	 * values this method will check for fieldName + "_n" until it does not 
	 * find any more values. The result is a list of all values for a field.
	 * 
	 * If the value has been selected to be removed then it is removed from 
	 * the list.
	 * 
	 * @param request
	 * 			The request containing the form information
	 * @param fieldName
	 * 			The fieldName of the composite field
	 * @return a List of Strings
	 */
	public static List<String> getFieldValues(Request request, String fieldName)
	{
		List<String> values = new ArrayList<String>();

		int i = -1;

		// Iterate through the values in the form.
		valueLoop: while (true)
		{
			i++;
			String value = null;

			// Get the component field's name
			if (i == 0)
            {
                value = request.getParameter(fieldName);
            }
			else
            {
                value = request.getParameter(fieldName + "_" + i);
            }

			// If this is null then it's the last one.
			if (value == null)
            {
                break valueLoop;
            }


			// Check to make sure that this value is not selected to be removed.
			String[] selected = request.getParameterValues(fieldName + "_selected");

			if (selected != null)
			{
				for (String select : selected)
				{
					if (select.equals(fieldName + "_" + i))
					{
						// Found, the user has requested that this value be deleted.
						continue valueLoop;
					}
				}
			}

			// Only add non-blank items to the list
			if (value.length() == 0)
            {
                continue valueLoop;
            }
			
			// Finally, add it to the list
			values.add(value.trim());
		}

		return values;
	}
	
    /**
     * Obtain a parameter from the given request as an int. <code>-1</code> is
     * returned if the parameter is garbled or does not exist.
     * 
     * @param request
     *            the HTTP request
     * @param param
     *            the name of the parameter
     * 
     * @return the integer value of the parameter, or -1
     */
    public static int getIntParameter(Request request, String param)
    {
        String val = request.getParameter(param);
		if (val != null)
		{
			try
			{
				return Integer.parseInt(val.trim());
			}
			catch (Exception e)
			{
				// Problem with parameter -- ignore
			}
		}
		return -1;
    }
	
	
}
