/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.configuration;

import java.util.List;

import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.springframework.beans.factory.annotation.Required;

public class ColumnVisualizationConfiguration
{
    private List<String> metadata;

    private String name;

    private Boolean sortable;

    private String sortField;

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public List<String> getMetadata()
    {
        return metadata;
    }

    @Required
    public void setMetadata(List<String> metadata)
    {
        this.metadata = metadata;
    }

    public String getSortField()
    {
        if (sortField != null)
        {
            return sortField;
        }
        try
        {
            for (SortOption tmpSo : SortOption.getSortOptions())
            {
                for (String md : metadata)
                {
                    if (md.equals(tmpSo.getMetadata()))
                    {
                        sortField = "bi_sort_" + tmpSo.getNumber() + "_sort";
                        return sortField;
                    }
                }
            }
        }
        catch (SortException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

    public boolean isSortable()
    {
        if (sortable != null)
        {
            return sortable;
        }
        try
        {
            for (SortOption tmpSo : SortOption.getSortOptions())
            {
                for (String md : metadata)
                {
                    if (md.equals(tmpSo.getMetadata()))
                    {
                        sortable = true;
                        return sortable;
                    }
                }
            }
            sortable = false;
        }
        catch (SortException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
        return false;
    }

    public String getHTMLContent(BrowsableDSpaceObject dso)
    {
        StringBuffer sb = new StringBuffer();
        for (String md : metadata)
        {
            String[] split = md.split("\\.");
            Metadatum[] values = dso.getMetadata(split[0], split[1],
                    split.length > 2 ? split[2] : null, Item.ANY);
            for (Metadatum v : values)
            {
                sb.append(v.value).append(", ");
            }
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 2) : null;
    }
}
