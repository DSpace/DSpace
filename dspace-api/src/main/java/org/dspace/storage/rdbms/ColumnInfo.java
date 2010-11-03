/*
 * ColumnInfo.java
 *
 * Version: $Revision: 5641 $
 *
 * Date: $Date: 2010-10-26 10:01:47 +0100 (Tue, 26 Oct 2010) $
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
