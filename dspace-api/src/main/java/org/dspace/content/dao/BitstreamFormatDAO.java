/*
 * BitstreamFormatDAO.java
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
package org.dspace.content.dao;

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.content.BitstreamFormat;
import org.dspace.dao.CRUD;

/**
 * @author James Rutherford
 */
public abstract class BitstreamFormatDAO extends ContentDAO<BitstreamFormatDAO>
        implements CRUD<BitstreamFormat>
{
    protected Logger log = Logger.getLogger(BitstreamFormatDAO.class);

    protected Context context;

    protected BitstreamFormatDAO childDAO;

    public BitstreamFormatDAO(Context context)
    {
        this.context = context;
    }

    public BitstreamFormatDAO getChild()
    {
        return childDAO;
    }

    public void setChild(BitstreamFormatDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public BitstreamFormat create() throws AuthorizeException
    {
        return childDAO.create();
    }

    public BitstreamFormat retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public BitstreamFormat retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public BitstreamFormat retrieveByMimeType(String mimeType)
    {
        return childDAO.retrieveByMimeType(mimeType);
    }

    public BitstreamFormat retrieveByShortDescription(String desc)
    {
        return childDAO.retrieveByShortDescription(desc);
    }

    public void update(BitstreamFormat bitstreamFormat)
            throws AuthorizeException
    {
        childDAO.update(bitstreamFormat);
    }

    /**
     * Delete this bitstream format. This converts the types of any bitstreams
     * that may have this type to "unknown". Use this with care!
     */
    public void delete(int id) throws AuthorizeException
    {
        childDAO.delete(id);
    }

    public List<BitstreamFormat> getBitstreamFormats()
    {
        return childDAO.getBitstreamFormats();
    }

    public List<BitstreamFormat> getBitstreamFormats(String extension)
    {
        return childDAO.getBitstreamFormats(extension);
    }

    /**
     * Retrieve all non-internal bitstream formats from the registry. The
     * "unknown" format is not included, and the formats are ordered by support
     * level (highest first) first then short description.
     */
    public List<BitstreamFormat> getBitstreamFormats(boolean internal)
    {
        return childDAO.getBitstreamFormats(internal);
    }
}
