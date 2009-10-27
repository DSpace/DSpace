/*
 * AuthorizeException.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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
package org.dspace.authorize;

import org.dspace.content.DSpaceObject;

/**
 * Exception indicating the current user of the context does not have permission
 * to perform a particular action.
 * 
 * @author David Stuve
 * @version $Revision$
 */
public class AuthorizeException extends Exception
{
    private int myaction; // action attempted, or -1

    private DSpaceObject myobject; // object action attempted on or null

    /**
     * Create an empty authorize exception
     */
    public AuthorizeException()
    {
        super();

        myaction = -1;
        myobject = null;
    }

    /**
     * create an exception with only a message
     * 
     * @param message
     */
    public AuthorizeException(String message)
    {
        super(message);

        myaction = -1;
        myobject = null;
    }

    /**
     * Create an authorize exception with a message
     * 
     * @param message
     *            the message
     */
    public AuthorizeException(String message, DSpaceObject o, int a)
    {
        super(message);

        myobject = o;
        myaction = a;
    }

    public int getAction()
    {
        return myaction;
    }

    public DSpaceObject getObject()
    {
        return myobject;
    }
}
