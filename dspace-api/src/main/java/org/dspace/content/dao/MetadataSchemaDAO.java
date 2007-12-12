/*
 * MetadataSchemaDAO.java
 *
 * Version: $Revision: 427 $
 *
 * Date: $Date: 2007-08-07 17:32:39 +0100 (Tue, 07 Aug 2007) $
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
package org.dspace.content.dao;

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.dao.CRUD;

public abstract class MetadataSchemaDAO extends ContentDAO<MetadataSchemaDAO>
        implements CRUD<MetadataSchema>
{
    protected Logger log = Logger.getLogger(MetadataSchemaDAO.class);

    protected Context context;

    protected MetadataSchemaDAO childDAO;

    public MetadataSchemaDAO(Context context)
    {
        this.context = context;
    }

    public MetadataSchemaDAO getChild()
    {
        return childDAO;
    }

    public void setChild(MetadataSchemaDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public MetadataSchema create() throws AuthorizeException
    {
        return childDAO.create();
    }

    public MetadataSchema retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public MetadataSchema retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    /**
     * Get the schema object corresponding to this short name (eg: dc).
     */
    public MetadataSchema retrieveByName(String name)
    {
        return childDAO.retrieveByName(name);
    }

    /**
     * Get the schema object corresponding to this namespace URI.
     */
    public MetadataSchema retrieveByNamespace(String namespace)
    {
        return childDAO.retrieveByNamespace(namespace);
    }

    public void update(MetadataSchema schema) throws AuthorizeException
    {
        childDAO.update(schema);
    }

    public void delete(int id) throws AuthorizeException
    {
        childDAO.delete(id);
    }

    /**
     * Return true if and only if the passed name appears within the allowed
     * number of times in the current schema.
     */
    protected boolean uniqueNamespace(int id, String namespace)
    {
        return childDAO.uniqueNamespace(id, namespace);
    }

    /**
     * Return true if and only if the passed name is unique.
     */
    protected boolean uniqueShortName(int id, String name)
    {
        return childDAO.uniqueShortName(id, name);
    }

    public List<MetadataSchema> getMetadataSchemas()
    {
        return childDAO.getMetadataSchemas();
    }
}

