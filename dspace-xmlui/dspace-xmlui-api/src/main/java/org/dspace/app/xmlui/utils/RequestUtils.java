/*
 * RequestUtils.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
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
	 * have multiple values this method will check for fieldName + "_n" untill
	 * it does not find any more values. The result is a list of all values 
	 * for a field.
	 * 
	 * If the value has been seleted to be removed then it is removed from 
	 * the list.
	 * 
	 * @param request
	 *            the request containing the form information
	 * @param compositeFieldName
	 * 			The fieldName of the composite field. 
	 * @param componentFieldName
	 * 			The fieldName of the component field
	 * @return a List of Strings
	 */
	public static List<String> getCompositeFieldValues(Request request, String compositeFieldName, String componentFieldName)
	{
		List<String> values = new ArrayList<String>();

		int i = -1;

		// Iterate through the values in the form.
		valueLoop: while (1 == 1)
		{
			i++;
			String value = null;

			// Get the component field's name
			if (i == 0)
				value = request.getParameter(componentFieldName);
			else
				value = request.getParameter(componentFieldName + "_" + i);

			// If this is null then it's the last one.
			if (value == null)
				break valueLoop;


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

			// Only add non blank items to the list
			if (value.length() == 0)
				continue valueLoop;
			
			// Finaly add it to the list
			values.add(value.trim());
		}

		return values;
	}


	/**
	 * Get values from a DRI multi value field, since a field may have multiple 
	 * values this method will check for fieldName + "_n" untill it does not 
	 * find any more values. The result is a list of all values for a field.
	 * 
	 * If the value has been seleted to be removed then it is removed from 
	 * the list.
	 * 
	 * @param request
	 *            the request containing the form information
	 * @param fieldName
	 * 			The fieldName of the composite field. 
	 * @return a List of Strings
	 */
	public static List<String> getFieldValues(Request request, String fieldName)
	{
		List<String> values = new ArrayList<String>();

		int i = -1;

		// Iterate through the values in the form.
		valueLoop: while (1 == 1)
		{
			i++;
			String value = null;

			// Get the component field's name
			if (i == 0)
				value = request.getParameter(fieldName);
			else
				value = request.getParameter(fieldName + "_" + i);

			// If this is null then it's the last one.
			if (value == null)
				break valueLoop;


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

			// Only add non blank items to the list
			if (value.length() == 0)
				continue valueLoop;
			
			// Finaly add it to the list
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

        try
        {
            return Integer.parseInt(val.trim());
        }
        catch (Exception e)
        {
            // Problem with parameter
            return -1;
        }
    }
	
	
}
