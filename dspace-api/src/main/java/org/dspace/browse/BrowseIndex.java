/*
 * BrowseIndex.java
 *
 * Version: $Revision: $
 *
 * Date: $Date:  $
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.core.ConfigurationManager;

/**
 * This class holds all the information about a specifically configured 
 * BrowseIndex.  It is responsible for parsing the configuration, understanding
 * about what sort options are available, and what the names of the database
 * tables that hold all the information are actually called.
 * 
 * @author Richard Jones
 */
public class BrowseIndex
{
	/** the configuration number, as specified in the config */
    private int number;
    
    /** the name of the browse index, as specified in the config */
    private String name;
    
    /** the value of the metadata, as specified in the config */
    private String metadata;
    
    /** the datatype of the index, as specified in the config */
    private String datatype;
    
    /** the display type of the metadata, as specified in the config */
    private String displayType;
    
    /** the message key to use to generate the UI component */
    private String messageKey;
    
    /** a three part array of the metadata bits (e.g. dc.contributor.author) */
    private String[] mdBits;
    
    /** the sort options available for this index */
    private Map sortOptions = new HashMap();
    
    /**
     * Create a new BrowseIndex object using the definition from the configuration,
     * and the number of the configuration option.  The definition should be of
     * the form:
     * 
     * <code>
     * [name]:[metadata]:[data type]:[display type]
     * </code>
     * 
     * [name] is a freetext name for the field
     * [metadata] is the usual format of the metadata such as dc.contributor.author
     * [data type] must be either "title", "date" or "text"
     * [display type] must be either "single" or "full"
     * 
     * @param definition	the configuration definition of this index
     * @param number		the configuration number of this index
     * @throws BrowseException
     */
    public BrowseIndex(String definition, int number)
    	throws BrowseException
    {
        this.number = number;
        
        String rx = "(\\w+):([\\w\\.\\*]+):(\\w+):(\\w+)";
        Pattern pattern = Pattern.compile(rx);
        Matcher matcher = pattern.matcher(definition);
        
        if (!matcher.matches())
        {
            throw new BrowseException("Browse Index configuration is not valid: webui.browse.index." + 
                    number + " = " + definition);
        }
        
        messageKey = name = matcher.group(1);
        metadata = matcher.group(2);
        datatype = matcher.group(3);
        displayType = matcher.group(4);
        
        // now load the sort options
        loadSortOptions();
    }

    /**
	 * @return Returns the datatype.
	 */
	public String getDataType()
	{
		return datatype;
	}

	/**
	 * @param datatype The datatype to set.
	 */
	public void setDataType(String datatype)
	{
		this.datatype = datatype;
	}

	/**
	 * @return Returns the displayType.
	 */
	public String getDisplayType()
	{
		return displayType;
	}

	/**
	 * @param displayType The displayType to set.
	 */
	public void setDisplayType(String displayType)
	{
		this.displayType = displayType;
	}

	/**
	 * @return Returns the mdBits.
	 */
	public String[] getMdBits()
	{
		return mdBits;
	}

	/**
	 * @param mdBits The mdBits to set.
	 */
	public void setMdBits(String[] mdBits)
	{
		this.mdBits = mdBits;
	}

	/**
	 * @return Returns the messageKey.
	 */
	public String getMessageKey()
	{
		return messageKey;
	}

