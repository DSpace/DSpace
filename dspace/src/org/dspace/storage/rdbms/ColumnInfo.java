/*
 * ColumnInfo.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
 * Simple representation of a database column
 *
 */
public class ColumnInfo
{
    /**
     * The name of the column
     */
    private String name;

    /**
     * The JDBC type of the column
     */
    private int type;

    /**
     * True if this column is a primary key
     */
    private boolean isPrimaryKey;

    /**
     * Constructor
     */
    ColumnInfo() {}

    /**
     * Constructor
     */
    ColumnInfo( String name, int type )
    {
        this.name = name;
        this.type = type;
    }

    /**
     * Get the value of name
     *
     * @return - The value of name
     */
    public String getName()
    {
        return  name;
    }

    /**
     * Set the value of name
     *
     * @param v - The value of name
     */
    void setName(String v)
    {
        name = v;
    }

    /**
     * Get the value of type
     *
     * @return - The value of type
     */
    public int getType()
    {
        return  type;
    }

    /**
     * Set the value of type
     *
     * @param v - The value of type
     */
    void setType(int v)
    {
        type = v;
    }

    /**
     * Get the value of isPrimaryKey.
     * @return Value of isPrimaryKey.
     */
    public boolean isPrimaryKey()
    {
        return isPrimaryKey;
    }

    /**
     * Set the value of isPrimaryKey.
     * @param v  Value to assign to isPrimaryKey.
     */
    void setIsPrimaryKey(boolean  v)
    {
        this.isPrimaryKey = v;
    }

    /*
     * Return true if this object is equal to OTHER, false otherwise
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof ColumnInfo))
            return false;

        ColumnInfo theOther = (ColumnInfo) other;

        return
            (name != null ? name.equals(theOther.name) :
             theOther.name == null) &&
            (type == theOther.type) &&
            (isPrimaryKey == theOther.isPrimaryKey);
    }

    /*
     * Return a hashCode for this object.
     */
    public int hashCode()
    {
        return new StringBuffer().append(name).append(type).append(isPrimaryKey).toString().hashCode();
    }
}
