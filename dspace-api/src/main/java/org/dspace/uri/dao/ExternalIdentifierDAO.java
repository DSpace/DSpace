/*
 * ExternalIdentifierDAO.java
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
package org.dspace.uri.dao;

import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ExternalIdentifierType;
import org.dspace.uri.IdentifierException;
import org.dspace.uri.dao.postgres.ExternalIdentifierDAOPostgres;
import org.dspace.core.Context;

/**
 * Abstract parent class to be extended by any DAO wishing to store ExternalIdentifier objects
 *
 * @author Richard Jones
 */
public abstract class ExternalIdentifierDAO
{
    /** DSpace context */
    protected Context context;

    /**
     * Construct a new ExternalIdentifierDAO with the given DSpace context
     *
     * @param context
     */
    public ExternalIdentifierDAO(Context context)
    {
        this.context = context;
    }

    /**
     * Create a persistent record of the given ExternalIdentifier to the given DSpaceObject
     *
     * @param eid
     */
    public abstract void create(ExternalIdentifier eid) throws ExternalIdentifierStorageException;

    /**
     * Retrieve all ExternalIdentifier objects associated with the given DSpaceObject
     *
     * @param dso
     * @return
     */
    public abstract List<ExternalIdentifier> retrieve(DSpaceObject dso) throws ExternalIdentifierStorageException, IdentifierException;

    /**
     * Retrieve an ExternalIdentifier object (if one exists) for the given identifier
     * type and value
     *
     * @param type
     * @param value
     * @return
     */
    public abstract ExternalIdentifier retrieve(ExternalIdentifierType type, String value) throws ExternalIdentifierStorageException;

    /**
     * Update the record of the given ExternalIdentifier
     *
     * @param eid
     */
    public abstract void update(ExternalIdentifier eid) throws ExternalIdentifierStorageException;

    /**
     * Delete all record of the given ExternalIdentifier
     *
     * @param eid
     */
    public abstract void delete(ExternalIdentifier eid) throws ExternalIdentifierStorageException;

    /**
     * Create a tombstone record for the given external identifier
     *
     * @param eid
     */
    public abstract void tombstone(ExternalIdentifier eid) throws ExternalIdentifierStorageException;

    /**
     * Retrieve all ExternalIdentifiers of the given type whose values start with the given
     * string fragment
     * 
     * @param type
     * @param startsWith
     * @return
     */
    public abstract List<ExternalIdentifier> startsWith(ExternalIdentifierType type, String startsWith) throws ExternalIdentifierStorageException;
}
