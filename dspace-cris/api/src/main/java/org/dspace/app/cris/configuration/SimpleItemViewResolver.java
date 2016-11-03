/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.configuration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.dto.SimpleViewEntityDTO;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.util.ItemUtils;

public class SimpleItemViewResolver implements ISimpleViewResolver
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(SimpleItemViewResolver.class);
    
    @Override
    public void fillDTO(Context context, SimpleViewEntityDTO dto, DSpaceObject dso)
    {
        
        Item item = (Item)dso;

        //collection
        List<String> collection = new ArrayList<String>();
        //status
        List<String> status = new ArrayList<String>();
        //submitter
        List<String> submitter = new ArrayList<String>();
        try
        {
            collection.add(item.getParentObject().getName());
            collection.add(item.getParentObject().getHandle());
            status.add(""+ItemUtils.getItemStatus(context, item));
            submitter.add(item.getSubmitter().getLastName());
            submitter.add(item.getSubmitter().getFirstName());
            submitter.add(item.getSubmitter().getNetid());
            submitter.add(""+item.getSubmitter().getID());
            //handle
            dto.setHandle(HandleManager.findHandle(context, item));
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        
        dto.getDuplicateItem().put("collection", collection);
        dto.getDuplicateItem().put("status", status);
        dto.getDuplicateItem().put("submitter", submitter);

        //lastmodified
        dto.setLastModified(item.getLastModified());

    }

}
