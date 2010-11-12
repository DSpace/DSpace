/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private Map<String, ArrayList> items;

    /**
     * Create a new CSV line
     *
     * @param id The item ID of the line
     */
    public DSpaceCSVLine(int id)
    {
        // Store the ID + separator, and initialise the hashtable
        this.id = id;
        items = new HashMap<String, ArrayList>();
    }

    /**
     * Create a new CSV line for a new item
     */
    public DSpaceCSVLine()
    {
        // Set the ID to be -1, and initialise the hashtable
        this.id = -1;
        this.items = new HashMap<String, ArrayList>();
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
    public List<String> get(String key)
    {
        // Return any relevant values
        return items.get(key);
    }

    /**
     * Get all the metadata keys that are represented in this line
     *
     * @return An enumeration of all the keys
     */
    public Set<String> keys()
    {
        // Return the keys
        return items.keySet();
    }

    /**
     * Write this line out as a CSV formatted string, in the order given by the headings provided
     *
     * @param headings The headings which define the order the elements must be presented in
     * @return The CSV formatted String
     */
    protected String toCSV(List<String> headings)
    {
        StringBuilder bits = new StringBuilder();

        // Add the id
        bits.append("\"").append(id).append("\"").append(DSpaceCSV.fieldSeparator);
        bits.append(valueToCSV(items.get("collection"))).append(DSpaceCSV.fieldSeparator);

        // Add the rest of the elements
        Iterator<String> i = headings.iterator();
        String key;
        while (i.hasNext())
        {
            key = i.next();
            if ((items.get(key) != null) && (!"collection".equals(key)))
            {
                bits.append(valueToCSV(items.get(key)));
            }

            if (i.hasNext())
            {
                bits.append(DSpaceCSV.fieldSeparator);
            }
        }
        return bits.toString();
    }

    /**
     * Internal method to create a CSV formatted String joining a given set of elements
     *
     * @param values The values to create the string from
     * @return The line as a CSV formatted String
     */
    private String valueToCSV(List<String> values)
    {
        // Check there is some content
        if (values == null)
        {
            return "";
        }

        // Get on with the work
        if (values.size() == 1)
        {
            return values.get(0);
        }

        // Concatenate any fields together
        StringBuilder str = new StringBuilder();

        Iterator i = values.iterator();
        while (i.hasNext())
        {
            str.append(i.next());
            if (i.hasNext())
            {
                str.append(DSpaceCSV.valueSeparator);
            }
        }

        // Replace internal quotes with two sets of quotes
        return "\"" + str.toString().replaceAll("\"", "\"\"") + "\"";
    }
}

