/*
 * MetadataSchema.java
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
package org.dspace.content;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.content.dao.MetadataSchemaDAO;
import org.dspace.content.dao.MetadataSchemaDAOFactory;

/**
 * Class representing a schema in DSpace.
 * <p>
 * The schema object exposes a name which can later be used to generate
 * namespace prefixes in RDF or XML, e.g. the core DSpace Dublin Core schema
 * would have a name of <code>'dc'</code>.
 * </p>
 *
 * @author Martin Hald
 * @version $Revision$
 * @see org.dspace.content.MetadataValue, org.dspace.content.MetadataField
 */
public class MetadataSchema
{
    private static Logger log = Logger.getLogger(MetadataSchema.class);

    /** Numeric Identifier of built-in Dublin Core schema. */
    public static final int DC_SCHEMA_ID = 1;

    /** Short Name of built-in Dublin Core schema. */
    public static final String DC_SCHEMA = "dc";

    private Context context;
    private MetadataSchemaDAO dao;
    private int id;
    private String namespace;
    private String name;

    public MetadataSchema(Context context, int id)
    {
        this.context = context;
        this.id = id;

        dao = MetadataSchemaDAOFactory.getInstance(context);
    }

    public int getID()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    @Deprecated
    public void update(Context context) throws AuthorizeException
    {
        dao.update(this);
    }

    @Deprecated
    public void delete(Context context) throws AuthorizeException
    {
        dao.delete(getID());
    }

    @Deprecated
    public static MetadataSchema[] findAll(Context context)
    {
        MetadataSchemaDAO dao = MetadataSchemaDAOFactory.getInstance(context);
        List<MetadataSchema> schemas = dao.getMetadataSchemas();

        return (MetadataSchema[]) schemas.toArray(new MetadataSchema[0]);
    }

    @Deprecated
    public static MetadataSchema find(Context context, int id)
    {
        MetadataSchemaDAO dao = MetadataSchemaDAOFactory.getInstance(context);
        return dao.retrieve(id);
    }

    @Deprecated
    public static MetadataSchema find(Context context, String name)
    {
        MetadataSchemaDAO dao = MetadataSchemaDAOFactory.getInstance(context);
        return dao.retrieveByName(name);
    }

    @Deprecated
    public static MetadataSchema findByNamespace(Context context,
            String namespace)
    {
        MetadataSchemaDAO dao = MetadataSchemaDAOFactory.getInstance(context);
        return dao.retrieveByNamespace(namespace);
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
