/*
 * TableRow.java
 *
 * Version: $Revision: 4316 $
 *
 * Date: $Date: 2009-10-02 22:30:56 -0400 (Fri, 02 Oct 2009) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.storage.rdbms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dspace.core.ConfigurationManager;

/**
 * Represents a database row.
 * 
 * @author Peter Breton
 * @version $Revision: 4316 $
 */
public class TableRow
{
    /** Marker object to indicate NULLs. */
    private static final Object NULL_OBJECT = new Object();

    /** The name of the database table containing this row */
    private String table;

    /**
     * A map of column names to column values. The key of the map is a String,
     * the column name; the value is an Object, either an Integer, Boolean,
     * Date, or String. If the value is NULL_OBJECT, then the column was NULL.
     */
    private Map data = new HashMap();

    private Map changed = new HashMap();

    /**
     * Constructor
     * 
     * @param table
     *            The name of the database table containing this row.
     * @param columns
     *            A list of column names. Each member of the List is a String.
     *            After construction, the list of columns is fixed; attempting
     *            to access a column not in the list will cause an
     *            IllegalArgumentException to be thrown.
     */
    public TableRow(String table, List columns)
    {
        this.table = table;
        nullColumns(columns);
        resetChanged(columns);
    }

    /**
     * Return the name of the table containing this row, or null if this row is
     * not associated with a database table.
     * 
     * @return The name of the table containing this row
     */
    public String getTable()
    {
        return table;
    }

    /**
     * Return true if this row contains a column with this name.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @return True if this row contains a column with this name.
     */
    public boolean hasColumn(String column)
    {
        return data.get(canonicalize(column)) != null;
    }

    /**
     * Return true if this row contains this column and the value has been updated.
     *
     * @param column
     *            The column name (case-insensitive)
     * @return True if this row contains a column with this name.
     */
    public boolean hasColumnChanged(String column)
    {
        return changed.get(canonicalize(column)) == Boolean.TRUE;
    }

