/*
 * MetadataImportException.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2010, The DSpace Foundation.  All rights reserved.
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

/**
 * Metadata importer exception
 *
 * @author Stuart Lewis
 */
public class MetadataImportInvalidHeadingException extends Exception
{
    /** The type of error (schema or element) */
    private int type;

    /** The bad heading */
    private String badHeading;

    /** Error with the schema */
    public static final int SCHEMA = 0;

    /** Error with the element */
    public static final int ELEMENT = 1;


    /**
     * Instantiate a new MetadataImportInvalidHeadingException
     *
     * @param message the error message
     * @param theType the type of the error
     */
    public MetadataImportInvalidHeadingException(String message, int theType)
    {
        super(message);
        badHeading = message;
        type = theType;
    }

    /**
     * Get the type of the exception
     *
     *  @return the type of the exception
     */
    public String getType()
    {
        return "" + type;
    }

    /**
     * Get the heading that was invalid
     *
     * @return the invalid heading
     */
    public String getBadHeader()
    {
        return badHeading;
    }

    /**
     * Get the exception message
     *
     * @return The exception message
     */
    public String getMessage()
    {
        if (type == SCHEMA)
        {
            return "Unknown metadata schema in heading: " + badHeading;
        }
        else
        {
            return "Unknown metadata elemnt in heading: " + badHeading;
        }
    }
}