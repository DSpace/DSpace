/*
 * DSpaceCSV.java
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

import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;

/**
 * Utility class to read and write CSV files
 *
 * @author Stuart Lewis
 */
public class DSpaceCSV
{
    /** The headings of the CSV file */
    private ArrayList<String> headings;

    /** An array list of CSV lines */
    private ArrayList<DSpaceCSVLine> lines;

    /** A counter of how many CSV lines this object holds */
    private int counter;

    /** The value separator (defaults to double pipe '||') */
    protected static String valueSeparator;

    /** The value separator in an escaped form for using in regexs */
    protected static String escpaedValueSeparator;

    /** The field separator (defaults to comma) */
    protected static String fieldSeparator;

    /** The field separator in an escaped form for using in regexs */
    protected static String escapedFieldSeparator;

    /** Whether to export all metadata such as handles and provenance information */
    private boolean exportAll;

    /** A list of metadata elements to ignore */
    private Hashtable ignore;


    /**
     * Create a new instance of a CSV line holder
     *
     * @param exportAll Whether to export all metadata such as handles and provenance information
     */
    public DSpaceCSV(boolean exportAll)
    {
        // Initalise the class
        init();

        // Stoee the exportAll setting
        this.exportAll = exportAll;
    }

    /**
     * Create a new instance, reading the lines in from file
     *
     * @param f The file to read from
     *
     * @throws Exception thrown if there is an error reading or processing the file
     */
    public DSpaceCSV(File f) throws Exception
    {
        // Initalise the class
        init();

        // Open the CSV file
        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF8"));

        // Read the heading line
        String head = input.readLine();
        String[] headingElements = head.split(escapedFieldSeparator);
        for (String element : headingElements)
        {
            // Remove surrounding quotes if there are any
            if ((element.startsWith("\"")) && (element.endsWith("\"")))
            {
                element = element.substring(1, element.length() - 1);
            }

            if (!"id".equals(element))
            {
                // Store the heading
                headings.add(element);
            }
        }

        // Read each subsequent line
        String line;
        while ((line = input.readLine()) != null){
            // Are there an odd number of quotes?
            while (((" " + line + " ").split("\"").length)%2 == 0)
            {
                line = line + "\n" + input.readLine();
            }

            // Parse the item metadata
            addItem(line);
        }
    }

    /**
     * Initalise this class with values from dspace.cfg
     */
    private void init()
    {
        // Set the value separator
        setValueSeparator();
        
        // Set the field separator
        setFieldSeparator();

        // Create the headings
        headings = new ArrayList<String>();

        // Create the blank list of items
        lines = new ArrayList<DSpaceCSVLine>();

        // Initalise the counter
        counter = 0;

        // Set the metadata fields to ignore
        ignore = new Hashtable();
        String toIgnore = ConfigurationManager.getProperty("bulkedit.ignore-on-export");
        if ((toIgnore == null) || ("".equals(toIgnore.trim())))
        {
            // Set a default value
            toIgnore = "dc.date.accession, dc.date.available, " +
                       "dc.description.provenance";
        }
        String[] toIgnoreArray = toIgnore.split(",");
        for (String toIgnoreString : toIgnoreArray)
        {
            if (!"".equals(toIgnoreString.trim()))
            {
                ignore.put(toIgnoreString.trim(), toIgnoreString.trim());
            }
        }
    }

    /**
     * Set the value separator for multiple values stored in one csv value.
     *
     * Is set in dspace.cfg as bulkedit.valueseparator
     *
     * If not set, defaults to double pipe '||'
     */
    private void setValueSeparator()
    {
        // Get the value separator
        valueSeparator = ConfigurationManager.getProperty("bulkedit.valueseparator");
        if ((valueSeparator != null) && (!"".equals(valueSeparator.trim())))
        {
            valueSeparator = valueSeparator.trim();
        }
        else
        {
            valueSeparator = "||";
        }

        // Now store the escaped version
        Pattern spchars = Pattern.compile("([\\\\*+\\[\\](){}\\$.?\\^|])");
        Matcher match = spchars.matcher(valueSeparator);
        escpaedValueSeparator = match.replaceAll("\\\\$1");
    }

