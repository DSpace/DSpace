/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents a database row.
 * 
 * @author Peter Breton
 * @version $Revision$
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
    private Map<String, Object> data = new HashMap<String, Object>();

    private Map<String, Boolean> changed = new HashMap<String, Boolean>();

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
    public TableRow(String table, List<String> columns)
    {
        this.table = table;

        for (String column : columns)
        {
            String canonicalized = ColumnInfo.canonicalize(column);
            data.put(canonicalized, NULL_OBJECT);
            changed.put(canonicalized, Boolean.TRUE);
        }
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

    public void setTable(String table) {
        this.table = table;
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
        try
        {
            return canonicalizeAndCheck(column) != null;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
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
        return hasColumnChangedCanonicalized(ColumnInfo.canonicalize(column));
    }

    boolean hasColumnChangedCanonicalized(String column)
    {
        return changed.get(column) == Boolean.TRUE;
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
        return isColumnNullCanonicalized(canonicalizeAndCheck(column));
    }

    boolean isColumnNullCanonicalized(String column)
    {
        return data.get(column) == NULL_OBJECT;
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
        String canonicalized = canonicalizeAndCheck(column);
        if (isColumnNullCanonicalized(canonicalized))
        {
            return -1;
        }

        Object value = data.get(canonicalized);

        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column + " not present");
        }

        if (value instanceof Integer)
        {
            return ((Integer) value);
        }
        else if (value instanceof Long)
        {
            long longValue = (Long)value;
            if ((longValue > Integer.MAX_VALUE) || longValue < Integer.MIN_VALUE)
                throw new IllegalArgumentException("Value for " + column + " does not fit in an Integer");
            else
            {
                return (int)longValue;
            }
        }
        else if (value instanceof BigDecimal)
        {
            return ((BigDecimal)value).intValueExact();
        }
        else
        {
            throw new IllegalArgumentException("Value for " + column + " is not an integer");
        }
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
        String canonicalized = canonicalizeAndCheck(column);
        if (isColumnNullCanonicalized(canonicalized))
        {
            return -1;
        }

        Object value = data.get(canonicalized);

        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column + " not present");
        }

        // If the value is an integer, it can be represented without error as a long
        // So, allow the return of a long. (This is needed for Oracle support).
        if ((value instanceof Integer))
        {
            return ((Integer) value).longValue();
        }
        else if (value instanceof Long)
        {
            return ((Long) value);
        }
        else if (value instanceof BigDecimal)
        {
            return ((BigDecimal)value).longValueExact();
        }
        else
        {
            throw new IllegalArgumentException("Value for " + column + " is not a long");
        }
    }

    /**
     * Return the BigDecimal value of column.
     *
     * @param column
     *      The column name (case-insensitive).
     * @return The BigDecimal value of the column, or -1 if the column is an SQL null.
     * @throws IllegalArgumentException if the column does not exist or is not
     *      convertible to BigDecimal.
     */
    public BigDecimal getNumericColumn(String column)
    {
        String canonicalized = canonicalizeAndCheck(column);
        if (isColumnNullCanonicalized(canonicalized))
            return BigDecimal.valueOf(-1);

        Object value = data.get(canonicalized);

        if (value == null)
            throw new IllegalArgumentException("Column " + column + " not present");

        if (value instanceof Integer)
            return new BigDecimal((Integer)value);
        else if (value instanceof Long)
            return new BigDecimal((Long)value);
        else if (value instanceof Double)
            return new BigDecimal((Double)value);
        else if (value instanceof BigDecimal)
            return (BigDecimal)value;
        else
            throw new IllegalArgumentException("Value for " + column + " is not numeric");
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
        String canonicalized = canonicalizeAndCheck(column);
        if (isColumnNullCanonicalized(canonicalized))
        {
            return -1;
        }

        Object value = data.get(canonicalized);

        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column + " not present");
        }
        
        if (!(value instanceof Double))
        {
            throw new IllegalArgumentException("Value for " + column + " is not a double");
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
        String canonicalized = canonicalizeAndCheck(column);
        if (isColumnNullCanonicalized(canonicalized))
        {
            return null;
        }

        Object value = data.get(canonicalized);

        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column + " not present");
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
        String canonicalized = canonicalizeAndCheck(column);
        if (isColumnNullCanonicalized(canonicalized))
        {
            return false;
        }

        Object value = data.get(canonicalized);

        // make sure that we tolerate integers or booleans
        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column + " not present");
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
        else if (value instanceof Long)
        {
            return ((Long) value) != 0;
        }
        else if (value instanceof BigDecimal)
        {
            return ! ((BigDecimal) value).equals(BigDecimal.ZERO);
        }
        else
        {
            throw new IllegalArgumentException("Value is not a boolean or an integer");
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
        String canonicalized = canonicalizeAndCheck(column);
        if (isColumnNullCanonicalized(canonicalized))
        {
            return null;
        }

        Object value = data.get(canonicalized);

        if (value == null)
        {
            throw new IllegalArgumentException("Column " + column + " not present");
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
        String canonicalized = canonicalizeAndCheck(column);
        if (data.get(canonicalized) != NULL_OBJECT)
        {
            data.put(canonicalized, NULL_OBJECT);
            changed.put(canonicalized, Boolean.TRUE);
        }
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
        String canonicalized = canonicalizeAndCheck(column);
        if (DatabaseManager.isOracle())
        {
            // if oracle, use 1 or 0 for true/false
            Integer value = b ? Integer.valueOf(1) : Integer.valueOf(0);
            if (!value.equals(data.get(canonicalized)))
            {
                data.put(canonicalized, value);
                changed.put(canonicalized, Boolean.TRUE);
            }
        }
        else
        {
            // default to postgres true/false
            Boolean value = b ? Boolean.TRUE : Boolean.FALSE;
            if (!value.equals(data.get(canonicalized)))
            {
                data.put(canonicalized, value);
                changed.put(canonicalized, Boolean.TRUE);
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
        String canonicalized = canonicalizeAndCheck(column);
        Object value = (s == null) ? NULL_OBJECT : s;
        if (!value.equals(data.get(canonicalized)))
        {
            data.put(canonicalized, value);
            changed.put(canonicalized, Boolean.TRUE);
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
        String canonicalized = canonicalizeAndCheck(column);
        Integer value = Integer.valueOf(i);
        if (!value.equals(data.get(canonicalized)))
        {
            data.put(canonicalized, value);
            changed.put(canonicalized, Boolean.TRUE);
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
        String canonicalized = canonicalizeAndCheck(column);
        Long value = Long.valueOf(l);
        if (!value.equals(data.get(canonicalized)))
        {
            data.put(canonicalized, value);
            changed.put(canonicalized, Boolean.TRUE);
        }
    }

    /**
     * Set column to the BigDecimal bd.
     *
     * @param column
     *          The column name (case-insensitive).
     * @param bd
     *          The BigDecimal value.
     * @throws IllegalArgumentException if the column does not exist.
     */
    public void setColumn(String column, BigDecimal bd)
    {
        String canonicalized = canonicalizeAndCheck(column);
        Object value = (bd == null) ? NULL_OBJECT : bd;
        if (!value.equals(data.get(canonicalized)))
        {
            data.put(canonicalized, bd);
            changed.put(canonicalized, Boolean.TRUE);
        }
    }

    /**
     * Set column to the double d.
     * 
     * If the column does not exist, an IllegalArgumentException is thrown.
     * 
     * @param column
     *            The column name (case-insensitive)
     * @param d
     *            The double value
     */
    public void setColumn(String column, double d)
    {
        String canonicalized = canonicalizeAndCheck(column);
        Double value = new Double(d);
        if (!value.equals(data.get(canonicalized)))
        {
            data.put(canonicalized, value);
            changed.put(canonicalized, Boolean.TRUE);
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
        String canonicalized = canonicalizeAndCheck(column);
        Object value = (d == null) ? NULL_OBJECT : d;
        if (!value.equals(data.get(canonicalized)))
        {
            data.put(canonicalized, value);
            changed.put(canonicalized, Boolean.TRUE);
        }
    }

    ////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////

    /**
     * Return a String representation of this object.
     * 
     * @return String representation
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

    private String canonicalizeAndCheck(String column)
    {
        if (data.containsKey(column))
        {
            return column;
        }

        String canonicalized = ColumnInfo.canonicalize(column);
        if (data.containsKey(canonicalized))
        {
            return canonicalized;
        }

        throw new IllegalArgumentException("No such column '" + canonicalized + "'");
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
        for (String column : changed.keySet())
        {
            changed.put(column, Boolean.FALSE);
        }
    }
}
