/*
 * BitstreamDAOCore.java
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

import java.io.IOException;
import java.io.InputStream;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.uri.ObjectIdentifier;

/**
 * @author James Rutherford
 */
public class BitstreamDAOCore extends BitstreamDAO
{
    public BitstreamDAOCore(Context context)
    {
        super(context);
    }

    @Override
    public Bitstream store(InputStream is)
        throws AuthorizeException, IOException
    {
        Bitstream bs = create();
        BitstreamStorageManager.store(context, bs, is);

        return bs;
    }

    @Override
    public Bitstream register(int assetstore, String path)
        throws AuthorizeException, IOException
    {
        Bitstream bs = create();
        BitstreamStorageManager.register(context, bs, assetstore, path);

        return bs;
    }

    public Bitstream create() throws AuthorizeException
    {
        Bitstream bitstream = childDAO.create();

        // now assign an object identifier
        ObjectIdentifier oid = new ObjectIdentifier(true);
        bitstream.setIdentifier(oid);

        // FIXME: Think about this
        AuthorizeManager.addPolicy(
                context, bitstream, Constants.WRITE, context.getCurrentUser());

        log.info(LogManager.getHeader(context, "create_bitstream",
                "bitstream_id=" + bitstream.getID()));

        return bitstream;
    }

    @Override
    public Bitstream retrieve(int id)
    {
        Bitstream bitstream =
                (Bitstream) context.fromCache(Bitstream.class, id);

        if (bitstream == null)
        {
            bitstream = childDAO.retrieve(id);
        }

        return bitstream;
    }

    @Override
    public void update(Bitstream bitstream) throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, bitstream, Constants.WRITE);

        log.info(LogManager.getHeader(context, "update_bitstream",
                "bitstream_id=" + bitstream.getID()));

        // finally, deal with the bitstream identifier/uuid
        ObjectIdentifier oid = bitstream.getIdentifier();
        if (oid == null)
        {
            oid = new ObjectIdentifier(true);
            bitstream.setIdentifier(oid);
        }
        oidDAO.update(bitstream.getIdentifier());

        childDAO.update(bitstream);
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        Bitstream bitstream = retrieve(id);
        bitstream.setDeleted(true);

        log.info(LogManager.getHeader(context, "delete_bitstream",
                "bitstream_id=" + id));

        context.removeCached(bitstream, id);

        AuthorizeManager.removeAllPolicies(context, bitstream);

        // remove the object identifier
        oidDAO.delete(bitstream);

        childDAO.delete(id);
    }
    
    @Override
    public void remove(int id) throws AuthorizeException
    {
        Bitstream bitstream = retrieve(id);

        AuthorizeManager.authorizeAction(context, bitstream, Constants.REMOVE);
        // remove the object identifier
        oidDAO.delete(bitstream);

        childDAO.remove(id);
    }
}