	/**
	 * @param messageKey The messageKey to set.
	 */
	public void setMessageKey(String messageKey)
	{
		this.messageKey = messageKey;
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
	 * @param sortOptions The sortOptions to set.
	 */
	public void setSortOptions(Map sortOptions)
	{
		this.sortOptions = sortOptions;
	}

	/**
	 * Populate the internal array containing the bits of metadata, for
	 * ease of use later
	 */
	public void generateMdBits()
    {
    	try
    	{
    		mdBits = interpretField(metadata, null);
    	}
    	catch(IOException e)
    	{
    		// it's not obvious what we really ought to do here
    		//log.error("caught exception: ", e);
    	}
    }
    
	/**
	 * Get the name of the sequence that will be used in the given circumnstances
	 * 
	 * @param isDistinct	is a distinct table
	 * @param isMap			is a map table
	 * @return				the name of the sequence
	 */
    public String getSequenceName(boolean isDistinct, boolean isMap)
    {
    	return BrowseIndex.getSequenceName(this.number, isDistinct, isMap);
    }
    
    /**
     * Get the name of the sequence that will be used in the given circumstances
     * 
     * @param number		the index configuration number
     * @param isDistinct	is a distinct table
     * @param isMap			is a map table
     * @return				the name of the sequence
     */
    public static String getSequenceName(int number, boolean isDistinct, boolean isMap)
    {
    	String baseName = "bi_" + number;
    	
    	if (isDistinct)
    	{
    		baseName = baseName + "_dis";
    	}
    	else if (isMap)
    	{
    		baseName = baseName + "_dmap";
    	}
    	
    	baseName = baseName + "_seq";
    	
    	return baseName;
    }
    
    /**
     * Get the name of the table for the given set of circumstances
     * 
     * @param number		the index configuration number
     * @param isCommunity	whether this is a community constrained index (view)
     * @param isCollection	whether this is a collection constrainted index (view)
     * @param isDistinct	whether this is a distinct table
     * @param isMap			whether this is a distinct map table
     * @return				the name of the table
     */
    public static String getTableName(int number, boolean isCommunity, boolean isCollection, boolean isDistinct, boolean isMap)
    {
    	String baseName = "bi_" + number;
    	
    	// isDistinct is meaningless in relation to isCommunity and isCollection
    	// so we bounce that back first, ignoring other arguments
    	if (isDistinct)
    	{
    		return baseName + "_dis";
    	}
    	
    	// isCommunity and isCollection are mutually exclusive
    	if (isCommunity)
    	{
    		baseName = baseName + "_com"; 
    	}
    	else if (isCollection)
    	{
    		baseName = baseName + "_col";
    	}
    	
    	// isMap is additive to isCommunity and isCollection
    	if (isMap)
    	{
    		baseName = baseName + "_dmap";
    	}
    	
    	return baseName;
    }
    
    /**
     * Get the name of the table in the given circumstances
     * 
     * @param isCommunity	whether this is a community constrained index (view)
     * @param isCollection	whether this is a collection constrainted index (view)
     * @param isDistinct	whether this is a distinct table
     * @param isMap			whether this is a distinct map table
     * @return				the name of the table
     */
    public String getTableName(boolean isCommunity, boolean isCollection, boolean isDistinct, boolean isMap)
    {
    	return BrowseIndex.getTableName(number, isCommunity, isCollection, isDistinct, isMap);
    }
    
    /**
     * Get the name of the table in the given circumstances.  This is the same as calling
     * 
     * <code>
     * getTableName(isCommunity, isCollection, false, false);
     * </code>
     * 
     * @param isCommunity	whether this is a community constrained index (view)
     * @param isCollection	whether this is a collection constrainted index (view)
     * @return				the name of the table
     */
    public String getTableName(boolean isCommunity, boolean isCollection)
    {
        return getTableName(isCommunity, isCollection, false, false);
    }
    
    /**
     * Get the default index table name.  This is the same as calling
     * 
     * <code>
     * getTableName(false, false, false, false);
     * </code>
     * 
     * @return
     */
    public String getTableName()
    {
        return getTableName(false, false, false, false);
    }
    
    /**
     * Get the table name for the given set of circumstances
     * 
     * This is the same as calling:
     * 
     * <code>
     * getTableName(isCommunity, isCollection, isDistinct, false);
     * </code>
     * 
     * @param isDistinct	is this a distinct table
     * @param isCommunity
     * @param isCollection
     * @return
     */
    public String getTableName(boolean isDistinct, boolean isCommunity, boolean isCollection)
    {
    	return getTableName(isCommunity, isCollection, isDistinct, false);
    }
    
    /**
     * Get the name of the distinct map table for the given set of circumstances.  This
     * is the same as calling
     * 
     * <code>
     * getTableName(isCommunity, isCollection, false, true);
     * </code>
     * 
     * @param isCommunity
     * @param isCollection
     * @return	the map table name
     */
    public String getMapName(boolean isCommunity, boolean isCollection)
    {
    	return getTableName(isCommunity, isCollection, false, true);
    }
    
    /**
     * Get the default name of the distinct map table.  This is the same as calling
     * 
     * <code>
     * getTableName(false, false, false, true);
     * </code>
     * 
     * @return
     */
    public String getMapName()
    {
    	return getTableName(false, false, false, true);
    }
    
    /**
     * Get the name of the colum that is used to store the default value column
     * 
     * @return	the name of the value column
     */
    public String getValueColumn()
    {
        if (!isDate())
        {
            return "sort_text_value";
        }
        else
        {
            return "text_value";
        }
    }
    
    /**
     * Get the name of the primary key index column
     * 
     * @return	the name of the primary key index column
     */
    public String getIndexColumn()
    {
        return "id";
    }
    
    /**
     * Is this browse index type for a title?
     * 
     * @return	true if title type, false if not
     */
    public boolean isTitle()
    {
        return "title".equals(datatype);
    }
    
    /**
     * Is the browse index type for a date?
     * 
     * @return	true if date type, false if not
     */
    public boolean isDate()
    {
        return "date".equals(datatype);
    }
    
    /**
     * Is the browse index type for a plain text type?
     * 
     * @return	true if plain text type, false if not
     */
    public boolean isText()
    {
        return "text".equals(datatype);
    }
    
    /**
     * Is the browse index of display type single?
     * 
     * @return true if singe, false if not
     */
    public boolean isSingle()
    {
        return "single".equals(displayType);
    }
    
    /**
     * Is the browse index of display type full?
     * 
     * @return	true if full, false if not
     */
    public boolean isFull()
    {
        return "full".equals(displayType);
    }
    
    /**
     * @return	the SortOptions object for this index
     */
    public Map getSortOptions()
    {
    	return sortOptions;
    }
    
    /**
     * For the specific browse index, load the sort options from
     * configuration
     * 
     * @throws BrowseException
     */
    public void loadSortOptions()
    	throws BrowseException
    {
    	Enumeration en = ConfigurationManager.propertyNames();
        
        ArrayList browseIndices = new ArrayList();
        
        String rx = "webui\\.browse\\.sort-option\\.(\\d+)";
        Pattern pattern = Pattern.compile(rx);
        
        while (en.hasMoreElements())
        {
            String property = (String) en.nextElement();
            Matcher matcher = pattern.matcher(property);
            if (matcher.matches())
            {
                int number = Integer.parseInt(matcher.group(1));
                String option = ConfigurationManager.getProperty(property);
                SortOption so = new SortOption(number, option);
                sortOptions.put(new Integer(number), so);
            }
        }
    }
    
    /**
     * @deprecated
     * @return
     * @throws BrowseException
     */
    public static String[] tables()
    	throws BrowseException
    {
        BrowseIndex[] bis = getBrowseIndices();
        ArrayList tables = new ArrayList();
        for (int i = 0; i < bis.length; i++)
        {
            String tableName = bis[i].getTableName();
            tables.add(tableName);
        }
        
        // FIXME: this complies with the old BrowseTables method, but I'm
        // not really sure why it's here
        tables.add("Communities2Item");
        
        String[] returnTables = new String[tables.size()];
        returnTables = (String[]) tables.toArray((String[]) returnTables);
        
        return returnTables;
    }
    
    /**
     * @deprecated
     * @return
     * @throws BrowseException
     */
    public static Map getBrowseIndicesMap()
    	throws BrowseException
    {
    	Map map = new HashMap();
    	BrowseIndex[] bis = getBrowseIndices();
    	for (int i = 0 ; i < bis.length; i++)
    	{
    		map.put(new Integer(bis[i].getNumber()), bis[i]);
    	}
    	return map;
    }
    
    /**
     * Get an array of all the browse indices for the current configuration
     * 
     * @return	an array of all the current browse indices
     * @throws BrowseException
     */
    public static BrowseIndex[] getBrowseIndices()
    	throws BrowseException
    {
        Enumeration en = ConfigurationManager.propertyNames();
        
        ArrayList browseIndices = new ArrayList();
        
        String rx = "webui\\.browse\\.index\\.(\\d+)";
        Pattern pattern = Pattern.compile(rx);
        
        while (en.hasMoreElements())
        {
            String property = (String) en.nextElement();
            Matcher matcher = pattern.matcher(property);
            if (matcher.matches())
            {
                int number = Integer.parseInt(matcher.group(1));
                String definition = ConfigurationManager.getProperty(property);
                BrowseIndex bi = new BrowseIndex(definition, number);
                browseIndices.add(bi);
            }
        }
        
        BrowseIndex[] bis = new BrowseIndex[browseIndices.size()];
        bis = (BrowseIndex[]) browseIndices.toArray((BrowseIndex[]) bis);
        
        return bis;
    }
    
    /**
     * Get the browse index from configuration with the specified name.  The
     * name is the first part of the browse configuration
     * 
     * @param name		the name to retrieve
     * @return			the specified browse index
     * @throws BrowseException
     */
    public static BrowseIndex getBrowseIndex(String name)
    	throws BrowseException
    {
        Enumeration en = ConfigurationManager.propertyNames();
        
        ArrayList browseIndices = new ArrayList();
        
        String rx = "webui\\.browse\\.index\\.(\\d+)";
        Pattern pattern = Pattern.compile(rx);
        
        while (en.hasMoreElements())
        {
            String property = (String) en.nextElement();
            Matcher matcher = pattern.matcher(property);
            if (matcher.matches())
            {
                int number = Integer.parseInt(matcher.group(1));
                String definition = ConfigurationManager.getProperty(property);
                BrowseIndex bi = new BrowseIndex(definition, number);
                if (bi.getName().equals(name))
                {
                    return bi;
                }
            }
        }
         
        return null;
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
