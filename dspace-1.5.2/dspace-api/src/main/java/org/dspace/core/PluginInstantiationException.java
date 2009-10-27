/*
 * PluginInstantiationException.java
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

package org.dspace.core;

/**
 * This exception indicates a fatal error when instantiating a plugin class.
 * <p>
 * It should only be thrown when something unexpected happens in the
 * course of instantiating a plugin, e.g. an access error, class not found,
 * etc.  Simply not finding a class in the configuration is not an exception.
 * <p>
 * This is a RuntimeException so it doesn't have to be declared, and can
 * be passed all the way up to a generalized fatal exception handler.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see PluginManager
 */

public class PluginInstantiationException extends RuntimeException
{
    /**
     * @param msg Error message text.
     */
    public PluginInstantiationException(String msg)
    {
        super(msg);
    }

    /**
     * @param msg Error message text.
     * @cause cause other exception that this one is wrapping.
     */
    public PluginInstantiationException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

    /**
     * @cause cause other exception that this one is wrapping.
     */
    public PluginInstantiationException(Throwable cause)
    {
        super(cause);
    }
}
