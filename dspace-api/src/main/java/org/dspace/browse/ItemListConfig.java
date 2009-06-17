/*
 * ItemListConfig.java
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.browse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.dspace.core.ConfigurationManager;

/**
 * Class to mediate with the item list configuration
 * 
 * @author Richard Jones
 *
 */
public class ItemListConfig
{
	/** a map of column number to metadata value */
	private Map metadata = new HashMap();
	
	/** a map of column number to data type */
	private Map types = new HashMap();
	
	/** constant for a DATE column */
	private static final int DATE = 1;
	
	/** constant for a TEXT column */
	private static final int TEXT = 2;
	
	/**
	 * Create a new instance of the Item list configuration.  This loads
	 * all the required information from configuration
	 * 
	 * @throws BrowseException
	 */
	public ItemListConfig()
		throws BrowseException
	{
		try
		{
			String configLine = ConfigurationManager.getProperty("webui.itemlist.columns");
			
			if (configLine == null || "".equals(configLine))
			{
				throw new BrowseException("There is no configuration for webui.itemlist.columns");
			}
			
			// parse the config
			StringTokenizer st = new StringTokenizer(configLine, ",");
			int i = 1;
			while (st.hasMoreTokens())
			{
				Integer key = new Integer(i);
				String token = st.nextToken();
				
				// find out if the field is a date
				if (token.indexOf("(date)") > 0)
				{
					token = token.replaceAll("\\(date\\)", "");
					types.put(key, new Integer(ItemListConfig.DATE));
				}
				else
				{
					types.put(key, new Integer(ItemListConfig.TEXT));
				}
				
				String[] mdBits = interpretField(token.trim(), null);
				metadata.put(key, mdBits);
				
				// don't forget to increment the key counter
				i++;
			}
		}
		catch (IOException e)
		{
			throw new BrowseException(e);
		}
	}

	/**
	 * how many columns are there?
	 * 
	 * @return	the number of columns
	 */
	public int numCols()
	{
		return metadata.size();
	}
	
	/**
	 * what metadata is to go in the given column number
	 * 
	 * @param col
	 * @return
	 */
	public String[] getMetadata(int col)
	{
		return (String[]) metadata.get(new Integer(col));
	}
	
	/**
     * Take a string representation of a metadata field, and return it as an array.
     * This is just a convenient utility method to basically break the metadata 
     * representation up by its delimiter (.), and stick it in an array, inserting
     * the value of the init parameter when there is no metadata field part.
     * 
     * @param mfield	the string representation of the metadata
     * @param init	the default value of the array elements
     * @return	a three element array with schema, element and qualifier respectively
     */
    public String[] interpretField(String mfield, String init)
    	throws IOException
    {
    	StringTokenizer sta = new StringTokenizer(mfield, ".");
    	String[] field = {init, init, init};
    	
    	int i = 0;
    	while (sta.hasMoreTokens())
    	{
    		field[i++] = sta.nextToken();
    	}
    	
    	// error checks to make sure we have at least a schema and qualifier for both
    	if (field[0] == null || field[1] == null)
    	{
    		throw new IOException("at least a schema and element be " +
    				"specified in configuration.  You supplied: " + mfield);
    	}
    	
    	return field;
    }
}
