/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.dspace.authority.AuthorityValue;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.service.AuthorityValueService;

import java.io.Serializable;
import java.util.*;

/**
 * Utility class to store a line from a CSV file
 *
 * @author Stuart Lewis
 */
public class DSpaceCSVLine implements Serializable
{
    /** The item id of the item represented by this line. -1 is for a new item */
    private final UUID id;

    /** The elements in this line in a hashtable, keyed by the metadata type */
    private final Map<String, ArrayList> items;

    protected transient final AuthorityValueService authorityValueService
            = AuthorityServiceFactory.getInstance().getAuthorityValueService();

    /** ensuring that the order-sensible columns of the csv are processed in the correct order */
    private transient final Comparator<? super String> headerComparator = new Comparator<String>() {
        @Override
        public int compare(String md1, String md2) {
            // The metadata coming from an external source should be processed after the others
            AuthorityValue source1 = authorityValueService.getAuthorityValueType(md1);
            AuthorityValue source2 = authorityValueService.getAuthorityValueType(md2);

            int compare;
            if (source1 == null && source2 != null) {
                compare = -1;
            }
            else if (source1 != null && source2 == null) {
                compare = 1;
            } else {
                // the order of the rest does not matter
                compare = md1.compareTo(md2);
            }
            return compare;
        }
    };

    /**
     * Create a new CSV line
     *
     * @param itemId The item ID of the line
     */
    public DSpaceCSVLine(UUID itemId)
    {
        // Store the ID + separator, and initialise the hashtable
        this.id = itemId;
        items = new TreeMap<>(headerComparator);
//        this.items = new HashMap<String, ArrayList>();
    }

    /**
     * Create a new CSV line for a new item
     */
    public DSpaceCSVLine()
    {
        // Set the ID to be null, and initialise the hashtable
        this.id = null;
        this.items = new TreeMap<>(headerComparator);
    }

    /**
     * Get the item ID that this line represents
     *
     * @return The item ID
     */
    public UUID getID()
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
     * Get any action associated with this line
     *
     * @return The action (may be blank, 'withdraw', 'reinstate' or 'delete')
     */
    public String getAction()
    {
        if (items.containsKey("action")) {
            ArrayList actions = items.get("action");
            if (actions.size() > 0) {
                return ((String)actions.get(0)).trim();
            }
        }
        return "";
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
     * @param fieldSeparator separator between metadata fields
     * @param valueSeparator separator between metadata values (within a field)
     * @return The CSV formatted String
     */
    protected String toCSV(List<String> headings, String fieldSeparator, String valueSeparator)
    {
        StringBuilder bits = new StringBuilder();

        // Add the id
        bits.append("\"").append(id).append("\"").append(fieldSeparator);
        bits.append(valueToCSV(items.get("collection"),valueSeparator));

        // Add the rest of the elements
        for (String heading : headings)
        {
            bits.append(fieldSeparator);
            List<String> values = items.get(heading);
            if (values != null && !"collection".equals(heading))
            {
                bits.append(valueToCSV(values, valueSeparator));
            }
        }

        return bits.toString();
    }

    /**
     * Internal method to create a CSV formatted String joining a given set of elements
     *
     * @param values The values to create the string from
     * @param valueSeparator value separator
     * @return The line as a CSV formatted String
     */
    protected String valueToCSV(List<String> values, String valueSeparator)
    {
        // Check there is some content
        if (values == null)
        {
            return "";
        }

        // Get on with the work
        String s;
        if (values.size() == 1)
        {
            s = values.get(0);
        }
        else
        {
            // Concatenate any fields together
            StringBuilder str = new StringBuilder();

            for (String value : values)
            {
                if (str.length() > 0)
                {
                    str.append(valueSeparator);
                }

                str.append(value);
            }

            s = str.toString();
        }

        // Replace internal quotes with two sets of quotes
        return "\"" + s.replaceAll("\"", "\"\"") + "\"";
    }
}

