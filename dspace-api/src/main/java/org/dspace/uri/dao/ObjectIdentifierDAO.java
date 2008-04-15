/*
 * ObjectIdentifierDAO.java
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
package org.dspace.uri.dao;

import org.dspace.core.Context;
import org.dspace.uri.ObjectIdentifier;

import java.util.UUID;

/**
 * Abstract parent class to be extended by any DAO wishing to store ObjectIdentifier objects
 *
 * @author Richard Jones
 */
public abstract class ObjectIdentifierDAO
{
    /** the DSpace context */
    protected Context context;

    /**
     * Construct a new ObjectIdentifierDAO with the given DSpace context
     * @param context
     */
    public ObjectIdentifierDAO(Context context)
    {
        this.context = context;
    }

    /**
     * Create a persistent record of the given ObjectIdentifier
     *
     * @param oid
     */
    public abstract void create(ObjectIdentifier oid) throws ObjectIdentifierStorageException;

    /**
     * Retrieve the ObjectIdentifier associated with the given DSpaceObject
     *
     * @param uuid
     * @return
     */
    public abstract ObjectIdentifier retrieve(UUID uuid) throws ObjectIdentifierStorageException;

    /**
     * Retrieve the ObjectIdentifier associated with the given DSpace object type and
     * storage layer id
     *
     * @param type
     * @param id
     * @return
     */
    public abstract ObjectIdentifier retrieve(int type, int id) throws ObjectIdentifierStorageException;

    /**
     * Update the record of the given ObjectIdentifier
     *
     * @param oid
     */
    public abstract void update(ObjectIdentifier oid) throws ObjectIdentifierStorageException;

    /**
     * Delete all record of the given ObjectIdentifier
     *
     * @param oid
     */
    public abstract void delete(ObjectIdentifier oid) throws ObjectIdentifierStorageException;
}
