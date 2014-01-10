/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

/**
 * Represents a column in an RDBMS table.
 */

public class ColumnInfo
{
    /** The name of the column */
    private String name;

    private String canonicalizedName;

    /** The JDBC type of the column */
    private int type;

    /** True if this column is a primary key */
    private boolean isPrimaryKey;

    /**
     * Constructor
     */
    ColumnInfo()
    {
    }

    /**
     * Constructor
     */
    ColumnInfo(String name, int type)
    {
        this.name = name;
        this.type = type;
        this.canonicalizedName = canonicalize(name);
    }

    /**
     * Return the column name.
     *
     * @return - The column name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Return the column name.
     *
     * @return - The column name
     */
    public String getCanonicalizedName()
    {
        return canonicalizedName;
    }

    /**
     * Set the column name
     *
     * @param v -
     *            The column name
     */
    void setName(String v)
    {
        name = v;
        canonicalizedName = canonicalize(name);
    }

    /**
     * Return the JDBC type. This is one of the constants from java.sql.Types.
     *
     * @return - The JDBC type
     * @see java.sql.Types
     */
    public int getType()
    {
        return type;
    }

    /**
     * Set the JDBC type. This should be one of the constants from
     * java.sql.Types.
     *
     * @param v -
     *            The JDBC type
     * @see java.sql.Types
     */
    void setType(int v)
    {
        type = v;
    }

    /**
     * Return true if this column is a primary key.
     *
     * @return True if this column is a primary key, false otherwise.
     */
    public boolean isPrimaryKey()
    {
        return isPrimaryKey;
    }

    /**
     * Set whether this column is a primary key.
     *
     * @param v
     *            True if this column is a primary key.
     */
    void setIsPrimaryKey(boolean v)
    {
        this.isPrimaryKey = v;
    }

    /*
     * Return true if this object is equal to other, false otherwise.
     *
     * @return True if this object is equal to other, false otherwise.
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof ColumnInfo))
        {
            return false;
        }

        ColumnInfo theOther = (ColumnInfo) other;

        return ((name != null) ? name.equals(theOther.name)
                : (theOther.name == null))
                && (type == theOther.type)
                && (isPrimaryKey == theOther.isPrimaryKey);
    }

    /*
     * Return a hashCode for this object.
     *
     * @return A hashcode for this object.
     */
    public int hashCode()
    {
        return new StringBuffer().append(name).append(type)
                .append(isPrimaryKey).toString().hashCode();
    }

    static String canonicalize(String column)
    {
        return column.toLowerCase();
    }
}
