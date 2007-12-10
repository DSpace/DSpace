/*
 * MetadataFieldDAOCore.java
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

public class MetadataFieldDAOCore extends MetadataFieldDAO
{
    public MetadataFieldDAOCore(Context context)
    {
        super(context);
    }

    @Override
    public MetadataField create() throws AuthorizeException
    {
        // Check authorisation: Only admins may create DC types
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        MetadataField field = childDAO.create();

        int id = field.getID();
        int schemaID = field.getSchemaID();
        String element = field.getElement();
        String qualifier = field.getQualifier();

        // Ensure the element and qualifier are unique within a given schema.
        if (!unique(id, schemaID, element, qualifier))
        {
            throw new RuntimeException(
                    new NonUniqueMetadataException("Please make " + element +
                        "." + qualifier + " unique within schema #" +
                        schemaID));
        }

        log.info(LogManager.getHeader(context, "create_metadata_field",
                    "metadata_field_id=" + id));

        return field;
    }

    @Override
    public MetadataField retrieve(int id)
    {
        MetadataField field =
                (MetadataField) context.fromCache(MetadataField.class, id);

        if (field == null)
        {
            field = childDAO.retrieve(id);
        }

        return field;
    }

    @Override
    public void update(MetadataField field) throws AuthorizeException
    {
        int id = field.getID();
        int schemaID = field.getSchemaID();
        String element = field.getElement();
        String qualifier = field.getQualifier();

        // Check authorisation: Only admins may update the metadata registry
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modiffy the metadata registry");
        }

        // Check to see if the field ID was altered. If is was then we will
        // query to ensure that there is not already a duplicate name field.
        if (schemaChanged(field))
        {
            if (hasElement(id, element, qualifier))
            {
                throw new RuntimeException(
                        new NonUniqueMetadataException(
                            "Duplcate field name found in target field"));
            }
        }

        // Ensure the element and qualifier are unique within a given field.
        if (!unique(id, schemaID, element, qualifier))
        {
            throw new RuntimeException(
                new NonUniqueMetadataException("Please make " + element + "." +
                    qualifier + " unique"));
        }

        log.info(LogManager.getHeader(context, "update_metadatafieldregistry",
                "metadata_field_id=" + id +
                "element=" + element +
                "qualifier=" + qualifier));

        childDAO.update(field);
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        MetadataField field = retrieve(id);
        update(field); // Sync in-memory object before removal

        // Check authorisation: Only admins may delete metadata fields
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        log.info(LogManager.getHeader(context, "delete_metadata_field",
                "metadata_field_id=" + id));

        context.removeCached(field, id);

        childDAO.delete(id);
    }

    @Override
    public List<MetadataField> getMetadataFields(int schemaID)
    {
        MetadataSchemaDAO msDAO = MetadataSchemaDAOFactory.getInstance(context);
        return getMetadataFields(msDAO.retrieve(schemaID));
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    /**
     * Return true if and only if the schema has a field with the given element
     * and qualifier pair.
     *
     * @param context dspace context
     * @param schemaID schema by ID
     * @param element element name
     * @param qualifier qualifier name
     * @return true if the field exists
     */
    private boolean hasElement(int schemaID, String element, String qualifier)
    {
        return retrieve(schemaID, element, qualifier) != null;
    }
}
