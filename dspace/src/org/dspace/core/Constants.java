/*
 * DSpaceTypes.java
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

package org.dspace.core;

/**
 * Class with constants and matching strings, for DSpace types
 *
 * @author dstuve
 * @version $Revision$
 */

public class Constants
{
    public static final int BITSTREAM   = 0;
    public static final int BUNDLE      = 1;
    public static final int ITEM        = 2;
    public static final int COLLECTION  = 3;
    public static final int COMMUNITY   = 4;
    public static final int SITE        = 5;
    public static final int EGROUPS     = 6;
    public static final int EPERSON     = 7;
    public static final int WORKFLOW    = 8;

    /**
     * typetext lets you look up type names from the type IDs
     */
    public static final String[] typetext =
    {
        "BITSTREAM",
        "BUNDLE",
        "ITEM",
        "COLLECTION",
        "COMMUNITY",
        "SITE",
        "EGROUPS",
        "EPERSON",
        "WORKFLOW"
    };

    public static final int READ            = 0;
    public static final int WRITE           = 1;
    public static final int DELETE          = 2;
    public static final int ADD             = 3;
    public static final int REMOVE          = 4;
    public static final int SUBMIT_REVIEW   = 5; // workflow
    public static final int SUBMIT_ADMIN    = 6; // workflow
    public static final int SUBMIT_EDIT	    = 7; // workflow
    public static final int SUBMIT_ABORT    = 8; // workflow

    public static final String[] actiontext =
    {
        "READ",
        "WRITE",
        "DELETE",
        "ADD",
        "REMOVE",
        "SUBMIT_REVIEW",
        "SUBMIT_ADMIN",
        "SUBMIT_EDIT",
        "SUBMIT_ABORT"
    };


    /**
     * If you know the string, look up the corresponding type ID constant,
     * or -1 if no match found.
     *
     * @param type  String with the name of the type (must be exact match)
     */
    public static int getTypeNumber(String type)
    {
        for (int i = 0; i < typetext.length; i++)
        {
            if (typetext[i].equals(type))
                return i;
        }

        return -1;
    }


    /**
     * Return the constant corresponding to ACTION, or -1
     * if no match is found.
     */
     
    public static int getActionNumber(String action)
    {
        for (int i = 0; i < actiontext.length; i++)
        {
            if (actiontext[i].equals(action))
                return i;
        }

        return -1;
    }
}
