/*
 * PackageUtils.java
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

package org.dspace.content.packager;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.license.CreativeCommons;

/**
 * Container class for code that is useful to many packagers.
 *
 * @author Larry Stone
 * @version $Revision$
 */

public class PackageUtils
{
    /**
     * Test that item has adequate metadata.
     * Check item for the minimal DC metadata required to ingest a
     * new item, and throw a PackageValidationException if test fails.
     * Used by all SIP processors as a common sanity test.
     *
     * @param item - item to test.
     */
    public static void checkMetadata(Item item)
        throws PackageValidationException
    {
        DCValue t[] = item.getDC( "title", null, Item.ANY);
        if (t == null || t.length == 0)
            throw new PackageValidationException("Item cannot be created without the required \"title\" DC metadata.");
    }

    /**
     * Add DSpace Deposit License to an Item.
     * Utility function to add the a user-supplied deposit license or
     * a default one if none was given; creates new bitstream in the
     * "LICENSE" bundle and gives it the special license bitstream format.
     *
     * @param context - dspace context
     * @param license - license string to add, may be null to invoke default.
     * @param item - the item.
     * @param collection - get the default license from here.
     */
    public static void addDepositLicense(Context context, String license,
                                       Item item, Collection collection)
        throws SQLException, IOException, AuthorizeException
    {
        if (license == null)
            license = collection.getLicense();
        InputStream lis = new ByteArrayInputStream(license.getBytes());
        Bundle lb = item.createBundle(Constants.LICENSE_BUNDLE_NAME);
        Bitstream lbs = lb.createBitstream(lis);
        lis.close();
        BitstreamFormat bf = BitstreamFormat.findByShortDescription(context, "License");
        if (bf == null)
            bf = FormatIdentifier.guessFormat(context, lbs);
        lbs.setFormat(bf);
        lbs.setName(Constants.LICENSE_BITSTREAM_NAME);
        lbs.setSource(Constants.LICENSE_BITSTREAM_NAME);
        lbs.update();
    }

    /**
     * Find bitstream by its Name, looking in all bundles.
     *
     * @param item Item whose bitstreams to search.
     * @param name Bitstream's name to match.
     * @return first bitstream found or null.
     */
    public static Bitstream getBitstreamByName(Item item, String name)
        throws SQLException
    {
        return getBitstreamByName(item, name, null);
    }

    /**
     * Find bitstream by its Name, looking in specific named bundle.
     *
     * @param item - dspace item whose bundles to search.
     * @param bsName - name of bitstream to match.
     * @param bnName - bundle name to match, or null for all.
     * @return the format found or null if none found.
     */
    public static Bitstream getBitstreamByName(Item item, String bsName, String bnName)
        throws SQLException
    {
        Bundle[] bundles;
        if (bnName == null)
            bundles = item.getBundles();
        else
            bundles = item.getBundles(bnName);
        for (int i = 0; i < bundles.length; i++)
        {
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            for (int k = 0; k < bitstreams.length; k++)
            {
                if (bsName.equals(bitstreams[k].getName()))
                    return bitstreams[k];
            }
        }
        return null;
    }

    /**
     * Find bitstream by its format, looking in a specific bundle.
     * Used to look for particularly-typed Package Manifest bitstreams.
     *
     * @param item - dspace item whose bundles to search.
     * @param bsf - BitstreamFormat object to match.
     * @param bnName - bundle name to match, or null for all.
     * @return the format found or null if none found.
     */
    public static Bitstream getBitstreamByFormat(Item item,
            BitstreamFormat bsf, String bnName)
        throws SQLException
    {
        int fid = bsf.getID();
        Bundle[] bundles;
        if (bnName == null)
            bundles = item.getBundles();
        else
            bundles = item.getBundles(bnName);
        for (int i = 0; i < bundles.length; i++)
        {
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            for (int k = 0; k < bitstreams.length; k++)
            {
                if (bitstreams[k].getFormat().getID() == fid)
                    return bitstreams[k];
            }
        }
        return null;
    }

    /**
     * Predicate, does this bundle container meta-information.  I.e.
     * does this bundle contain descriptive metadata or other metadata
     * such as license bitstreams?  If so we probablly don't want to put
     * it into the "content" section of a package; hence this predicate.
     *
     * @param bn -- the bundle
     * @return true if this bundle name indicates it is a meta-info bundle.
     */
    public static boolean isMetaInfoBundle(Bundle bn)
    {
        return (bn.getName().equals(Constants.LICENSE_BUNDLE_NAME) ||
                bn.getName().equals(CreativeCommons.CC_BUNDLE_NAME) ||
                bn.getName().equals(Constants.METADATA_BUNDLE_NAME));
    }

    /**
     * Stream wrapper that does not allow its wrapped stream to be
     * closed.  This is needed to work around problem when loading
     * bitstreams from ZipInputStream.  The Bitstream constructor
     * invokes close() on the input stream, which would prematurely end
     * the ZipInputStream.
     * Example:
     * <pre>
     *      ZipEntry ze = zip.getNextEntry();
     *      Bitstream bs = bundle.createBitstream(new PackageUtils.UnclosableInputStream(zipInput));
     * </pre>
     */
    public static class UnclosableInputStream extends FilterInputStream
    {
        public UnclosableInputStream(InputStream in)
        {
            super(in);
        }

        /**
         * Do nothing, to prevent wrapped stream from being closed prematurely.
         */
        public void close()
        {
        }
    }

    /**
     * Find or create a bitstream format to match the given short
     * description.
     * Used by packager ingesters to obtain a special bitstream
     * format for the manifest (and/or metadata) file.
     * <p>
     * NOTE: When creating a new format, do NOT set any extensions, since
     *  we don't want any file with the same extension, which may be something
     *  generic like ".xml", to accidentally get set to this format.
     * @param context - the context.
     * @param shortDesc - short descriptive name, used to locate existing format.
     * @param MIMEtype - mime content-type
     * @param desc - long description
     * @return BitstreamFormat object that was found or created.  Never null.
     */
     public static BitstreamFormat findOrCreateBitstreamFormat(Context context,
            String shortDesc, String MIMEType, String desc)
        throws SQLException, AuthorizeException
     {
        BitstreamFormat bsf = BitstreamFormat.findByShortDescription(context,
                                shortDesc);
        // not found, try to create one
        if (bsf == null)
        {
            bsf = BitstreamFormat.create(context);
            bsf.setShortDescription(shortDesc);
            bsf.setMIMEType(MIMEType);
            bsf.setDescription(desc);
            bsf.setSupportLevel(BitstreamFormat.KNOWN);
            bsf.update();
        }
        return bsf;
    }
}
