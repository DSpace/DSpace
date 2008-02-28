/*
 * ItemService.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2008/01/01 04:11:09 $
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

package org.dspace.content.service;

import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Thumbnail;
import org.dspace.core.Context;

import java.sql.SQLException;

public class ItemService
{
    public static Thumbnail getThumbnail(Context context, int itemId, boolean requireOriginal) throws SQLException
    {
        ItemDAO dao = ItemDAOFactory.getInstance(context);

        Bitstream thumbBitstream = null;
        Bitstream primaryBitstream = dao.getPrimaryBitstream(itemId, "ORIGINAL");
        if (primaryBitstream != null)
        {
            if (primaryBitstream.getFormat().getMIMEType().equals("text/html"))
                return null;

            thumbBitstream = dao.getNamedBitstream(itemId, "THUMBNAIL", primaryBitstream.getName() + ".jpg");
        }
        else
        {
            if (requireOriginal)
                primaryBitstream = dao.getFirstBitstream(itemId, "ORIGINAL");
            
            thumbBitstream   = dao.getFirstBitstream(itemId, "THUMBNAIL");
        }

        if (thumbBitstream != null)
            return new Thumbnail(thumbBitstream, primaryBitstream);

        return null;
    }
}
