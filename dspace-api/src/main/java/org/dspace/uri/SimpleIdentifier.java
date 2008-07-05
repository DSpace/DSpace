/*
 * SimpleIdentifier.java
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
package org.dspace.uri;

import java.util.UUID;

/**
 * Very simple entity class to form the basis of all other identifiers.  It
 * wraps a single unique identifier (UUID), and provides construction,
 * getters/setters and primitive implementations for canonical forms.
 *
 * Any identifier scheme which wishes to be backed by UUID should extend this class.
 *
 * Any object which requires a UUID but does not require that the UUID be
 * resolvable to the object in software should employ this class
 *
 * @author Richard Jones
 */
public class SimpleIdentifier
{
    /** the UUID backing this class */
    protected UUID uuid;

    /**
     * Construct a new SimpleIdentifier backed by the given UUID
     *
     * @param uuid
     */
    public SimpleIdentifier(UUID uuid)
    {
        this.uuid = uuid;
    }

    /**
     * Construct a new SimpleIdentifier backed by the given string representation of a UUID
     *
     * @param uuid
     */
    public SimpleIdentifier(String uuid)
    {
        this.uuid = UUID.fromString(uuid);
    }

    /**
     * Get the UUID which backs this SimpleIdentifier
     *
     * @return
     */
    public UUID getUUID()
    {
        return uuid;
    }

    /**
     * Get the canonical form of a UUID backed identifier.  This will be of the form:
     *
     * <code>urn:uuid:[the uuid]</code>
     *
     * @return
     */
    public String getCanonicalForm()
    {
        if (uuid == null)
        {
            return null;
        }
        return "urn:uuid:" + uuid.toString();
    }

    /**
     * Parse the given canonical form string into a SimpleIdentifier object.  The canonical
     * form used is:
     *
     * <code>uuid:[the uuid]</code>
     *
     * @param canonicalForm
     * @return
     */
    public static SimpleIdentifier parseCanonicalForm(String canonicalForm)
    {
        if (!canonicalForm.startsWith("urn:uuid:"))
        {
            return null;
        }

        String value = canonicalForm.substring(5);

        return new SimpleIdentifier(value);
    }
}
