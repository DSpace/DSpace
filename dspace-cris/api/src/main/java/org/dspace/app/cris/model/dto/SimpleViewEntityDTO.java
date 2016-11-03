/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.dto;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.dspace.core.Context;
import org.dspace.util.ActionUtils;

public class SimpleViewEntityDTO
{

    private String handle;
    
    private Integer entityID;

    private Integer entityTypeID;

    private Date lastModified;
    
    private Map<String, List<String>> duplicateItem;
    
    private Boolean withdrawn;

    /**
     * Allowed actions on the item
     */
    private List<String> actions;
    
    // FIXME why is it here!!!?!
    /**
     * HACK to provide the locale back to the client...
     */
    private Locale locale;

    public SimpleViewEntityDTO(Context context, int itemID, int typeID)
    {    
        this.locale = context.getCurrentLocale();
        this.setEntityID(itemID);
        this.setEntityTypeID(typeID);
    }
    
    
    public Map<String, List<String>> getDuplicateItem()
    {
        if(duplicateItem==null) {
            duplicateItem = new HashMap<String, List<String>>();
        }
        return duplicateItem;
    }

    public void setDuplicateItem(Map<String, List<String>> duplicateItem)
    {
        this.duplicateItem = duplicateItem;
    }

    public Integer getEntityID()
    {
        return entityID;
    }

    public void setEntityID(Integer entityID)
    {
        this.entityID = entityID;
    }

    public Integer getEntityTypeID()
    {
        return entityTypeID;
    }

    public void setEntityTypeID(Integer entityTypeID)
    {
        this.entityTypeID = entityTypeID;
    }

    public Date getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(Date lastModified)
    {
        this.lastModified = lastModified;
    }

    public Boolean getWithdrawn()
    {
        return withdrawn;
    }

    public void setWithdrawn(Boolean withdrawn)
    {
        this.withdrawn = withdrawn;
    }

    public List<String> getActions()
    {
        return actions;
    }

    public void setActions(List<String> actions)
    {
        this.actions = actions;
    }
    
    public Map<String, String> getActionsLabel()
    {       
        return ActionUtils.createActionsLabel(locale);
    }


    public String getHandle()
    {
        return handle;
    }


    public void setHandle(String handle)
    {
        this.handle = handle;
    }
}
