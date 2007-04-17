/*
 * SeriesNumber.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2004/12/22 17:48:40 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
package org.dspace.content;

/**
 * Series and report number, as stored in relation.ispartofseries
 * 
 * @author Robert Tansley
 * @version $Id: DCSeriesNumber.java,v 1.4 2004/12/22 17:48:40 jimdowning Exp $
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