    /**
     * Return true if the column is an SQL NULL.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @return True if the column is an SQL NULL
     */
    public boolean isColumnNull(String column)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        return data.get(canonicalize(column)) == NULL_OBJECT;
    }
    
    /**
     * Return the integer value of column.
     * 
     * If the column's type is not an integer, or the column does not exist, an
     * IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @return The integer value of the column, or -1 if the column is an SQL
     *         null.
     */
    public int getIntColumn(String column)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String name = canonicalize(column);

        if (isColumnNull(name))
        {
            return -1;
        }

        Object value = data.get(name);

        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column
                    + " not present");
        }

        if (!(value instanceof Integer))
        {
            throw new IllegalArgumentException("Value for " + column
                    + " is not an integer");
        }

        return ((Integer) value).intValue();
    }

    /**
     * Return the long value of column.
     * 
     * If the column's type is not an long, or the column does not exist, an
     * IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @return The long value of the column, or -1 if the column is an SQL null.
     */
    public long getLongColumn(String column)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String name = canonicalize(column);

        if (isColumnNull(name))
        {
            return -1;
        }

        Object value = data.get(name);

        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column
                    + " not present");
        }

        // If the value is an integer, it can be represented without error as a long
        // So, allow the return of a long. (This is needed for Oracle support).
        if ((value instanceof Integer))
        {
            return ((Integer) value).longValue();
        }
        
        if (!(value instanceof Long))
        {
            throw new IllegalArgumentException("Value for " + column
                    + " is not a long");
        }

        return ((Long) value).longValue();
    }

    /**
     * Return the double value of column.
     * 
     * If the column's type is not an float, or the column does not exist, an
     * IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @return The double value of the column, or -1 if the column is an SQL null.
     */
    public double getDoubleColumn(String column)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String name = canonicalize(column);

        if (isColumnNull(name))
        {
            return -1;
        }

        Object value = data.get(name);

        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column
                    + " not present");
        }
        
        if (!(value instanceof Double))
        {
            throw new IllegalArgumentException("Value for " + column
                    + " is not a double");
        }

        return ((Double) value).doubleValue();
    }

    /**
     * Return the String value of column.
     * 
     * If the column's type is not a String, or the column does not exist, an
     * IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @return The String value of the column, or null if the column is an SQL
     *         null.
     */
    public String getStringColumn(String column)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String name = canonicalize(column);

        if (isColumnNull(name))
        {
            return null;
        }

        Object value = data.get(name);

        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column
                    + " not present");
        }

        if (!(value instanceof String))
        {
            throw new IllegalArgumentException("Value is not an string");
        }

        return (String) value;
    }

    /**
     * Return the boolean value of column.
     * 
     * If the column's type is not a boolean, or the column does not exist, an
     * IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @return The boolean value of the column, or false if the column is an SQL
     *         null.
     */
    public boolean getBooleanColumn(String column)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String name = canonicalize(column);

        if (isColumnNull(name))
        {
            return false;
        }

        Object value = data.get(name);

        // make sure that we tolerate integers or booleans
        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column
                    + " not present");
        }

        if ((value instanceof Boolean))
        {
            return ((Boolean) value).booleanValue();
        }
        else if ((value instanceof Integer))
        {
            int i = ((Integer) value).intValue();

            if (i == 0)
            {
                return false; // 0 is false
            }

            return true; // nonzero is true
        }
        else
        {
            throw new IllegalArgumentException(
                    "Value is not a boolean or an integer");
        }
    }

    /**
     * Return the date value of column.
     * 
     * If the column's type is not a date, or the column does not exist, an
     * IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @return - The date value of the column, or null if the column is an SQL
     *         null.
     */
    public java.util.Date getDateColumn(String column)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String name = canonicalize(column);

        if (isColumnNull(name))
        {
            return null;
        }

        Object value = data.get(name);

        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column
                    + " not present");
        }

        if (!(value instanceof java.util.Date))
        {
            throw new IllegalArgumentException("Value is not a Date");
        }

        return (java.util.Date) value;
    }

    /**
     * Set column to an SQL NULL.
     * 
     * If the column does not exist, an IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     */
    public void setColumnNull(String column)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        setColumnNullInternal(canonicalize(column));
    }

    /**
     * Set column to the boolean b.
     * 
     * If the column does not exist, an IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @param b
     *            The boolean value
     */
    public void setColumn(String column, boolean b)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String canonName = canonicalize(column);
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            // if oracle, use 1 or 0 for true/false
            Integer value = b ? new Integer(1) : new Integer(0);
            if (!value.equals(data.get(canonName)))
            {
                data.put(canonName, value);
                changed.put(canonName, Boolean.TRUE);
            }
        }
        else
        {
            // default to postgres true/false
            Boolean value = b ? Boolean.TRUE : Boolean.FALSE;
            if (!value.equals(data.get(canonName)))
            {
                data.put(canonName, value);
                changed.put(canonName, Boolean.TRUE);
            }
        }
    }

    /**
     * Set column to the String s. If s is null, the column is set to null.
     * 
     * If the column does not exist, an IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @param s
     *            The String value
     */
    public void setColumn(String column, String s)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String canonName = canonicalize(column);
        Object value = (s == null) ? NULL_OBJECT : s;
        if (!value.equals(data.get(canonName)))
        {
            data.put(canonName, value);
            changed.put(canonName, Boolean.TRUE);
        }
    }

    /**
     * Set column to the integer i.
     * 
     * If the column does not exist, an IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @param i
     *            The integer value
     */
    public void setColumn(String column, int i)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String canonName = canonicalize(column);
        Integer value = new Integer(i);
        if (!value.equals(data.get(canonName)))
        {
            data.put(canonName, value);
            changed.put(canonName, Boolean.TRUE);
        }
    }

    /**
     * Set column to the long l.
     * 
     * If the column does not exist, an IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @param l
     *            The long value
     */
    public void setColumn(String column, long l)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String canonName = canonicalize(column);
        Long value = new Long(l);
        if (!value.equals(data.get(canonName)))
        {
            data.put(canonName, value);
            changed.put(canonName, Boolean.TRUE);
        }
    }

    /**
     * Set column to the double d.
     * 
     * If the column does not exist, an IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @param l
     *            The double value
     */
    public void setColumn(String column, double d)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String canonName = canonicalize(column);
        Double value = new Double(d);
        if (!value.equals(data.get(canonName)))
        {
            data.put(canonName, value);
            changed.put(canonName, Boolean.TRUE);
        }
    }

    /**
     * Set column to the date d. If the date is null, the column is set to NULL
     * as well.
     * 
     * If the column does not exist, an IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @param d
     *            The date value
     */
    public void setColumn(String column, java.util.Date d)
    {
        if (!hasColumn(column))
        {
            throw new IllegalArgumentException("No such column " + column);
        }

        String canonName = canonicalize(column);
        Object value = (d == null) ? NULL_OBJECT : d;
        if (!value.equals(data.get(canonName)))
        {
            data.put(canonName, value);
            changed.put(canonName, Boolean.TRUE);
        }
    }

    ////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////

    /**
     * Return a String representation of this object.
     * 
     * @return String representaton
     */
    public String toString()
    {
        final String NEWLINE = System.getProperty("line.separator");
    	StringBuffer result;
    	
    	if (table==null)
    	{
    		result = new StringBuffer("no_table");
    	}
    	else
    	{
    		result = new StringBuffer(table);
    	}
    	
    	result.append(NEWLINE);

        for (Iterator iterator = data.keySet().iterator(); iterator.hasNext();)
        {
            String column = (String) iterator.next();
            result.append("\t").append(column).append(" = ").append(
                    isColumnNull(column) ? "NULL" : data.get(column)).append(
                    NEWLINE);
        }

        return result.toString();
    }

    /**
     * Return a hash code for this object.
     * 
     * @return int hash of object
     */
    public int hashCode()
    {
        return toString().hashCode();
    }

    /**
     * Return true if this object equals obj, false otherwise.
     * 
     * @param obj
     * @return true if TableRow objects are equal
     */
    public boolean equals(Object obj)
    {
        if (!(obj instanceof TableRow))
        {
            return false;
        }

        return data.equals(((TableRow) obj).data);
    }

    /**
     * Return the canonical name for column.
     * 
     * @param column
     *            The name of the column.
     * @return The canonical name of the column.
     */
    static String canonicalize(String column)
    {
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            // oracle requires uppercase
            return column.toUpperCase();
        }

        // postgres default lowercase
        return column.toLowerCase();
    }

    /**
     * Set columns to null.
     * 
     * @param columns -
     *            A list of the columns to set to null. Each element of the list
     *            is a String.
     */
    private void nullColumns(List columns)
    {
        for (Iterator iterator = columns.iterator(); iterator.hasNext();)
        {
            setColumnNullInternal((String) iterator.next());
        }
    }

    private void resetChanged(List columns)
    {
        for (Iterator iterator = columns.iterator(); iterator.hasNext();)
        {
            changed.put(canonicalize((String) iterator.next()), Boolean.FALSE);
        }
    }

    /**
     * package private method to reset the flags of which columns have been updated
     * This is used by the database manager after it has finished processing the contents
     * of a resultset, so that it can update only columns that have been updated.
     * Note that this method does not reset the values themselves, only the flags,
     * and should not be considered safe to call from anywhere other than the DatabaseManager.
     */
    void resetChanged()
    {
        for (Iterator iterator = changed.keySet().iterator(); iterator.hasNext();)
        {
            changed.put(canonicalize((String) iterator.next()), Boolean.FALSE);
        }
    }

    /**
     * Internal method to set column to null. The public method ensures that
     * column actually exists.
     * 
     * @param column
     */
    private void setColumnNullInternal(String column)
    {
        String canonName = canonicalize(column);
        if (data.get(canonName) != NULL_OBJECT)
        {
            data.put(canonName, NULL_OBJECT);
            changed.put(canonName, Boolean.TRUE);
        }
    }
}
