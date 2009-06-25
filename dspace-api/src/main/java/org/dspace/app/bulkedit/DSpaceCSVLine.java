/*
 * DSpaceCSVLine.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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

package org.dspace.app.bulkedit;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Utility class to store a line from a CSV file
 *
 * @author Stuart Lewis
 */
public class DSpaceCSVLine
{
    /** The item id of the item represented by this line. -1 is for a new item */
    private int id;

    /** The elements in this line in a hashtable, keyed by the metadata type */
    private Hashtable<String, ArrayList> items;

    /**
     * Create a new CSV line
     *
     * @param id The item ID of the line
     */
    public DSpaceCSVLine(int id)
    {
        // Store the ID + separator, and initalise the hashtable
        this.id = id;
        items = new Hashtable<String, ArrayList>();
    }

    /**
     * Create a new CSV line for a new item
     */
    public DSpaceCSVLine()
    {
        // Set the ID to be -1, and initalise the hashtable
        this.id = -1;
        this.items = new Hashtable<String, ArrayList>();
    }

    /**
     * Get the item ID that this line represents
     *
     * @return The item ID
     */
    public int getID()
    {
        // Return the ID
        return id;
    }

    /**
     * Add a new metadata value to this line
     *
     * @param key The metadata key (e.g. dc.contributor.author)
     * @param value The metadata value
     */
    public void add(String key, String value)
    {
        // Create the array list if we need to
        if (items.get(key) == null)
        {
            items.put(key, new ArrayList<String>());
        }

        // Store the item if it is not null
        if (value != null)
        {
            items.get(key).add(value);
        }
    }

    /**
     * Get all the values that match the given metadata key. Will be null if none exist.
     *
     * @param key The metadata key
     * @return All the elements that match
     */
    public ArrayList<String> get(String key)
    {
        // Return any relevant values
        return items.get(key);
    }

    /**
     * Get all the metadata keys that are represented in this line
     *
     * @return An enumeration of all the keys
     */
    public Enumeration<String> keys()
    {
        // Return the keys
        return items.keys();
    }

    /**
     * Write this line out as a CSV formatted string, in the order given by the headings provided
     *
     * @param headings The headings which define the order the elements must be presented in
     * @return The CSV formatted String
     */
    protected String toCSV(ArrayList<String> headings)
    {
        // Add the id
        String bits = "\"" + id + "\"" + DSpaceCSV.fieldSeparator;
        bits += valueToCSV(items.get("collection")) + DSpaceCSV.fieldSeparator;

        // Add the rest of the elements
        Iterator<String> i = headings.iterator();
        String key;
        while (i.hasNext())
        {
            key = i.next();
            if ((items.get(key) != null) && (!"collection".equals(key)))
            {
                bits = bits + valueToCSV(items.get(key));
            }

            if (i.hasNext())
            {
                bits = bits + DSpaceCSV.fieldSeparator;
            }
        }
        return bits;
    }

    /**
     * Internal method to create a CSV formatted String joining a given set of elements
     *
     * @param values The values to create the string from
     * @return The line as a CSV formatted String
     */
    private String valueToCSV(ArrayList<String> values)
    {
        // Concatenate any fields together 
        String s = "";

        // Check there is some content
        if (values == null)
        {
            return s;
        }

        // Get on with the work
        if (values.size() == 1)
        {
            s = values.get(0);
        }
        else
        {
            Iterator i = values.iterator();
            while (i.hasNext())
            {
                s = s + i.next();
                if (i.hasNext())
                {
                    s = s + DSpaceCSV.valueSeparator;
                }
            }
        }

        // Replace internal quotes with two sets of quotes
        s = s.replaceAll("\"", "\"\"");

        // Wrap in quotes
        s = "\"" + s + "\"";

        // Return the csv formatted string
        return s;
    }
}

