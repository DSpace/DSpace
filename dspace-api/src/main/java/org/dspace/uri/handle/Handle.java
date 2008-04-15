/*
 * Handle.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
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
package org.dspace.uri.handle;

import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ObjectIdentifier;

/**
 * Primary class representing the Persistent Identifier scheme for Handles
 *
 * This class extends the ExternalIdentifier class, so that instances of this
 * class can be passed around the identifier code.  It inherits all features
 * from this super class, but provides its own implementation of the
 * <code>parseCanonicalForm</code> method.
 *
 * @author Richard Jones
 * @author James Rutherford
 */
public class Handle extends ExternalIdentifier
{
    /**
     * DO NOT USE
     *
     * Required for PluginManager to function, but should not be used
     * to construct new Handle instances in main code
     */
    public Handle()
    {
        super();
    }

    /**
     * Construct a new Handle object with the given handle value backed
     * by the given ObjectIdentifier
     *
     * @param value
     * @param oid
     */
    public Handle(String value, ObjectIdentifier oid)
    {
        super(new HandleType(), value, oid);
    }

    /**
     * Construct a new Handle object with the given handle value.  This
     * should be used with care, as any attempt to resolve an Identifiable
     * from this object may fail if an ObjectIdentifier is not available
     * which can back this identifier.
     *
     * @param value
     */
    public Handle(String value)
    {
        super(new HandleType(), value);
    }

    /**
     * Parse the provided string as the canonical form of a handle, and construct
     * an un-backed instance of the Handle object.
     * @param canonical
     * @return
     */
    public Handle parseCanonicalForm(String canonical)
    {
        // canonical form: hdl:xxxxx/yyyy
        String[] bits = canonical.split(":");

        if (bits.length != 2)
        {
            return null;
        }

        HandleType type = new HandleType();

        if (!type.getNamespace().equals(bits[0]))
        {
            return null;
        }

        Handle handle = new Handle(bits[1]);
        return handle;
    }
}
