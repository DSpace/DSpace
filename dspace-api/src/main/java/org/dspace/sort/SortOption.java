/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sort;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

/**
 * Class to mediate with the sort configuration
 * 
 * @author Richard Jones
 *
 */
public class SortOption
{
    private static final Logger log = Logger.getLogger(SortOption.class);

    public static final String ASCENDING  = "ASC";
    public static final String DESCENDING = "DESC";

    /** the sort configuration number */
	private int number;
	
	/** the name of the sort */
	private String name;
	
	/** the metadata field to sort on */
	private String metadata;
	
	/** the type of data we are sorting by */
	private String type;
	
	/** the metadata broken down into bits for convenience */
	private String[] mdBits;

    /** should the sort option be visible for user selection */
    private boolean visible;

    /** the sort options available for this index */
    private static Set<SortOption> sortOptionsSet = null;
    static {
        try
        {
            Set<SortOption> newSortOptionsSet = new TreeSet<SortOption>(new Comparator<SortOption>() {
	            @Override
	            public int compare(SortOption sortOption, SortOption sortOption1) {
		            return Integer.valueOf(sortOption.getNumber()).compareTo(Integer.valueOf(sortOption1.getNumber()));
	            }
            });
            int idx = 1;
            String option;

            while ( ((option = ConfigurationManager.getProperty("webui.itemlist.sort-option." + idx))) != null)
            {
                SortOption so = new SortOption(idx, option);
                newSortOptionsSet.add(so);
                idx++;
            }

            SortOption.sortOptionsSet = newSortOptionsSet;
        }
        catch (SortException se)
        {
            log.fatal("Unable to load SortOptions", se);
        }
    }
	/**
	 * Construct a new SortOption object with the given parameters
	 * 
	 * @param number
	 * @param name
	 * @param md
	 * @param type
	 * @throws SortException if sort error
	 */
	public SortOption(int number, String name, String md, String type)
		throws SortException
	{
		this.name = name;
		this.type = type;
		this.metadata = md;
		this.number = number;
        this.visible = true;
        generateMdBits();
	}

	/**
	 * Construct a new SortOption object using the definition from the configuration
	 * 
	 * @param number
	 * @param definition
	 * @throws SortException if sort error
	 */
	public SortOption(int number, String definition)
		throws SortException
	{
		this.number = number;
		
		String rx = "(\\w+):([\\w\\.\\*]+):(\\w+):?(\\w*)";
        Pattern pattern = Pattern.compile(rx);
        Matcher matcher = pattern.matcher(definition);
        
        if (!matcher.matches())
        {
            throw new SortException("Sort Order configuration is not valid: webui.itemlist.sort-option." +
                    number + " = " + definition);
        }
        
        name = matcher.group(1);
        metadata = matcher.group(2);
        type = matcher.group(3);

        // If the option is configured to be hidden, then set the visible flag to false
        // otherwise, flag it as visible (true)
        if (matcher.groupCount() > 3 && "hide".equalsIgnoreCase(matcher.group(4)))
        {
            visible = false;
        }
        else
        {
            visible = true;
        }

        generateMdBits();
	}

    /**
	 * @return Returns the metadata.
	 */
	public String getMetadata()
	{
		return metadata;
	}

	/**
	 * @param metadata The metadata to set.
	 */
	public void setMetadata(String metadata)
	{
		this.metadata = metadata;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return Returns the type.
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * @param type The type to set.
	 */
	public void setType(String type)
	{
		this.type = type;
	}

	/**
	 * @return Returns the number.
	 */
	public int getNumber()
	{
		return number;
	}

	/**
	 * @param number The number to set.
	 */
	public void setNumber(int number)
	{
		this.number = number;
	}

    /**
     * Should this sort option be made visible in the UI
     * @return true if visible, false otherwise
     */
    public boolean isVisible()
    {
        return visible;
    }

    /**
	 * @return	a 3 element array of the metadata bits
	 */
	public String[] getMdBits()
    {
    	return (String[]) ArrayUtils.clone(mdBits);
    }
    
	/**
	 * Tell the class to generate the metadata bits
	 * 
	 * @throws SortException if sort error
	 */
    private void generateMdBits()
    	throws SortException
    {
    	try
    	{
    		mdBits = interpretField(metadata, null);
    	}
    	catch(IOException e)
    	{
    		throw new SortException(e);
    	}
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
    
    /**
     * Is this a date field?
     */
    public boolean isDate()
    {
    	if ("date".equals(type))
        {
            return true;
        }

    	return false;
    }
    
    /**
     * Is the default sort option?
     */
    public boolean isDefault()
    {
    	if (number == 0)
    	{
    		return true;
    	}
    	return false;
    }

    /**
     * Return all the configured sort options.
     * @throws SortException if sort error
     */
    public static Set<SortOption> getSortOptions() throws SortException
    {
        if (SortOption.sortOptionsSet == null)
        {
            throw new SortException("Sort options not loaded");
        }

        return SortOption.sortOptionsSet;
    }
    
    /**
     * Get the defined sort option by number (.1, .2, etc).
     * @param number
     * @throws SortException if sort error
     */
    public static SortOption getSortOption(int number) throws SortException
    {
        for (SortOption so : SortOption.getSortOptions())
        {
            if (so.getNumber() == number)
            {
                return so;
            }
        }
        
        return null;
    }
    
    /**
     * Get the default sort option - initially, just the first one defined.
     * @throws SortException if sort error
     */
    public static SortOption getDefaultSortOption() throws SortException
    {
        for (SortOption so : getSortOptions())
        {
            return so;
        }
        
        return null;
    }
}
