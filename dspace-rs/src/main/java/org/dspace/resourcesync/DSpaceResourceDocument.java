/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.openarchives.resourcesync.ResourceSync;
import org.openarchives.resourcesync.ResourceSyncDocument;
import org.openarchives.resourcesync.ResourceSyncLn;
import org.openarchives.resourcesync.URL;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Richard Jones
 *
 */
public class DSpaceResourceDocument
{
    protected List<MetadataFormat> mdFormats = null;
    protected Context context;
    protected UrlManager um = new UrlManager();

    public DSpaceResourceDocument(Context context)
    {
        this.context = context;

        // get all the configuration
        this.mdFormats = this.getMetadataFormats();
    }

    public DSpaceResourceDocument(Context context, List<String> exposeBundles, List<MetadataFormat> mdFormats)
    {
        this.context = context;
        this.mdFormats = mdFormats;
    }

    protected void addResources(Item item, ResourceSyncDocument rl)
            throws SQLException
    {
        // record all of the bitstreams that we are going to expose
        List<Bitstream> exposed = new ArrayList<Bitstream>();

        // get the collections that the item is part of
        Collection[] collection = item.getCollections();
        List<Collection> clist = Arrays.asList(collection);

        // add all the relevant bitstreams
        boolean isOnlyMetadata = ConfigurationManager.getBooleanProperty("resourcesync", "resourcedump.onlymetadata");
        if (!isOnlyMetadata)
        {
        	for (Bundle bundle : item.getBundles())
        	{
        		// only expose resources in permitted bundles
        		if (!ResourceSyncConfiguration.getBundlesToExpose().contains(bundle.getName()))
        		{
        			continue;
        		}

        		for (Bitstream bitstream : bundle.getBitstreams())
        		{
        			this.addBitstream(bitstream, item, clist, rl);
        			exposed.add(bitstream);
        		}
        	}
        }
        // add all the relevant metadata formats
        for (MetadataFormat format : this.mdFormats)
        {
            this.addMetadata(item, format, exposed, clist, rl);
        }
    }

    protected URL addBitstream(Bitstream bitstream, Item item, List<Collection> collections, ResourceSyncDocument rl)
    {
        URL bs = new URL();

        bs.setLoc(this.getBitstreamUrl(bitstream));
        bs.setLastModified(item.getLastModified()); // last modified date is not available on a bitstream, so we use the item one
        bs.setType(bitstream.getFormat().getMIMEType());
        bs.setLength(bitstream.getSize());
        bs.addHash(bitstream.getChecksumAlgorithm().toLowerCase(), bitstream.getChecksum());

        for (MetadataFormat format : this.mdFormats)
        {
            bs.addLn(ResourceSync.REL_DESCRIBED_BY, this.getMetadataUrl(item, format));
        }

        for (Collection collection : collections)
        {
            bs.addLn(ResourceSync.REL_COLLECTION, this.getCollectionUrl(collection));
        }

        rl.addEntry(bs);
        return bs;
    }

    protected URL addMetadata(Item item, MetadataFormat format, List<Bitstream> describes, List<Collection> collections,
    			ResourceSyncDocument rl)
    {
        URL metadata = new URL();

        // set the metadata url
        metadata.setLoc(this.getMetadataUrl(item, format));
        metadata.addLn(ResourceSync.REL_PROFILE, format.getNamespace());

        // technically this only tells us when the item was last updated, not the metadata
        metadata.setLastModified(item.getLastModified());

        // set the type
        if (format.getMimetype() != null)
        {
            metadata.setType(format.getMimetype());
        }

        for (Bitstream bs : describes)
        {
            metadata.addLn(ResourceSync.REL_DESCRIBES, this.getBitstreamUrl(bs));
        }

        for (Collection collection : collections)
        {
            metadata.addLn(ResourceSync.REL_COLLECTION, this.getCollectionUrl(collection));
        }

        // now add "alternate" links for all the other metadata formats
        for (MetadataFormat f : this.mdFormats)
        {
            if (f.getNamespace().equals(format.getNamespace()))
            {
                // don't do an alternate of the one that is the main record
                continue;
            }
            ResourceSyncLn ln = metadata.addLn(ResourceSync.REL_ALTERNATE, this.getMetadataUrl(item, f));
            ln.setType(f.getMimetype());
        }

        rl.addEntry(metadata);
        return metadata;
    }


    protected List<MetadataFormat> getMetadataFormats()
    {
        List<MetadataFormat> formats = new ArrayList<MetadataFormat>();

        // load our config options
        String formatCfg = ConfigurationManager.getProperty("resourcesync", "metadata.formats");
        String typeCfg = ConfigurationManager.getProperty("resourcesync", "metadata.types");

        // if there's no format config, there are no formats, irrespective of what
        // the type config says
        if (formatCfg == null || "".equals(formatCfg))
        {
            return formats;
        }

        // get the pairs from the config options
        Map<String, String> formatPairs = this.getPairs(formatCfg);
        Map<String, String> typePairs = this.getPairs(typeCfg);

        for (String prefix : formatPairs.keySet())
        {
            MetadataFormat mf = new MetadataFormat(prefix, formatPairs.get(prefix), typePairs.get(prefix));
            formats.add(mf);
        }

        return formats;
    }

    public MetadataFormat getMetadataFormat(String prefix)
    {
        List<MetadataFormat> formats = this.getMetadataFormats();
        for (MetadataFormat format : formats)
        {
            if (format.getPrefix().equals(prefix))
            {
                return format;
            }
        }
        return null;
    }

    private Map<String, String> getPairs(String cfg)
    {
        Map<String, String> pairs = new HashMap<String, String>();

        if (cfg == null)
        {
            return pairs;
        }

        // first split the config by comma
        String[] bits = cfg.split(",");

        for (String bit : bits)
        {
            // now split around the = sign
            String[] pair = bit.split("=");
            if (pair.length != 2)
            {
                continue;
            }
            pairs.put(pair[0].trim(), pair[1].trim());
        }

        return pairs;
    }

    protected String getMetadataChangeFreq()
    {
        String cf = ConfigurationManager.getProperty("resourcesync", "metadata.change-freq");
        if (cf == null || "".equals(cf))
        {
            return null;
        }
        return cf;
    }

    protected String getBitstreamChangeFreq()
    {
        String cf = ConfigurationManager.getProperty("resourcesync", "bitstream.change-freq");
        if (cf == null || "".equals(cf))
        {
            return null;
        }
        return cf;
    }

    protected String getMetadataUrl(Item item, MetadataFormat format)
    {
        String baseUrl = ConfigurationManager.getProperty("resourcesync", "base-url");
        String handle = item.getHandle();
        String url = baseUrl + "/resource/" + handle + "/" + format.getPrefix();
        return url;
    }

	protected String getBitstreamUrl(Bitstream bitstream) {
		String bsLink = ConfigurationManager.getProperty("resourcesync", "base-url");
		bsLink += "/bitstreams/" + bitstream.getID();
		return bsLink;
	}

    protected String getCollectionUrl(Collection collection)
    {
        String handle = collection.getHandle();
        String base = ConfigurationManager.getProperty("dspace.url");
        return base + "/handle/" + handle;
    }
}