    /**
     * Set the field separator use to separate fields in the csv.
     *
     * Is set in dspace.cfg as bulkedit.fieldseparator
     *
     * If not set, defaults to comma ','.
     *
     * Special values are 'tab', 'hash' and 'semicolon' which will
     * get substituted from the text to the value.
     */
    private void setFieldSeparator()
    {
        // Get the value separator
        fieldSeparator = ConfigurationManager.getProperty("bulkedit.fieldseparator");
        if ((fieldSeparator != null) && (!"".equals(fieldSeparator.trim())))
        {
            fieldSeparator = fieldSeparator.trim();
            if ("tab".equals(fieldSeparator))
            {
                fieldSeparator = "\t";
            }
            else if ("semicolon".equals(fieldSeparator))
            {
                fieldSeparator = ";";
            }
            else if ("hash".equals(fieldSeparator))
            {
                fieldSeparator = "#";
            }
            else
            {
                fieldSeparator = fieldSeparator.trim();
            }
        }
        else
        {
            fieldSeparator = ",";
        }

        // Now store the escaped version
        Pattern spchars = Pattern.compile("([\\\\*+\\[\\](){}\\$.?\\^|])");
        Matcher match = spchars.matcher(fieldSeparator);
        escapedFieldSeparator = match.replaceAll("\\\\$1");
    }

    /**
     * Add a DSpace item to the CSV file
     *
     * @param i The DSpace item
     *
     * @throws Exception if something goes wrong with adding the Item
     */
    public void addItem(Item i) throws Exception
    {
        // Create the CSV line
        DSpaceCSVLine line = new DSpaceCSVLine(i.getID());

        // Add in owning collection
        String owningCollectionHandle = i.getOwningCollection().getHandle();
        line.add("collection", owningCollectionHandle);

        // Add in any mapped collections
        Collection[] collections = i.getCollections();
        for (Collection c : collections)
        {
            // Only add if it is not the owning collection
            if (!c.getHandle().equals(owningCollectionHandle))
            {
                line.add("collection", c.getHandle());   
            }
        }

        // Populate it
        DCValue md[] = i.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (DCValue value : md)
        {
            // Get the key (schema.element)
            String key = value.schema + "." + value.element;

            // Add the qualifer if there is one (schema.element.qualifier)
            if (value.qualifier != null)
            {
                key = key + "." + value.qualifier;
            }

            // Add the language if there is one (schema.element.qualifier[langauge])
            //if ((value.language != null) && (!"".equals(value.language)))
            if (value.language != null)
            {
                key = key + "[" + value.language + "]";
            }

            // Store the item
            if (exportAll || okToExport(value))
            {
                line.add(key, value.value);
                if (!headings.contains(key))
                {
                    headings.add(key);
                }
            }
        }
        lines.add(line);
        counter++;
    }

    /**
     * Add an item to the CSV file, from a CSV line of elements
     *
     * @param line The line of elements
     * @throws Exception Thrown if an error occurs when adding the item
     */
    public void addItem(String line) throws Exception
    {
        // Check to see if the last character is a field separator, which hides the last empy column
        boolean last = false;
        if (line.endsWith(fieldSeparator))
        {
            // Add a space to the end, then remove it later
            last = true;
            line += " ";
        }
        
        // Split up on field separator
        String[] parts = line.split(escapedFieldSeparator);
        ArrayList<String> bits = new ArrayList<String>();
        bits.addAll(Arrays.asList(parts));

        // Merge parts with embedded separators
        boolean alldone = false;
        while (!alldone)
        {
            boolean found = false;
            int i = 0;
            for (String part : bits)
            {
                int bitcounter = part.length() - part.replaceAll("\"", "").length();
                if ((part.startsWith("\"")) && ((!part.endsWith("\"")) || ((bitcounter %2) == 1)))
                {
                    found = true;
                    String add = bits.get(i) + fieldSeparator + bits.get(i + 1);
                    bits.remove(i);
                    bits.add(i, add);
                    bits.remove(i + 1);
                    break;
                }
                i++;
            }
            alldone = !found;
        }

        // Deal with quotes around the elements
        int i = 0;
        for (String part : bits)
        {
            if ((part.startsWith("\"")) && (part.endsWith("\"")))
            {
                part = part.substring(1, part.length() - 1);
                bits.set(i, part);
            }
            i++;
        }

        // Remove embedded quotes
        i = 0;
        for (String part : bits)
        {
            if (part.contains("\"\""))
            {
                part = part.replaceAll("\"\"", "\"");
                bits.set(i, part);
            }
            i++;
        }

        // Add elements to a DSpaceCSVLine
        String id = parts[0].replaceAll("\"", "");
        DSpaceCSVLine csvLine;
                
        // Is this an existing item, or a new item (where id = '+')
        if ("+".equals(id))
        {
            csvLine = new DSpaceCSVLine();
        }
        else
        {
            try
            {
                csvLine = new DSpaceCSVLine(Integer.parseInt(id));
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Invalid item identifier: " + id);
                System.err.println("Please check your CSV file for informaton. " +
                                   "Item id must be numeric, or a '+' to add a new item");
                throw(nfe);
            }
        }

        // Add the rest of the parts
        i = 0;
        for (String part : bits)
        {
            if (i > 0)
            {
                // Is this a last empty item?
                if ((last) && (i == headings.size()))
                {
                    part = "";
                }

                // Make sure we register that this column was there
                csvLine.add(headings.get(i - 1), null);
                String[] elements = part.split(escpaedValueSeparator);
                for (String element : elements)
                {
                    if ((element != null) && (!"".equals(element)))
                    {
                        csvLine.add(headings.get(i - 1), element);
                    }
                }
            }
            i++;
        }
        lines.add(csvLine);
        counter++;
    }

