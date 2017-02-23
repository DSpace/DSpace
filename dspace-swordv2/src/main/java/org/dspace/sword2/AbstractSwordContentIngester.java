/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

public abstract class AbstractSwordContentIngester
        implements SwordContentIngester
{
    public static final Logger log = Logger.getLogger(
        AbstractSwordContentIngester.class);

    protected BitstreamFormatService bitstreamFormatService =
        ContentServiceFactory.getInstance().getBitstreamFormatService();

    protected ItemService itemService =
        ContentServiceFactory.getInstance().getItemService();

    public DepositResult ingest(Context context, Deposit deposit,
            DSpaceObject dso, VerboseDescription verboseDescription)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        return this.ingest(context, deposit, dso, verboseDescription, null);
    }

    public DepositResult ingest(Context context, Deposit deposit,
            DSpaceObject dso, VerboseDescription verboseDescription,
            DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        if (dso instanceof Collection)
        {
            return this.ingestToCollection(context, deposit, (Collection) dso,
                verboseDescription, result);
        }
        else if (dso instanceof Item)
        {
            return this.ingestToItem(context, deposit, (Item) dso,
                verboseDescription, result);
        }
        return null;
    }

    public abstract DepositResult ingestToCollection(Context context,
            Deposit deposit, Collection collection,
            VerboseDescription verboseDescription, DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException;

    public abstract DepositResult ingestToItem(Context context, Deposit deposit,
            Item item, VerboseDescription verboseDescription,
            DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException;

    protected BitstreamFormat getFormat(Context context, String fileName)
            throws SQLException
    {
        String fext = null;
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot > -1)
        {
            fext = fileName.substring(lastDot + 1);
        }

        if (fext == null)
        {
            return null;
        }

        List<BitstreamFormat> formats = bitstreamFormatService.findAll(context);
        for (BitstreamFormat format : formats)
        {
            List<String> extensions = format.getExtensions();
            for (String ext : extensions)
            {
                if (ext.equals(fext))
                {
                    return format;
                }
            }
        }
        return null;
    }

    /**
     * Add the current date to the item metadata.  This looks up
     * the field in which to store this metadata in the configuration
     * sword.updated.field
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     target item
     * @param verboseDescription
     *     The description.
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    protected void setUpdatedDate(Context context, Item item,
            VerboseDescription verboseDescription)
            throws DSpaceSwordException
    {
        String field = ConfigurationManager
            .getProperty("swordv2-server", "updated.field");
        if (field == null || "".equals(field))
        {
            throw new DSpaceSwordException(
                "No configuration, or configuration is invalid for: sword.updated.field");
        }

        MetadataFieldInfo info = this.configToDC(field, null);
        try
        {
            itemService.clearMetadata(context, item, info.schema, info.element,
                info.qualifier, Item.ANY);
            DCDate date = new DCDate(new Date());
            itemService.addMetadata(context, item, info.schema, info.element,
                info.qualifier, null, date.toString());
        }
        catch (SQLException e)
        {
            log.error("Caught exception trying to set update date", e);
            throw new DSpaceSwordException(e);
        }

        verboseDescription.append(
            "Updated date added to response from item metadata where available");
    }

    /**
     * Store the given slug value (which is used for suggested identifiers,
     * and which DSpace ignores) in the item metadata.  This looks up the
     * field in which to store this metadata in the configuration
     * sword.slug.field
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     target item
     * @param slugVal
     *     slug value
     * @param verboseDescription
     *     The description.
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    protected void setSlug(Context context, Item item, String slugVal,
            VerboseDescription verboseDescription)
            throws DSpaceSwordException
    {
        // if there isn't a slug value, don't set it
        if (slugVal == null)
        {
            return;
        }

        String field = ConfigurationManager
            .getProperty("swordv2-server", "slug.field");
        if (field == null || "".equals(field))
        {
            throw new DSpaceSwordException(
                "No configuration, or configuration is invalid for: sword.slug.field");
        }

        MetadataFieldInfo info = this.configToDC(field, null);
        try
        {
            itemService.clearMetadata(context, item, info.schema, info.element,
                info.qualifier, Item.ANY);
            itemService.addMetadata(context, item, info.schema, info.element,
                info.qualifier, null, slugVal);
        }
        catch (SQLException e)
        {
            log.error("Caught exception trying to set slug", e);
            throw new DSpaceSwordException(e);
        }

        verboseDescription.append("Slug value set in response where available");
    }

    /**
     * Utility method to turn given metadata fields of the form
     schema.element.qualifier into Metadatum objects which can be
     used to access metadata in items.
     *
     * The def parameter should be null, * or "" depending on how
     you intend to use the Metadatum object.
     *
     * @param config
     * @param def
     */
    private MetadataFieldInfo configToDC(String config, String def)
    {
        MetadataFieldInfo mfi = new MetadataFieldInfo();
        mfi.schema = def;
        mfi.element = def;
        mfi.qualifier = def;

        StringTokenizer stz = new StringTokenizer(config, ".");
        mfi.schema = stz.nextToken();
        mfi.element = stz.nextToken();
        if (stz.hasMoreTokens())
        {
            mfi.qualifier = stz.nextToken();
        }

        return mfi;
    }

    private class MetadataFieldInfo
    {
        private String schema;

        private String element;

        private String qualifier;
    }
}
