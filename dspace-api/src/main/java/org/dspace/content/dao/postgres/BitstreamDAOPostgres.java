/*
 * BitstreamDAOPostgres.java
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
import java.io.InputStream;
import java.io.IOException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.UnsupportedIdentifierException;
import org.dspace.uri.IdentifierException;
import org.dspace.uri.dao.ExternalIdentifierStorageException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author James Rutherford
 * @author Richard Jones
 */
public class BitstreamDAOPostgres extends BitstreamDAO
{
    public BitstreamDAOPostgres(Context context)
    {
        super(context);
    }

    @Override
    public Bitstream create() throws AuthorizeException
    {
        // UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row = DatabaseManager.create(context, "bitstream");
            // row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("bitstream_id");
            Bitstream bitstream = new Bitstream(context, id);
            // bitstream.setIdentifier(new ObjectIdentifier(uuid));

            return bitstream;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Bitstream retrieve(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context, "bitstream", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Bitstream retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context, "bitstream",
                    "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(Bitstream bitstream) throws AuthorizeException
    {
        try
        {
            TableRow row =
                DatabaseManager.find(context, "bitstream", bitstream.getID());

            if (row != null)
            {
                update(bitstream, row);
            }
            else
            {
                throw new RuntimeException("Didn't find bitstream " +
                        bitstream.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private void update(Bitstream bitstream, TableRow row)
        throws AuthorizeException
    {
        try
        {
            populateTableRowFromBitstream(bitstream, row);

            DatabaseManager.update(context, row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        boolean oracle = false;
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            oracle = true;
        }

        try
        {
            // Remove references to primary bitstreams in bundle
            String query =
                "UPDATE bundle SET primary_bitstream_id = " +
                (oracle ? "''" : "Null") +
                " WHERE primary_bitstream_id = ? ";

            DatabaseManager.updateQuery(context, query, id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * FIXME: This BADLY needs some sanity checking.
     */
    @Override
    public void remove(int id) throws AuthorizeException
    {
        try
        {
            DatabaseManager.delete(context, "bitstream", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Bitstream> getBitstreamsByBundle(Bundle bundle)
    {
        try
        {
            // Get bitstreams
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT bitstream_id FROM bundle2bitstream " +
                    "WHERE bundle_id = ? ", bundle.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Bitstream> getDeletedBitstreams()
    {
        try
        {
            // Get bitstreams
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT bitstream_id FROM bitstream " +
                    "WHERE deleted = '1'");

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

    private Bitstream retrieve(TableRow row)
    {
        try
        {
            if (row == null)
            {
                return null;
            }

            int id = row.getIntColumn("bitstream_id");
            Bitstream bitstream = new Bitstream(context, id);
            populateBitstreamFromTableRow(bitstream, row);

            // FIXME: I'd like to bump the rest of this up into the superclass
            // so we don't have to do it for every implementation, but I can't
            // figure out a clean way of doing this yet.
            List<ExternalIdentifier> identifiers = identifierDAO.retrieve(bitstream);
            bitstream.setExternalIdentifiers(identifiers);

            return bitstream;
        }
        catch (UnsupportedIdentifierException e)
        {
            log.error("caught exception: ", e);
            throw new RuntimeException(e);
        }
        catch (ExternalIdentifierStorageException e)
        {
            log.error("caught exception: ", e);
            throw new RuntimeException(e);
        }
        catch (IdentifierException e)
        {
            log.error("caught exception: ", e);
            throw new RuntimeException(e);
        }
    }

    private List<Bitstream> returnAsList(TableRowIterator tri)
            throws SQLException
    {
        List <Bitstream> bitstreams = new ArrayList<Bitstream>();

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("bitstream_id");
            bitstreams.add(retrieve(id));
        }

        return bitstreams;
    }

    private void populateBitstreamFromTableRow(Bitstream bitstream,
            TableRow row)
    {
        int sequenceID = row.getIntColumn("sequence_id");
        int storeNumber = row.getIntColumn("store_number");
        String internalID = row.getStringColumn("internal_id");

        String name = row.getStringColumn("name");
        String source = row.getStringColumn("source");
        String description = row.getStringColumn("description");
        String checksum = row.getStringColumn("checksum");
        String checksumAlgorithm = row.getStringColumn("checksum_algorithm");
        String userFormatDescription =
            row.getStringColumn("user_format_description");
        boolean deleted = row.getBooleanColumn("deleted");

        long sizeBytes = -1l;
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            sizeBytes = new Long(row.getIntColumn("size_bytes"));
        }
        else
        {
            sizeBytes = row.getLongColumn("size_bytes");
        }

        int bitstreamFormatID = row.getIntColumn("bitstream_format_id");
        BitstreamFormat bitstreamFormat = null;

        if (bitstreamFormatID > 0)
        {
            bitstreamFormat = BitstreamFormat.find(context, bitstreamFormatID);
        }

        bitstream.setSequenceID(sequenceID);
        bitstream.setName(name);
        bitstream.setSource(source);
        bitstream.setDescription(description);
        bitstream.setChecksum(checksum);
        bitstream.setChecksumAlgorithm(checksumAlgorithm);
        bitstream.setSize(sizeBytes);
        bitstream.setUserFormatDescription(userFormatDescription);
        bitstream.setFormat(bitstreamFormat);
        bitstream.setStoreNumber(storeNumber);
        bitstream.setInternalID(internalID);
        bitstream.setDeleted(deleted);

        UUID uuid = UUID.fromString(row.getStringColumn("uuid"));
        bitstream.setIdentifier(new ObjectIdentifier(uuid));
    }

    private void populateTableRowFromBitstream(Bitstream bitstream,
            TableRow row)
    {
        BitstreamFormat bitstreamFormat = bitstream.getFormat();
        int sequenceID = bitstream.getSequenceID();
        int storeNumber = bitstream.getStoreNumber();
        long sizeBytes = bitstream.getSize();

        String name = bitstream.getName();
        String source = bitstream.getSource();
        String description = bitstream.getDescription();
        String checksum = bitstream.getChecksum();
        String checksumAlgorithm = bitstream.getChecksumAlgorithm();
        String userFormatDescription = bitstream.getUserFormatDescription();
        String internalID = bitstream.getInternalID();
        boolean deleted = bitstream.isDeleted();

        // FIXME: I'm not 100% sure this is correct
        if (bitstreamFormat == null)
        {
            // No format: use "Unknown"
            bitstreamFormat = BitstreamFormat.findUnknown(context);

            // Panic if we can't find it
            if (bitstreamFormat == null)
            {
                throw new IllegalStateException("No Unknown bitsream format");
            }
        }

        row.setColumn("uuid", bitstream.getIdentifier().getUUID().toString());

        row.setColumn("sequence_id", sequenceID);
        row.setColumn("store_number", storeNumber);
        row.setColumn("bitstream_format_id", bitstreamFormat.getID());
        row.setColumn("size_bytes", sizeBytes);

        row.setColumn("name", name);
        row.setColumn("source", source);
        row.setColumn("internal_id", internalID);
        row.setColumn("deleted", deleted);

        if (description == null)
        {
            row.setColumnNull("description");
        }
        else
        {
            row.setColumn("description", description);
        }

        if (checksum == null)
        {
            row.setColumnNull("checksum");
        }
        else
        {
            row.setColumn("checksum", checksum);
        }

        if (checksumAlgorithm == null)
        {
            row.setColumnNull("checksum_algorithm");
        }
        else
        {
            row.setColumn("checksum_algorithm", checksumAlgorithm);
        }

        if (userFormatDescription == null)
        {
            row.setColumnNull("user_format_description");
        }
        else
        {
            row.setColumn("user_format_description", userFormatDescription);
        }
    }
}