    /**
     * Get the lines in CSV holders
     *
     * @return The lines
     */
    public ArrayList<DSpaceCSVLine> getCSVLines()
    {
        // Return the lines
        return lines;
    }

    /**
     * Get the CSV lines as an array of CSV formatted strings
     *
     * @return the array of CSV formatted Strings
     */
    public String[] getCSVLinesAsStringArray()
    {
        // Create the headings line
        String[] csvLines = new String[counter + 1];
        csvLines[0] = "id" + fieldSeparator + "collection";
        Collections.sort(headings);
        for (String value : headings)
        {
            csvLines[0] = csvLines[0] + fieldSeparator + value;
        }

        Iterator<DSpaceCSVLine> i = lines.iterator();
        int c = 1;
        while (i.hasNext())
        {
            csvLines[c++] = i.next().toCSV(headings);
        }

        return csvLines;
    }

    /**
     * Save the CSV file to the given filename
     *
     * @param filename The filename to save the CSV file to
     *
     * @throws IOException Thrown if an error occurs when writing the file
     */
    public void save(String filename) throws IOException
    {
        // Save the file
        BufferedWriter out = new BufferedWriter(
                             new OutputStreamWriter(
                             new FileOutputStream(filename), "UTF8"));
        for (String csvLine : getCSVLinesAsStringArray()) {
            out.write(csvLine + "\n");
        }
        out.flush();
        out.close();
    }

    /**
     * Is it Ok to export this value? When exportAll is set to false, we don't export
     * some of the metadata elements.
     *
     * The list can be configured via the key bulkedit.ignore-on-export in dspace.cfg
     *
     * @param md The DCValue to examine
     * @return Whether or not it is OK to export this element
     */
    private boolean okToExport(DCValue md)
    {
        // First check the metadata format, and K all non DC elements
        if (!"dc".equals(md.schema))
        {
            return true;
        }

        // Now compare with the list to ignore
        String key = md.schema + "." + md.element;
        if (md.qualifier != null)
        {
            key += "." + md.qualifier;
        }
        if (ignore.get(key) != null) {
            return false;
        }

        // Must be OK, so don't ignore
        return true;
    }

    /**
     * Return the csv file as one long formatted string
     *
     * @return The formatted String as a csv
     */
    public String toString()
    {
        // Return the csv as one long string
        StringBuffer csvLines = new StringBuffer();
        String[] lines = this.getCSVLinesAsStringArray();
        for (String line : lines)
        {
            csvLines.append(line).append("\n");
        }
        return csvLines.toString();
    }

    /**
     * Test main method to check the marshalling and unmarshalling of strings in and out of CSV format
     *
     * @param args Not used
     * @throws Exception Thrown if something goes wrong
     */
    public static void main(String[] args) throws Exception
    {
        // Test the CSV parsing
        String[] csv = {"id,\"dc.title\",dc.contributor.author,dc.description.abstract",
                        "1,Easy line,\"Lewis, Stuart\",A nice short abstract",
                        "2,Two authors,\"Lewis, Stuart||Bloggs, Joe\",Two people wrote this item",
                        "3,Three authors,\"Lewis, Stuart||Bloggs, Joe||Loaf, Meat\",Three people wrote this item",
                        "4,\"Two line\ntitle\",\"Lewis, Stuart\",abstract",
                        "5,\"\"\"Embedded quotes\"\" here\",\"Lewis, Stuart\",\"Abstract with\ntwo\nnew lines\"",
                        "6,\"\"\"Unbalanced embedded\"\" quotes\"\" here\",\"Lewis, Stuart\",\"Abstract with\ntwo\nnew lines\"",};

        // Write the string to a file
        String filename = "test.csv";
        BufferedWriter out = new BufferedWriter(
                             new OutputStreamWriter(
                             new FileOutputStream(filename), "UTF8"));
        for (String csvLine : csv) {
            out.write(csvLine + "\n");
        }
        out.flush();
        out.close();
        System.gc();

        // test the CSV parsing
        DSpaceCSV dcsv = new DSpaceCSV(new File(filename));
        String[] lines = dcsv.getCSVLinesAsStringArray();
        for (String line : lines)
        {
            System.out.println(line);          
        }

        // Delete the test file
        File toDelete = new File(filename);
        toDelete.delete();
    }
}
