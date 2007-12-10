/*
 * MetadataValueDAO.java
 *
 * Version: : $
 *
 * Date: : $
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.storage.dao.CRUD;

public abstract class MetadataValueDAO extends ContentDAO<MetadataValueDAO>
        implements CRUD<MetadataValue>
{
    protected Logger log = Logger.getLogger(MetadataValueDAO.class);

    protected Context context;

    protected MetadataValueDAO childDAO;

    public MetadataValueDAO(Context context)
    {
        this.context = context;
    }

    public MetadataValueDAO getChild()
    {
        return childDAO;
    }

    public void setChild(MetadataValueDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public MetadataValue create() throws AuthorizeException
    {
        return childDAO.create();
    }

    public MetadataValue retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public MetadataValue retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public void update(MetadataValue value) throws AuthorizeException
    {
        childDAO.update(value);
    }

    public void delete(int id) throws AuthorizeException
    {
        childDAO.delete(id);
    }

    @Deprecated
    public List<MetadataValue> getMetadataValues(int fieldID)
    {
        return childDAO.getMetadataValues(fieldID);
    }

    public List<MetadataValue> getMetadataValues(MetadataField field)
    {
        return childDAO.getMetadataValues(field);
    }

    public List<MetadataValue> getMetadataValues(MetadataField field,
                                                 String value)
    {
        return childDAO.getMetadataValues(field, value);
    }

    public List<MetadataValue> getMetadataValues(MetadataField field,
                                                 String value, String language)
    {
        return childDAO.getMetadataValues(field, value, language);
    }

    public List<MetadataValue> getMetadataValues(Item item)
    {
        return childDAO.getMetadataValues(item);
    }
}
