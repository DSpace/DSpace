/*
 * Constants.java
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
 * Class with constants and matching strings, for DSpace types.  These numbers
 * must never be changed!!
 *
 * @author  David Stuve
 * @version $Revision$
 */
public class Constants
{
    /** Type of bitstream objects */
    public static final int BITSTREAM   = 0;

    /** Type of bundle objects */
    public static final int BUNDLE      = 1;

    /** Type of item objects */
    public static final int ITEM        = 2;

    /** Type of collection objects */
    public static final int COLLECTION  = 3;

    /** Type of community objects */
    public static final int COMMUNITY   = 4;

    /** DSpace site type */
    public static final int SITE        = 5;

    /** Type of eperson groups */
    public static final int GROUP       = 6;

    /** Type of individual eperson objects */
    public static final int EPERSON     = 7;

    /**
     * lets you look up type names from the type IDs
     */
    public static final String[] typeText =
    {
        "BITSTREAM",
        "BUNDLE",
        "ITEM",
        "COLLECTION",
        "COMMUNITY",
        "SITE",
        "GROUP",
        "EPERSON",
    };


    /** Action of reading, viewing or downloading something */
    public static final int READ            = 0;

    /** Action of modifying something */
    public static final int WRITE           = 1;

    /**
     * Action of deleting something.  Different from removing something
     * from a container.
     * @see #REMOVE
     */
    public static final int DELETE          = 2;

    /**
     * Action of adding something to a container.  For example, to add
     * an item to a collection, a user must have <code>ADD</code> permission
     * on the collection.
    */
    public static final int ADD             = 3;

    /**
     * Action of removing something from a container.  Different from
     * deletion.
     * @see #DELETE
     */
    public static final int REMOVE          = 4;

    /** Action of performing workflow step 1 */
    public static final int WORKFLOW_STEP_1 = 5;

    /** Action of performing workflow step 2 */
    public static final int WORKFLOW_STEP_2 = 6;

    /** Action of performing workflow step 3 */
    public static final int WORKFLOW_STEP_3 = 7;

    /** Action of performing a workflow */
    public static final int WORKFLOW_ABORT  = 8;


    /**
     * lets you look up action names from the action IDs
     */
    public static final String[] actionText =
    {
        "READ",
        "WRITE",
        "DELETE",
        "ADD",
        "REMOVE",
        "WORKFLOW_STEP_1",
        "WORKFLOW_STEP_2",
        "WORKFLOW_STEP_3",
        "WORKFLOW_ABORT"
    };


    /**
     * If you know the type string, look up the corresponding type ID constant.
     *
     * @param type  String with the name of the type (must be exact match)
     *
     * @return  the corresponding type ID, or <code>-1</code> if the type
     *          string is unknown
     */
    public static int getTypeID(String type)
    {
        for (int i = 0; i < typeText.length; i++)
        {
            if (typeText[i].equals(type))
            {
                return i;
            }
        }

        return -1;
    }


    /**
     * If you know the action string, look up the corresponding type ID
     * constant.
     *
     * @param type  String with the name of the action (must be exact match)
     *
     * @return  the corresponding action ID, or <code>-1</code> if the action
     *          string is unknown
     */
    public static int getActionID(String action)
    {
        for (int i = 0; i < actionText.length; i++)
        {
            if (actionText[i].equals(action))
            {
                return i;
            }
        }

        return -1;
    }
}
