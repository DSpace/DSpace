/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
 package org.dspace.rest.filter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;

import com.ibm.icu.util.Calendar;

public class ItemFilterUtil {
    protected static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    static Logger log = Logger.getLogger(ItemFilterUtil.class);
    public enum BundleName{ORIGINAL,TEXT,LICENSE,THUMBNAIL;}

    static String[] getDocumentMimeTypes() {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("rest.report-mime-document");
    }

    static String[] getSupportedDocumentMimeTypes() {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("rest.report-mime-document-supported");
    }

    static String[] getSupportedImageMimeTypes() {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("rest.report-mime-document-image");
    }

    static int countOriginalBitstream(Item item) {
        return countBitstream(BundleName.ORIGINAL, item);
    }

    static int countBitstream(BundleName bundleName, Item item)
    {
        int count = 0;
        for (Bundle bundle: item.getBundles())
        {
            if (!bundle.getName().equals(bundleName.name()))
            {
                continue;
            }
            count += bundle.getBitstreams().size();
        }

        return count;
    }

    static List<String> getBitstreamNames(BundleName bundleName, Item item)
    {
        ArrayList<String> names = new ArrayList<String>();
        for (Bundle bundle: item.getBundles())
        {
            if (!bundle.getName().equals(bundleName.name()))
            {
                continue;
            }
            for (Bitstream bit: bundle.getBitstreams())
            {
                names.add(bit.getName());
            }
        }
        return names;
    }


    static int countOriginalBitstreamMime(Context context, Item item, String[] mimeList) {
        return countBitstreamMime(context, BundleName.ORIGINAL, item, mimeList);
    }

    static int countBitstreamMime(Context context, BundleName bundleName, Item item, String[] mimeList)
    {
        int count = 0;
        for (Bundle bundle: item.getBundles())
        {
            if (!bundle.getName().equals(bundleName.name()))
            {
                continue;
            }
            for (Bitstream bit: bundle.getBitstreams())
            {
                for (String mime: mimeList)
                {
                    try
                    {
                        if (bit.getFormat(context).getMIMEType().equals(mime.trim()))
                        {
                            count++;
                        }
                    }
                    catch (SQLException e)
                    {
                        log.error("Get format error for bitstream " + bit.getName());
                    }
                }
            }
        }
        return count;
    }

    static int countBitstreamByDesc(BundleName bundleName, Item item, String[] descList)
    {
        int count = 0;
        for (Bundle bundle: item.getBundles())
        {
            if (!bundle.getName().equals(bundleName.name()))
            {
                continue;
            }
            for (Bitstream bit: bundle.getBitstreams())
            {
                for (String desc: descList)
                {
                    String bitDesc = bit.getDescription();
                    if (bitDesc == null)
                    {
                        continue;
                    }
                    if (bitDesc.equals(desc.trim()))
                    {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    static int countBitstreamSmallerThanMinSize(Context context, BundleName bundleName, Item item, String[] mimeList, String prop)
    {
        long size = DSpaceServicesFactory.getInstance().getConfigurationService().getLongProperty(prop);
        int count = 0;
        try
        {
            for (Bundle bundle: item.getBundles())
            {
                if (!bundle.getName().equals(bundleName.name()))
                {
                    continue;
                }
                for (Bitstream bit: bundle.getBitstreams())
                {
                    for (String mime: mimeList)
                    {
                        if (bit.getFormat(context).getMIMEType().equals(mime.trim()))
                        {
                            if (bit.getSize() < size)
                            {
                                count++;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
        }
        return count;
    }

    static int countBitstreamLargerThanMaxSize(Context context, BundleName bundleName, Item item, String[] mimeList, String prop)
    {
        long size = DSpaceServicesFactory.getInstance().getConfigurationService().getLongProperty(prop);
        int count = 0;
        try
        {
            for (Bundle bundle: item.getBundles())
            {
                if (!bundle.getName().equals(bundleName.name()))
                {
                    continue;
                }
                for (Bitstream bit: bundle.getBitstreams())
                {
                    for (String mime: mimeList)
                    {
                        if (bit.getFormat(context).getMIMEType().equals(mime.trim()))
                        {
                            if (bit.getSize() > size)
                            {
                                count++;
                            }
                        }
                    }
                }
            }
        }
        catch (SQLException e)
        {
        }
        return count;
    }

    static int countOriginalBitstreamMimeStartsWith(Context context, Item item, String prefix) {
        return countBitstreamMimeStartsWith(context, BundleName.ORIGINAL, item, prefix);
    }

    static int countBitstreamMimeStartsWith(Context context, BundleName bundleName, Item item, String prefix)
    {
        int count = 0;
        try
        {
            for (Bundle bundle: item.getBundles())
            {
                if (!bundle.getName().equals(bundleName.name()))
                {
                    continue;
                }
                for (Bitstream bit: bundle.getBitstreams())
                {
                    if (bit.getFormat(context).getMIMEType().startsWith(prefix))
                    {
                        count++;
                    }
                }
            }
        }
        catch (SQLException e)
        {
        }
        return count;
    }

    static boolean hasUnsupportedBundle(Item item, String[] bundleList)
    {
        if (bundleList == null)
        {
            return false;
        }
        ArrayList<String> bundles = new ArrayList<String>();
        for (String bundleName: bundleList)
        {
            bundles.add(bundleName.trim());
        }
        for (Bundle bundle: item.getBundles())
        {
            if (!bundles.contains(bundle.getName()))
            {
                return true;
            }
        }
        return false;
    }

    static boolean hasOriginalBitstreamMime(Context context, Item item, String[] mimeList) {
        return hasBitstreamMime(context, BundleName.ORIGINAL, item, mimeList);
    }

    static boolean hasBitstreamMime(Context context, BundleName bundleName, Item item, String[] mimeList) {
        return countBitstreamMime(context, bundleName, item, mimeList) > 0;
    }

    static boolean hasMetadataMatch(Item item, String fieldList, Pattern regex)
    {
        if (fieldList.equals("*"))
        {
            for (MetadataValue md: itemService.getMetadata(item, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY))
            {
                if (regex.matcher(md.getValue()).matches())
                {
                    return true;
                }
            }
        }
        else
        {
            for (String field: fieldList.split(","))
            {
                for (MetadataValue md: itemService.getMetadataByMetadataString(item, field.trim()))
                {
                    if (regex.matcher(md.getValue()).matches())
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    static boolean hasOnlyMetadataMatch(Item item, String fieldList, Pattern regex)
    {
        boolean matches = false;
        if (fieldList.equals("*"))
        {
            for (MetadataValue md: itemService.getMetadata(item, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY))
            {
                if (regex.matcher(md.getValue()).matches())
                {
                    matches = true;
                }
                else
                {
                    return false;
                }
            }
        }
        else
        {
            for (String field: fieldList.split(","))
            {
                for (MetadataValue md: itemService.getMetadataByMetadataString(item, field.trim()))
                {
                    if (regex.matcher(md.getValue()).matches())
                    {
                        matches = true;
                    }
                    else
                    {
                        return false;
                    }
                }
            }
        }
        return matches;
    }

    static boolean recentlyModified(Item item, int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -days);
        return cal.getTime().before(item.getLastModified());
    }
}
