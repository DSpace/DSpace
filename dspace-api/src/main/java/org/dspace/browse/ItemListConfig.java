/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.lang.ArrayUtils;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class to mediate with the item list configuration
 * 
 * @author Richard Jones
 *
 */
public class ItemListConfig
{
	/** a map of column number to metadata value */
	private Map<Integer, String[]> metadata = new HashMap<Integer, String[]>();
	
	/** a map of column number to data type */
	private Map<Integer, Integer> types = new HashMap<Integer, Integer>();
	
	/** constant for a DATE column */
	private static final int DATE = 1;
	
	/** constant for a TEXT column */
	private static final int TEXT = 2;

        private final transient ConfigurationService configurationService
             = DSpaceServicesFactory.getInstance().getConfigurationService();
	
	/**
	 * Create a new instance of the Item list configuration.  This loads
	 * all the required information from configuration
	 * 
	 * @throws BrowseException if count error
	 */
	public ItemListConfig()
		throws BrowseException
	{
		try
		{
			String[] browseFields  = configurationService.getArrayProperty("webui.itemlist.columns");
			
			if (ArrayUtils.isEmpty(browseFields))
			{
				throw new BrowseException("There is no configuration for webui.itemlist.columns");
			}
			
			// parse the config
			int i = 1;
			for(String token : browseFields)
			{
				Integer key = Integer.valueOf(i);

				// find out if the field is a date
				if (token.indexOf("(date)") > 0)
				{
					token = token.replaceAll("\\(date\\)", "");
					types.put(key, Integer.valueOf(ItemListConfig.DATE));
				}
				else
				{
					types.put(key, Integer.valueOf(ItemListConfig.TEXT));
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
	 * What metadata is to go in the given column number?
	 * 
	 * @param col column
         * @return array of metadata
	 */
	public String[] getMetadata(int col)
	{
		return metadata.get(Integer.valueOf(col));
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
     * @throws IOException if IO error
     */
    public final String[] interpretField(String mfield, String init)
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
