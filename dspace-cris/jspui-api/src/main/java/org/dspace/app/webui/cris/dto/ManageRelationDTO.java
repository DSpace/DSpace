/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.dto;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.list.LazyList;

public class ManageRelationDTO
{
    private List<String> toUnLink = LazyList.decorate(new ArrayList<String>(),
            FactoryUtils.instantiateFactory(String.class));

    private List<String> toActivate = LazyList.decorate(
            new ArrayList<String>(),
            FactoryUtils.instantiateFactory(String.class));

    private List<String> toHide = LazyList.decorate(new ArrayList<String>(),
            FactoryUtils.instantiateFactory(String.class));

    private List<String> orderedSelected = LazyList.decorate(
            new ArrayList<String>(),
            FactoryUtils.instantiateFactory(String.class));

    public List<String> getToUnLink()
    {
        return toUnLink;
    }

    public void setToUnLink(List<String> toUnLink)
    {
        this.toUnLink = toUnLink;
    }

    public List<String> getToActivate()
    {
        return toActivate;
    }

    public void setToActivate(List<String> toActivate)
    {
        this.toActivate = toActivate;
    }

    public List<String> getToHide()
    {
        return toHide;
    }

    public void setToHide(List<String> toHide)
    {
        this.toHide = toHide;
    }

    public List<String> getOrderedSelected()
    {
        return orderedSelected;
    }

    public void setOrderedSelected(List<String> orderedSelected)
    {
        this.orderedSelected = orderedSelected;
    }
}
