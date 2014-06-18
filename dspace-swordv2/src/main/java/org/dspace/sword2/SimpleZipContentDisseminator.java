/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SimpleZipContentDisseminator implements SwordContentDisseminator
{
    public InputStream disseminate(Context context, Item item)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        try
        {
            // first write everything to a temp file
            String tempDir = ConfigurationManager.getProperty("upload.temp.dir");
            String fn = tempDir + File.separator + "SWORD." + item.getID() + "." + UUID.randomUUID().toString() + ".zip";
            OutputStream outStream = new FileOutputStream(new File(fn));
            ZipOutputStream zip = new ZipOutputStream(outStream);

            Bundle[] originals = item.getBundles("ORIGINAL");
            for (Bundle original : originals)
            {
                Bitstream[] bss = original.getBitstreams();
                for (Bitstream bitstream : bss)
                {
                    ZipEntry ze = new ZipEntry(bitstream.getName());
                    zip.putNextEntry(ze);
                    InputStream is = bitstream.retrieve();
                    Utils.copy(is, zip);
                    zip.closeEntry();
                    is.close();
                }
            }
            zip.close();

            return new TempFileInputStream(new File(fn));
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (IOException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (AuthorizeException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    public boolean disseminatesContentType(String contentType)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        return "application/zip".equals(contentType);
    }

    public boolean disseminatesPackage(String contentType)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        return UriRegistry.PACKAGE_SIMPLE_ZIP.equals(contentType);
    }

    public void setContentType(String contentType)
    {
        // this only does the one thing, so we can ignore this
    }

    public void setPackaging(String packaging)
    {
        // this only does the one thing, so we can ignore this
    }

    public String getContentType()
    {
        return "application/zip";
    }

    public String getPackaging()
    {
        return UriRegistry.PACKAGE_SIMPLE_ZIP;
    }

}
