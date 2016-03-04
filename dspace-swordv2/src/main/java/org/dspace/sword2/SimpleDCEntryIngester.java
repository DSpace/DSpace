/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.swordapp.server.*;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;


public class SimpleDCEntryIngester extends AbstractSimpleDC
        implements SwordEntryIngester
{
    private static final Logger log = Logger
            .getLogger(SimpleDCEntryIngester.class);

    protected WorkspaceItemService workspaceItemService = ContentServiceFactory
            .getInstance().getWorkspaceItemService();
    
    protected ConfigurationService configurationService = DSpaceServicesFactory
            .getInstance().getConfigurationService();

    public SimpleDCEntryIngester()
    {
        this.loadMetadataMaps();
    }

    public DepositResult ingest(Context context, Deposit deposit,
            DSpaceObject dso, VerboseDescription verboseDescription)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        return this
                .ingest(context, deposit, dso, verboseDescription, null, false);
    }

    public DepositResult ingest(Context context, Deposit deposit,
            DSpaceObject dso, VerboseDescription verboseDescription,
            DepositResult result, boolean replace)
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
                    verboseDescription, result, replace);
        }
        return null;
    }

    public DepositResult ingestToItem(Context context, Deposit deposit,
            Item item, VerboseDescription verboseDescription,
            DepositResult result, boolean replace)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        try
        {
            if (result == null)
            {
                result = new DepositResult();
            }
            result.setItem(item);

            // clean out any existing item metadata which is allowed to be replaced
            if (replace)
            {
                this.removeMetadata(context, item);
            }

            // add the metadata to the item
            this.addMetadataToItem(context, deposit, item);

            // update the item metadata to inclue the current time as
            // the updated date
            this.setUpdatedDate(context, item, verboseDescription);

            // in order to write these changes, we need to bypass the
            // authorisation briefly, because although the user may be
            // able to add stuff to the repository, they may not have
            // WRITE permissions on the archive.
            context.turnOffAuthorisationSystem();
            itemService.update(context, item);
            context.restoreAuthSystemState();

            verboseDescription.append("Update successful");

            result.setItem(item);
            result.setTreatment(this.getTreatment());

            return result;
        }
        catch (SQLException | AuthorizeException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    private void removeMetadata(Context context, Item item)
            throws DSpaceSwordException
    {
        String[] replaceableMetadata = configurationService
                .getArrayProperty("swordv2-server.metadata.replaceable");
        for (String part : replaceableMetadata)
        {
            MetadataValueInfo info = this
                    .makeMetadataValueInfo(part.trim(), null);
            try
            {
                itemService
                        .clearMetadata(context, item, info.schema, info.element,
                                info.qualifier, Item.ANY);
            }
            catch (SQLException e)
            {
                log.error("Caught exception trying to remove metadata", e);
                throw new DSpaceSwordException(e);
            }
        }
    }

    private void addUniqueMetadata(Context context, MetadataValueInfo info,
            Item item) throws SQLException
    {
        String qual = info.qualifier;
        if (info.qualifier == null)
        {
            qual = Item.ANY;
        }

        String lang = info.language;
        if (info.language == null)
        {
            lang = Item.ANY;
        }
        List<MetadataValue> existing = itemService
                .getMetadata(item, info.schema, info.element, qual, lang);
        for (MetadataValue dcValue : existing)
        {
            // FIXME: probably we want to be slightly more careful about qualifiers and languages
            //
            // if the submitted value is already attached to the item, just skip it
            if (dcValue.getValue().equals(info.value))
            {
                return;
            }
        }

        // if we get to here, go on and add the metadata
        itemService.addMetadata(context, item, info.schema, info.element,
                info.qualifier, info.language, info.value);
    }

    private void addMetadataToItem(Context context, Deposit deposit, Item item)
            throws DSpaceSwordException
    {
        // now, go through and get the metadata from the EntryPart and put it in DSpace
        SwordEntry se = deposit.getSwordEntry();

        // first do the standard atom terms (which may get overridden later)
        String title = se.getTitle();
        String summary = se.getSummary();
        if (title != null)
        {
            String titleField = this.dcMap.get("title");
            if (titleField != null)
            {
                MetadataValueInfo info = this
                        .makeMetadataValueInfo(titleField, title);
                try
                {
                    this.addUniqueMetadata(context, info, item);
                }
                catch (SQLException e)
                {
                    log.error("Caught exception trying to add title", e);
                    throw new DSpaceSwordException(e);
                }
            }
        }
        if (summary != null)
        {
            String abstractField = this.dcMap.get("abstract");
            if (abstractField != null)
            {
                MetadataValueInfo info = this
                        .makeMetadataValueInfo(abstractField, summary);
                try
                {
                    this.addUniqueMetadata(context, info, item);
                }
                catch (SQLException e)
                {
                    log.error("Caught exception trying to set abstract", e);
                    throw new DSpaceSwordException(e);
                }
            }
        }

        Map<String, List<String>> dc = se.getDublinCore();
        for (String term : dc.keySet())
        {
            String dsTerm = this.dcMap.get(term);
            if (dsTerm == null)
            {
                // ignore anything we don't understand
                continue;
            }

            // now add all the metadata terms
            MetadataValueInfo info = this.makeMetadataValueInfo(dsTerm, null);
            for (String value : dc.get(term))
            {
                info.value = value;
                try
                {
                    this.addUniqueMetadata(context, info, item);
                }
                catch (SQLException e)
                {
                    log.error("Caught exception trying to add metadata", e);
                    throw new DSpaceSwordException(e);
                }
            }
        }
    }

    public DepositResult ingestToCollection(Context context, Deposit deposit,
            Collection collection, VerboseDescription verboseDescription,
            DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        try
        {
            // decide whether we have a new item or an existing one
            Item item = null;
            WorkspaceItem wsi = null;
            if (result != null)
            {
                item = result.getItem();
            }
            else
            {
                result = new DepositResult();
            }
            if (item == null)
            {
                // simple zip ingester uses the item template, since there is no native metadata
                wsi = workspaceItemService.create(context, collection, true);
                item = wsi.getItem();
            }

            // add the metadata to the item
            this.addMetadataToItem(context, deposit, item);

            // update the item metadata to inclue the current time as
            // the updated date
            this.setUpdatedDate(context, item, verboseDescription);

            // DSpace ignores the slug value as suggested identifier, but
            // it does store it in the metadata
            this.setSlug(context, item, deposit.getSlug(), verboseDescription);

            // in order to write these changes, we need to bypass the
            // authorisation briefly, because although the user may be
            // able to add stuff to the repository, they may not have
            // WRITE permissions on the archive.
            context.turnOffAuthorisationSystem();
            itemService.update(context, item);
            context.restoreAuthSystemState();

            verboseDescription.append("Ingest successful");
            verboseDescription
                    .append("Item created with internal identifier: " +
                            item.getID());

            result.setItem(item);
            result.setTreatment(this.getTreatment());

            return result;
        }
        catch (AuthorizeException e)
        {
            throw new SwordAuthException(e);
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    public MetadataValueInfo makeMetadataValueInfo(String field, String value)
            throws DSpaceSwordException
    {
        MetadataValueInfo dcv = new MetadataValueInfo();
        String[] bits = field.split("\\.");
        if (bits.length < 2 || bits.length > 3)
        {
            throw new DSpaceSwordException("invalid DC value: " + field);
        }
        dcv.schema = bits[0];
        dcv.element = bits[1];
        if (bits.length == 3)
        {
            dcv.qualifier = bits[2];
        }
        dcv.value = value;
        return dcv;
    }

    /**
     * Add the current date to the item metadata.  This looks up
     * the field in which to store this metadata in the configuration
     * sword.updated.field
     *
     *
     * @param context
     * @param item
     * @throws DSpaceSwordException
     */
    protected void setUpdatedDate(Context context, Item item,
            VerboseDescription verboseDescription)
            throws DSpaceSwordException
    {
        String field = configurationService
                .getProperty("swordv2-server.updated.field");
        if (StringUtils.isBlank(field))
        {
            throw new DSpaceSwordException(
                    "No configuration, or configuration is invalid for: swordv2-server.updated.field");
        }

        MetadataValueInfo info = this.makeMetadataValueInfo(field, null);
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
            log.error("Exception caught trying to set updated date", e);
            throw new DSpaceSwordException(e);
        }

        verboseDescription
                .append("Updated date added to response from item metadata where available");
    }

    /**
     * Store the given slug value (which is used for suggested identifiers,
     * and which DSpace ignores) in the item metadata.  This looks up the
     * field in which to store this metadata in the configuration
     * sword.slug.field
     *
     *
     * @param context
     * @param item
     * @param slugVal
     * @throws DSpaceSwordException
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

        String field = configurationService
                .getProperty("swordv2-server.slug.field");
        if (StringUtils.isBlank(field))
        {
            throw new DSpaceSwordException(
                    "No configuration, or configuration is invalid for: swordv2-server.slug.field");
        }

        MetadataValueInfo info = this.makeMetadataValueInfo(field, null);
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
     * The human readable description of the treatment this ingester has
     * put the deposit through
     *
     * @return
     * @throws DSpaceSwordException
     */
    private String getTreatment() throws DSpaceSwordException
    {
        return "A metadata only item has been created";
    }

    private class MetadataValueInfo
    {
        private String schema;

        private String element;

        private String qualifier;

        private String language;

        private String value;
    }
}
