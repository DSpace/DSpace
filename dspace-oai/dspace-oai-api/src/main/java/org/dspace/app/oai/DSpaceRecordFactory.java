/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.oai;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.dspace.content.DCDate;
import org.dspace.search.HarvestedItemInfo;

import ORG.oclc.oai.server.catalog.RecordFactory;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

/**
 * Implementation of the OAICat RecordFactory base class for DSpace items.
 * 
 * @author Robert Tansley
 * @version $Revision: 5845 $
 */
public class DSpaceRecordFactory extends RecordFactory
{
    public DSpaceRecordFactory(Properties properties)
    {
        // We don't use the OAICat properties; pass on up
        super(properties);
    }

    public String fromOAIIdentifier(String identifier)
    {
        // Our local identifier is actually the same as the OAI one (the Handle)
        return identifier;
    }

    public String quickCreate(Object nativeItem, String schemaURL,
            String metadataPrefix) throws IllegalArgumentException,
            CannotDisseminateFormatException
    {
        // Not supported
        return null;
    }

    public String getOAIIdentifier(Object nativeItem)
    {
        String h = DSpaceOAICatalog.OAI_ID_PREFIX
                + ((HarvestedItemInfo) nativeItem).handle;

        return h;
    }

    public String getDatestamp(Object nativeItem)
    {
        Date d = ((HarvestedItemInfo) nativeItem).datestamp;

        // Return as ISO8601
        return new DCDate(d).toString();
    }

    public Iterator getSetSpecs(Object nativeItem)
    {
        HarvestedItemInfo hii = (HarvestedItemInfo) nativeItem;
        Iterator<String> i = hii.collectionHandles.iterator();
        List<String> setSpecs = new LinkedList<String>();

        // Convert the DB Handle string 123.456/789 to the OAI-friendly
        // hdl_123.456/789
        while (i.hasNext())
        {
            String handle = "hdl_" + i.next();
            setSpecs.add(handle.replace('/', '_'));
        }

        return setSpecs.iterator();
    }

    public boolean isDeleted(Object nativeItem)
    {
        HarvestedItemInfo hii = (HarvestedItemInfo) nativeItem;

        return hii.withdrawn;
    }

    public Iterator getAbouts(Object nativeItem)
    {
        // Nothing in the about section for now
        return new LinkedList().iterator();
    }
}
