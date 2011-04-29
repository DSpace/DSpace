/*
 * DCSeriesNumber.java
 *
 * Version: $Revision: 3761 $
 *
 * Date: $Date: 2009-05-07 00:18:02 -0400 (Thu, 07 May 2009) $
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
package org.dspace.content;

/**
 * Series and report number, as stored in relation.ispartofseries
 * 
 * @author Robert Tansley
 * @version $Id: DCSeriesNumber.java 3761 2009-05-07 04:18:02Z stuartlewis $
 */
public class DCSeriesNumber
{
    /** Series */
    private String series;

    /** Number */
    private String number;

    /** Construct clean series number */
    public DCSeriesNumber()
    {
        series = null;
        number = null;
    }

    /**
     * Construct from raw DC value
     * 
     * @param value
     *            value from database
     */
    public DCSeriesNumber(String value)
    {
        this();

        int semicolon = -1;

        if (value != null)
        {
            semicolon = value.indexOf(';');
        }

        if (semicolon >= 0)
        {
            series = value.substring(0, semicolon);
            number = value.substring(semicolon + 1);
        }
        else
        {
            series = value;
        }
    }

    /**
     * Construct from given values
     * 
     * @param s
     *            the series
     * @param n
     *            the number
     */
    public DCSeriesNumber(String s, String n)
    {
        series = s;
        number = n;
    }

    /**
     * Write as raw DC value
     * 
     * @return the series and number as they should be stored in the DB
     */
    public String toString()
    {
        if (series == null)
        {
            return (null);
        }
        else if (number == null)
        {
            return (series);
        }
        else
        {
            return (series + ";" + number);
        }
    }

    /**
     * Get the series name - guaranteed non-null
     */
    public String getSeries()
    {
        return ((series == null) ? "" : series);
    }

    /**
     * Get the number - guaranteed non-null
     */
    public String getNumber()
    {
        return ((number == null) ? "" : number);
    }
}
