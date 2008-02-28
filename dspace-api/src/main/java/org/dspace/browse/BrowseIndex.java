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
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.core.ConfigurationManager;
import org.dspace.sort.SortOption;
import org.dspace.sort.SortException;

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
    /** used for single metadata browse tables for generating the table name */
    private int number;
    
    /** the name of the browse index, as specified in the config */
    private String name;

    /** the SortOption for this index (only valid for item indexes) */
    private SortOption sortOption;

    /** the value of the metadata, as specified in the config */
    private String metadataAll;

    /** the metadata fields, as an array */
    private String[] metadata;

    /** the datatype of the index, as specified in the config */
    private String datatype;
    
    /** the display type of the metadata, as specified in the config */
    private String displayType;
    
    /** base name for tables, sequences */
    private String tableBaseName;
    
    /** a three part array of the metadata bits (e.g. dc.contributor.author) */
    private String[][] mdBits;

    /** default order (asc / desc) for this index */
    private String defaultOrder = SortOption.ASCENDING;

    /** additional 'internal' tables that are always defined */
    private static BrowseIndex itemIndex      = new BrowseIndex("bi_item");
    private static BrowseIndex withdrawnIndex = new BrowseIndex("bi_withdrawn");

    /**
     * Ensure noone else can create these
     */
    private BrowseIndex()
    {
    }
    
    /**
     * Constructor for creating generic / internal index objects
     * @param baseName The base of the table name
     */
    private BrowseIndex(String baseName)
    {
        try
        {
            number = -1;
            tableBaseName = baseName;
            displayType = "item";
            sortOption = SortOption.getDefaultSortOption();
        }
        catch (SortException se)
        {
            // FIXME Exception handling
        }
    }
    
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
    private BrowseIndex(String definition, int number)
    	throws BrowseException
    {
        try
        {
            boolean valid = true;
            this.defaultOrder = SortOption.ASCENDING;
            this.number = number;

            String rx = "(\\w+):(\\w+):([\\w\\.\\*,]+):?(\\w*):?(\\w*)";
            Pattern pattern = Pattern.compile(rx);
            Matcher matcher = pattern.matcher(definition);

            if (matcher.matches())
            {
                name = matcher.group(1);
                displayType = matcher.group(2);

                if (isMetadataIndex())
                {
                    metadataAll = matcher.group(3);
                    datatype = matcher.group(4);

                    if (metadataAll != null)
                        metadata = metadataAll.split(",");

                    if (metadata == null || metadata.length == 0)
                        valid = false;

                    if (datatype == null || datatype.equals(""))
                        valid = false;

                    // If an optional ordering configuration is supplied,
                    // set the defaultOrder appropriately (asc or desc)
                    if (matcher.groupCount() > 4)
                    {
                        String order = matcher.group(5);
                        if (SortOption.DESCENDING.equalsIgnoreCase(order))
                            this.defaultOrder = SortOption.DESCENDING;
                    }

                    tableBaseName = getItemBrowseIndex().tableBaseName;
                }
                else if (isItemIndex())
                {
                    String sortName = matcher.group(3);

                    for (SortOption so : SortOption.getSortOptions())
                    {
                        if (so.getName().equals(sortName))
                            sortOption = so;
                    }

                    if (sortOption == null)
                        valid = false;

                    // If an optional ordering configuration is supplied,
                    // set the defaultOrder appropriately (asc or desc)
                    if (matcher.groupCount() > 3)
                    {
                        String order = matcher.group(4);
                        if (SortOption.DESCENDING.equalsIgnoreCase(order))
                            this.defaultOrder = SortOption.DESCENDING;
                    }

                    tableBaseName = getItemBrowseIndex().tableBaseName;
                }
                else
                {
                    valid = false;
                }
            }
            else
            {
                valid = false;
            }

            if (!valid)
            {
                throw new BrowseException("Browse Index configuration is not valid: webui.browse.index." +
                        number + " = " + definition);
            }
        }
        catch (SortException se)
        {
            throw new BrowseException("Error in SortOptions", se);
        }
    }

    /**
     * @return Default order for this index, null if not specified
     */
    public String getDefaultOrder()
    {
        return defaultOrder;
    }

    /**
	 * @return Returns the datatype.
	 */
	public String getDataType()
	{
        if (sortOption != null)
            return sortOption.getType();

		return datatype;
	}

	/**
	 * @return Returns the displayType.
	 */
	public String getDisplayType()
	{
        return displayType;
	}

    /**
     * @return Returns the number of metadata fields for this index
     */
    public int getMetadataCount()
    {
        if (isMetadataIndex())
            return metadata.length;

        return 0;
    }

    /**
	 * @return Returns the mdBits.
	 */
	public String[] getMdBits(int idx)
	{
	    if (isMetadataIndex())
	        return mdBits[idx];
	    
	    return null;
	}

	/**
	 * @return Returns the metadata.
	 */
	public String getMetadata()
	{
        return metadataAll;
	}

    public String getMetadata(int idx)
    {
        return metadata[idx];
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
//	public void setName(String name)
//	{
//		this.name = name;
//	}
	
	/**
	 * Get the SortOption associated with this index.
	 */
	public SortOption getSortOption()
	{
	    return sortOption;
	}
	
	/**
	 * Populate the internal array containing the bits of metadata, for
	 * ease of use later
	 */
	public void generateMdBits()
    {
    	try
    	{
    	    if (isMetadataIndex())
            {
                mdBits = new String[metadata.length][];
                for (int i = 0; i < metadata.length; i++)
                {
                    mdBits[i] = interpretField(metadata[i], null);
                }
            }
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
        if (isDistinct || isMap)
            return BrowseIndex.getSequenceName(number, isDistinct, isMap);
        
        return BrowseIndex.getSequenceName(tableBaseName, isDistinct, isMap);
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
        return BrowseIndex.getSequenceName(makeTableBaseName(number), isDistinct, isMap);
    }
    
    /**
     * Generate a sequence name from the given base
     * @param baseName
     * @param isDistinct
     * @param isMap
     * @return
     */
    private static String getSequenceName(String baseName, boolean isDistinct, boolean isMap)
    {
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
     * This is provided solely for cleaning the database, where you are
     * trying to create table names that may not be reflected in the current index
     * 
     * @param number		the index configuration number
     * @param isCommunity	whether this is a community constrained index (view)
     * @param isCollection	whether this is a collection constrainted index (view)
     * @param isDistinct	whether this is a distinct table
     * @param isMap			whether this is a distinct map table
     * @return				the name of the table
     * @deprecated 1.5
     */
    public static String getTableName(int number, boolean isCommunity, boolean isCollection, boolean isDistinct, boolean isMap)
    {
        return BrowseIndex.getTableName(makeTableBaseName(number), isCommunity, isCollection, isDistinct, isMap);
    }
    
    /**
     * Generate a table name from the given base
     * @param baseName
     * @param isCommunity
     * @param isCollection
     * @param isDistinct
     * @param isMap
     * @return
     */
    private static String getTableName(String baseName, boolean isCommunity, boolean isCollection, boolean isDistinct, boolean isMap)
    {
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
     * @deprecated 1.5
     */
    public String getTableName(boolean isCommunity, boolean isCollection, boolean isDistinct, boolean isMap)
    {
        if (isDistinct || isMap)
            return BrowseIndex.getTableName(number, isCommunity, isCollection, isDistinct, isMap);
        
        return BrowseIndex.getTableName(tableBaseName, isCommunity, isCollection, isDistinct, isMap);
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
     * @deprecated 1.5
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
     * @deprecated 1.5
     */
    public String getTableName(boolean isDistinct, boolean isCommunity, boolean isCollection)
    {
    	return getTableName(isCommunity, isCollection, isDistinct, false);
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
    public String getMapTableName()
    {
    	return getTableName(false, false, false, true);
    }
    
    /**
     * Get the default name of the distinct table.  This is the same as calling
     *
     * <code>
     * getTableName(false, false, true, false);
     * </code>
     *
     * @return
     */
    public String getDistinctTableName()
    {
    	return getTableName(false, false, true, false);
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
//    public boolean isTitle()
//    {
//        return "title".equals(getDataType());
//    }
    
    /**
     * Is the browse index type for a date?
     * 
     * @return	true if date type, false if not
     */
    public boolean isDate()
    {
        return "date".equals(getDataType());
    }
    
    /**
     * Is the browse index type for a plain text type?
     * 
     * @return	true if plain text type, false if not
     */
//    public boolean isText()
//    {
//        return "text".equals(getDataType());
//    }
    
    /**
     * Is the browse index of display type single?
     * 
     * @return true if singe, false if not
     */
    public boolean isMetadataIndex()
    {
        return "metadata".equals(displayType);
    }
    
    /**
     * Is the browse index of display type full?
     * 
     * @return	true if full, false if not
     */
    public boolean isItemIndex()
    {
        return "item".equals(displayType);
    }
    
    /**
     * Get the field for sorting associated with this index
     * @return
     * @throws BrowseException
     */
    public String getSortField(boolean isSecondLevel) throws BrowseException
    {
        String focusField;
        if (isMetadataIndex() && !isSecondLevel)
        {
            focusField = "sort_value";
        }
        else
        {
            if (sortOption != null)
                focusField = "sort_" + sortOption.getNumber();
            else
                focusField = "sort_1";  // Use the first sort column
        }
        
        return focusField;
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
        String[] returnTables = new String[bis.length];
        for (int i = 0; i < bis.length; i++)
        {
            returnTables[i] = bis[i].getTableName();
        }

        return returnTables;
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
        int idx = 1;
        String definition;
        ArrayList browseIndices = new ArrayList();

        while ( ((definition = ConfigurationManager.getProperty("webui.browse.index." + idx))) != null)
        {
            BrowseIndex bi = new BrowseIndex(definition, idx);
            browseIndices.add(bi);
            idx++;
        }

        BrowseIndex[] bis = new BrowseIndex[browseIndices.size()];
        bis = (BrowseIndex[]) browseIndices.toArray((BrowseIndex[]) bis);

        return bis;
    }

    /**
     * Get the browse index from configuration with the specified name.
     * The name is the first part of the browse configuration
     *
     * @param name		the name to retrieve
     * @return			the specified browse index
     * @throws BrowseException
     */
    public static BrowseIndex getBrowseIndex(String name)
    	throws BrowseException
    {
        for (BrowseIndex bix : BrowseIndex.getBrowseIndices())
        {
            if (bix.getName().equals(name))
                return bix;
        }
         
        return null;
    }
    
    /**
     * Get the configured browse index that is defined to use this sort option
     * 
     * @param so
     * @return
     * @throws BrowseException
     */
    public static BrowseIndex getBrowseIndex(SortOption so) throws BrowseException
    {
        for (BrowseIndex bix : BrowseIndex.getBrowseIndices())
        {
            if (bix.getSortOption() == so)
                return bix;
        }
        
        return null;
    }
    
    /**
     * Get the internally defined browse index for archived items
     * 
     * @return
     */
    public static BrowseIndex getItemBrowseIndex()
    {
        return BrowseIndex.itemIndex;
    }
    
    /**
     * Get the internally defined browse index for withdrawn items
     * @return
     */
    public static BrowseIndex getWithdrawnBrowseIndex()
    {
        return BrowseIndex.withdrawnIndex;
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
    
    /**
     * Does the browse index represent one of the internal item indexes
     * 
     * @param bi
     * @return
     */
    public static boolean isInternalIndex(BrowseIndex bi)
    {
        return (bi == itemIndex || bi == withdrawnIndex);
    }

    /**
     * Does this browse index represent one of the internal item indexes
     * 
     * @return
     */
    public boolean isInternalIndex()
    {
        return (this == itemIndex || this == withdrawnIndex);
    }

    /**
     * Generate a base table name
     * @param number
     * @return
     */
    private static String makeTableBaseName(int number)
    {
        return "bi_" + Integer.toString(number);
    }
}
