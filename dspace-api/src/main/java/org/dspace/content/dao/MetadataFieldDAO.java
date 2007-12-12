/*
 * MetadataFieldDAO.java
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
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.dao.CRUD;

public abstract class MetadataFieldDAO extends ContentDAO<MetadataFieldDAO>
        implements CRUD<MetadataField>
{
    protected Logger log = Logger.getLogger(MetadataFieldDAO.class);

    protected Context context;

    protected MetadataFieldDAO childDAO;

    public MetadataFieldDAO(Context context)
    {
        this.context = context;
    }

    public MetadataFieldDAO getChild()
    {
        return childDAO;
    }

    public void setChild(MetadataFieldDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public MetadataField create() throws AuthorizeException
    {
        return childDAO.create();
    }

    public MetadataField retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public MetadataField retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    /**
     * Retrieves the metadata field from the database.
     *
     * @param schemaID schema by ID
     * @param element element name
     * @param qualifier qualifier (may be ANY or null)
     */
    public MetadataField retrieve(int schemaID, String element,
                                  String qualifier)
    {
        return childDAO.retrieve(schemaID, element, qualifier);
    }

    public void update(MetadataField field) throws AuthorizeException
    {
        childDAO.update(field);
    }

    public void delete(int id) throws AuthorizeException
    {
        childDAO.delete(id);
    }

    public boolean schemaChanged(MetadataField field)
    {
        return childDAO.schemaChanged(field);
    }

    /**
     * A sanity check that ensures a given element and qualifier are unique
     * within a given schema. The check happens in code as we cannot use a
     * database constraint.
     *
     * @param context dspace context
     * @param schemaID
     * @param element
     * @param qualifier
     * @return true if unique
     * @throws AuthorizeException
     * @throws IOException
     */
    protected boolean unique(int fieldID, int schemaID,
                             String element, String qualifier)
    {
        return childDAO.unique(fieldID, schemaID, element, qualifier);
    }

    public List<MetadataField> getMetadataFields()
    {
        return childDAO.getMetadataFields();
    }

    @Deprecated
    public List<MetadataField> getMetadataFields(int schemaID)
    {
        return childDAO.getMetadataFields(schemaID);
    }

    public List<MetadataField> getMetadataFields(MetadataSchema schema)
    {
        return childDAO.getMetadataFields(schema);
    }
}
