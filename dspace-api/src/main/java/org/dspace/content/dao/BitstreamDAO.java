/*
 * BitstreamDAO.java
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

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.core.Context;
import org.dspace.dao.CRUD;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;
import org.dspace.uri.dao.ObjectIdentifierDAO;
import org.dspace.uri.dao.ObjectIdentifierDAOFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * @author James Rutherford
 */
public abstract class BitstreamDAO extends ContentDAO<BitstreamDAO>
        implements CRUD<Bitstream>
{
    protected Logger log = Logger.getLogger(BitstreamDAO.class);

    protected Context context;
    protected ExternalIdentifierDAO identifierDAO;
    protected ObjectIdentifierDAO oidDAO;

    protected BitstreamDAO childDAO;

    public BitstreamDAO(Context context)
    {
        this.context = context;

        identifierDAO = ExternalIdentifierDAOFactory.getInstance(context);
        oidDAO = ObjectIdentifierDAOFactory.getInstance(context);
    }

    public BitstreamDAO getChild()
    {
        return childDAO;
    }

    public void setChild(BitstreamDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public Bitstream create() throws AuthorizeException
    {
        return childDAO.create();
    }

    /**
     * Create a new bitstream, with a new ID. The checksum and file size are
     * calculated. This method does not check authorisation; other methods such
     * as Bundle.createBitstream() will check authorisation. The newly created
     * bitstream has the "unknown" format.
     *
     * @param is the bits to put in the bitstream
     *
     * @return the newly created bitstream
     * @throws AuthorizeException
     */
    public Bitstream store(InputStream is)
            throws AuthorizeException, IOException
    {
        return childDAO.store(is);
    }

    /**
     * Register a new bitstream, with a new ID. The checksum and file size are
     * calculated. This method does not check authorisation; other methods such
     * as Bundle.createBitstream() will check authorisation. The newly
     * registered bitstream has the "unknown" format.
     *
     *
     * @return the newly created bitstream
     * @throws AuthorizeException
     */
    public Bitstream register(int assetstore, String path)
            throws AuthorizeException, IOException
    {
        return childDAO.register(assetstore, path);
    }

    public Bitstream retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public Bitstream retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public void update(Bitstream bitstream) throws AuthorizeException
    {
        childDAO.update(bitstream);
    }

    /**
     * Mark the bitstream as deleted. Actual removal doesn't happen until a
     * cleanup happens, and remove() is called.
     */
    public void delete(int id) throws AuthorizeException
    {
        childDAO.delete(id);
    }

    /**
     * Actually remove the reference to the bitstream. Note that this doesn't
     * do anything to the actual files, just their representation in the
     * system.
     */
    public void remove(int id) throws AuthorizeException
    {
        childDAO.remove(id);
    }

    public List<Bitstream> getBitstreamsByBundle(Bundle bundle)
    {
        return childDAO.getBitstreamsByBundle(bundle);
    }

    public List<Bitstream> getDeletedBitstreams()
    {
        return childDAO.getDeletedBitstreams();
    }
}
