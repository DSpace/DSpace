/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.data;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.xoai.util.XOAICacheManager;
import org.dspace.xoai.util.XOAIDatabaseManager;

import com.lyncode.xoai.dataprovider.core.ItemMetadata;
import com.lyncode.xoai.dataprovider.core.ReferenceSet;
import com.lyncode.xoai.dataprovider.data.AbstractAbout;
import com.lyncode.xoai.dataprovider.exceptions.MetadataBindException;
import com.lyncode.xoai.dataprovider.xml.xoai.Element;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceDatabaseItem extends DSpaceItem
{
    private static Logger log = LogManager.getLogger(DSpaceDatabaseItem.class);

    private static final String itemIdMd = ConfigurationManager.getProperty("xoai","identifier.metadata");
    private static final String itemIdPfx = ConfigurationManager.getProperty("xoai", "identifier.prefix");

    private Item item;
    private List<ReferenceSet> sets;

    private static List<ReferenceSet> getSets(Item item)
    {
        List<ReferenceSet> sets = new ArrayList<ReferenceSet>();
        List<Community> coms = new ArrayList<Community>();
        try
        {
            Collection[] itemCollections = item.getCollections();
            for (Collection col : itemCollections)
            {
                ReferenceSet s = new DSpaceSet(col);
                sets.add(s);
                for (Community com : XOAIDatabaseManager
                        .flatParentCommunities(col))
                    if (!coms.contains(com))
                        coms.add(com);
            }
            for (Community com : coms)
            {
                ReferenceSet s = new DSpaceSet(com);
                sets.add(s);
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return sets;
    }

    public DSpaceDatabaseItem(Item item)
    {
        this.item = item;
        this.sets = getSets(item);
    }

    @Override
    public List<AbstractAbout> getAbout()
    {
        return new ArrayList<AbstractAbout>();
    }

    @Override
    public Date getDatestamp()
    {
        return item.getLastModified();
    }

    @Override
    public List<ReferenceSet> getSets()
    {
        return sets;
    }

    @Override
    public boolean isDeleted()
    {
        return item.isWithdrawn();
    }

    private static Element getElement(List<Element> list, String name)
    {
        for (Element e : list)
            if (name.equals(e.getName()))
                return e;

        return null;
    }

    private ItemMetadata metadata = null;
    
    @Override
    public ItemMetadata getMetadata()
    {
        if (metadata == null)
        {
            try
            {
                metadata = new ItemMetadata(XOAICacheManager.getCompiledMetadata(this));
            }
            catch (MetadataBindException e)
            {
                log.warn(e.getMessage(), e);
                metadata = new ItemMetadata(XOAICacheManager.getMetadata(this));
            }
        }
        return metadata;
    }

    private List<String> getMetadata(List<Element> elems, String[] parts)
    {
        List<String> list = new ArrayList<String>();
        if (parts.length > 1)
        {
            if (parts[0].equals("*"))
            {
                for (Element e : elems)
                {
                    if (e.getElement() != null)
                        list.addAll(this.getMetadata(e.getElement(),
                                Arrays.copyOfRange(parts, 1, parts.length)));
                }
            }
            else
            {
                Element e = getElement(elems, parts[0]);
                if (e != null)
                    list.addAll(this.getMetadata(e.getElement(),
                            Arrays.copyOfRange(parts, 1, parts.length)));
            }
        }
        else if (parts.length == 1)
        {
            // Here we could have reached our target (named fields)
            for (Element e : elems)
            {
                for (Element.Field f : e.getField())
                {
                    if (parts[0].equals("*"))
                        list.add(f.getValue());
                    else if (parts[0].equals(f.getName()))
                        list.add(f.getValue());
                }
            }

            if (parts[0].equals("*"))
            {
                for (Element e : elems)
                {
                    if (e.getElement() != null)
                        list.addAll(this.getMetadata(e.getElement(),
                                Arrays.copyOfRange(parts, 1, parts.length)));
                }
            }
            else
            {
                Element e = getElement(elems, parts[0]);
                if (e != null)
                    list.addAll(this.getMetadata(e.getElement(),
                            Arrays.copyOfRange(parts, 1, parts.length)));
            }
        }
        else
        {
            // Here we have reached our target (unnamed fields)
            for (Element e : elems)
            {
                for (Element.Field f : e.getField())
                {
                    if (f.getName() == null || f.getName().equals(""))
                        list.add(f.getValue());
                }
            }
        }
        return list;
    }

    public List<String> getMetadata(String field)
    {
        String[] parts = field.split(Pattern.quote("."));
        try {
            return getMetadata(this.getMetadata().getMetadata().getElement(), parts);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return new ArrayList<String>();
    }

    public Item getItem()
    {
        return item;
    }

    public static String buildIdentifier (String handle, Item item)
    {
        String itemIdStr = null;
        if (itemIdMd != null) {
            DCValue[] mds = null;
            String[] parts = itemIdMd.split(Pattern.quote("."));
            if (parts.length == 2) {
                mds = item.getMetadata(parts[0], parts[1], null, null);
            } else if (parts.length == 3) {
                mds = item.getMetadata(parts[0], parts[1], parts[2], null);
            }
            if (mds != null && mds.length > 0 && mds[0].value != null)
                itemIdStr = mds[0].value.toString();
        }
        if (itemIdStr == null) {
            itemIdStr = handle;
        }
        return String.format("oai:%s:%s", itemIdPfx, itemIdStr);
    }

    @Override
    public String getIdentifier()
    {
        return buildIdentifier(getHandle(), item);
    }

    @Override
    protected String getHandle()
    {
        return item.getHandle();
    }
}
