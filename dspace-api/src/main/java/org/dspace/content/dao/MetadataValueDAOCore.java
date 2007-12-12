/*
 * MetadataValueDAO.java
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

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

public class MetadataValueDAOCore extends MetadataValueDAO
{
    public MetadataValueDAOCore(Context context)
    {
        super(context);
    }

    @Override
    public MetadataValue create() throws AuthorizeException
    {
        return childDAO.create();
    }

    @Override
    public MetadataValue retrieve(int id)
    {
        MetadataValue mv =
                (MetadataValue) context.fromCache(MetadataValue.class, id);

        if (mv == null)
        {
            mv = childDAO.retrieve(id);
        }

        return mv;
    }

    @Override
    public void update(MetadataValue value) throws AuthorizeException
    {
        log.info(LogManager.getHeader(context, "update_metadatavalue",
                    "metadata_value_id=" + value.getID()));

        childDAO.update(value);
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        log.info(LogManager.getHeader(context, "delete_metadata_value",
                    "metadata_value_id=" + id));

        childDAO.delete(id);
    }

    @Override
    public List<MetadataValue> getMetadataValues(int fieldID)
    {
        MetadataFieldDAO mfDAO = MetadataFieldDAOFactory.getInstance(context);
        return getMetadataValues(mfDAO.retrieve(fieldID));
    }
}
