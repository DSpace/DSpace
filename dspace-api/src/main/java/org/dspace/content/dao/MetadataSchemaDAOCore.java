/*
 * MetadataSchemaDAOCore.java
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

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

public class MetadataSchemaDAOCore extends MetadataSchemaDAO
{
    public MetadataSchemaDAOCore(Context context)
    {
        super(context);
    }

    @Override
    public MetadataSchema create() throws AuthorizeException
    {
        // Check authorisation: Only admins may create metadata schemas
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        MetadataSchema schema = childDAO.create();

        log.info(LogManager.getHeader(context, "create_metadata_schema",
                    "metadata_schema_id=" + schema.getID()));

        return schema;
    }

    @Override
    public MetadataSchema retrieve(int id)
    {
        MetadataSchema schema =
                (MetadataSchema) context.fromCache(MetadataSchema.class, id);

        if (schema == null)
        {
            schema = childDAO.retrieve(id);
        }

        return schema;
    }

    @Override
    public void update(MetadataSchema schema) throws AuthorizeException
    {
        // Check authorisation: Only admins may create metadata schemas
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        int id = schema.getID();
        String name = schema.getName();
        String namespace = schema.getNamespace();

        // Ensure the schema name is unique
        if (!uniqueShortName(id, name))
        {
            throw new RuntimeException(
                    new NonUniqueMetadataException("Please make the name " + name
                    + " unique"));
        }

        // Ensure the schema namespace is unique
        if (!uniqueNamespace(id, namespace))
        {
            throw new RuntimeException(
                    new NonUniqueMetadataException("Please make the namespace "
                        + namespace + " unique"));
        }

        log.info(LogManager.getHeader(context, "update_metadata_schema",
                    "metadata_schema_id=" + id +
                    "namespace=" + namespace +
                    "name=" + name));

        childDAO.update(schema);
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        MetadataSchema schema = retrieve(id);

        log.info(LogManager.getHeader(context, "delete_metadata_schema",
                "metadata_schema_id=" + id));

        context.removeCached(schema, id);

        MetadataFieldDAO dao = MetadataFieldDAOFactory.getInstance(context);
        for (MetadataField field : dao.getMetadataFields(schema))
        {
            dao.delete(field.getID());
        }

        childDAO.delete(id);
    }
}
