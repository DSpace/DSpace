/*
 * BitstreamFormatDAOPostgres.java
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
package org.dspace.content.dao.postgres;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.dao.BitstreamFormatDAO;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author James Rutherford
 */
public class BitstreamFormatDAOPostgres extends BitstreamFormatDAO
{
    public BitstreamFormatDAOPostgres(Context context)
    {
        super(context);
    }

    @Override
    public BitstreamFormat create() throws AuthorizeException
    {
        UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row =
                DatabaseManager.create(context, "bitstreamformatregistry");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("bitstream_format_id");
            BitstreamFormat bitstreamFormat = new BitstreamFormat(context, id);
            bitstreamFormat.setIdentifier(new ObjectIdentifier(uuid));

            return bitstreamFormat;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public BitstreamFormat retrieve(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context,
                    "bitstreamformatregistry", id);

            if (row == null)
            {
                log.debug("bitstream format " + id + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public BitstreamFormat retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "bitstreamformatregistry", "uuid", uuid.toString());

            if (row == null)
            {
                log.debug("bitstream format " + uuid + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public BitstreamFormat retrieveByShortDescription(String desc)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "bitstreamformatregistry", "short_description", desc);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public BitstreamFormat retrieveByMimeType(String mimeType)
    {
        try
        {
            // NOTE: Avoid internal formats since e.g. "License" also has
            // a MIMEtype of text/plain.
            TableRow row = DatabaseManager.querySingle(context,
                "SELECT * FROM bitstreamformatregistry " +
                "WHERE mimetype LIKE ? AND internal = '0'", mimeType);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(BitstreamFormat bitstreamFormat) throws AuthorizeException
    {
        try
        {
            TableRow row = DatabaseManager.find(context,
                    "bitstreamformatregistry", bitstreamFormat.getID());

            if (row != null)
            {
                // FIXME: Not only is this a totally stupid way of doing it, it's
                // also not very inheritance-friendly. It might be better to have
                // link() and unlink() for BitstreamFormats and file extensions.

                // Delete extensions
                DatabaseManager.updateQuery(context,
                        "DELETE FROM fileextension WHERE bitstream_format_id= ? ",
                        bitstreamFormat.getID());

                // Rewrite extensions
                for (String extension : bitstreamFormat.getExtensions())
                {
                    TableRow r = DatabaseManager.create(context, "fileextension");
                    r.setColumn("bitstream_format_id", bitstreamFormat.getID());
                    r.setColumn("extension", extension);
                    DatabaseManager.update(context, r);
                }

                populateTableRowFromBitstreamFormat(bitstreamFormat, row);
                DatabaseManager.update(context, row);
            }
            else
            {
                throw new RuntimeException("Didn't find bitstream format " +
                        bitstreamFormat.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        try
        {
            BitstreamFormat unknown = BitstreamFormat.findUnknown(context);

            // Set bitstreams with this format to "unknown"
            int numberChanged = DatabaseManager.updateQuery(context,
                    "UPDATE bitstream SET bitstream_format_id = ? " + 
                    "WHERE bitstream_format_id = ? ", 
                    unknown.getID(), id);

            // Delete extensions
            DatabaseManager.updateQuery(context,
                    "DELETE FROM fileextension WHERE bitstream_format_id= ? ",
                    id);

            log.info(LogManager.getHeader(context, "delete_bitstream_format",
                    "bitstream_format_id=" + id + ",bitstreams_changed="
                            + numberChanged));
            DatabaseManager.delete(context, "bitstreamformatregistry", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<BitstreamFormat> getBitstreamFormats()
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT bitstream_format_id " +
                    "FROM bitstreamformatregistry " +
                    "ORDER BY bitstream_format_id");
            
            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<BitstreamFormat> getBitstreamFormats(String extension)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                "SELECT bfr.bitstream_format_id " +
                "FROM bitstreamformatregistry bfr, fileextension fe " +
                "WHERE fe.extension LIKE ? " + 
                "AND bfr.bitstream_format_id = fe.bitstream_format_id",
                extension);

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<BitstreamFormat> getBitstreamFormats(boolean internal)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                "SELECT bitstream_format_id FROM bitstreamformatregistry " +
                "WHERE internal = '" + (internal ? "1" : "0") + "' " +
                "AND short_description NOT LIKE 'Unknown' " +
                "ORDER BY support_level DESC, short_description");

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private BitstreamFormat retrieve(TableRow row)
    {
        if (row == null)
        {
            return null;
        }

        int id = row.getIntColumn("bitstream_format_id");
        BitstreamFormat bitstreamFormat = new BitstreamFormat(context, id);
        populateBitstreamFormatFromTableRow(bitstreamFormat, row);

        return bitstreamFormat;
    }

    private List<BitstreamFormat> returnAsList(TableRowIterator tri)
            throws SQLException
    {
        List<BitstreamFormat> formats = new ArrayList<BitstreamFormat>();

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("bitstream_format_id");
            formats.add(retrieve(id));
        }

        return formats;
    }

    private void populateBitstreamFormatFromTableRow(BitstreamFormat
            bitstreamFormat, TableRow row)
    {
        UUID uuid = UUID.fromString(row.getStringColumn("uuid"));
        bitstreamFormat.setIdentifier(new ObjectIdentifier(uuid));

        List<String> extensions = new ArrayList<String>();

        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT extension FROM fileextension " +
                    "WHERE bitstream_format_id= ? ", bitstreamFormat.getID());

            for (TableRow extensionRow : tri.toList())
            {
                extensions.add(extensionRow.getStringColumn("extension"));
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

        bitstreamFormat.setExtensions(
                (String[]) extensions.toArray(new String[0]));
        bitstreamFormat.setMIMEType(row.getStringColumn("mimetype"));
        bitstreamFormat.setShortDescription(
            row.getStringColumn("short_description"));
        bitstreamFormat.setDescription(row.getStringColumn("description"));
        bitstreamFormat.setSupportLevel(row.getIntColumn("support_level"));
        bitstreamFormat.setInternal(row.getBooleanColumn("internal"));
    }

    private void populateTableRowFromBitstreamFormat(BitstreamFormat
            bitstreamFormat, TableRow row)
    {
        row.setColumn("mimetype", bitstreamFormat.getMIMEType());
        row.setColumn("short_description",
                bitstreamFormat.getShortDescription());
        row.setColumn("description", bitstreamFormat.getDescription());
        row.setColumn("support_level", bitstreamFormat.getSupportLevel());
        row.setColumn("internal", bitstreamFormat.isInternal());
    }
}

