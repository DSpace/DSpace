/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.harvest.dao.HarvestedItemDAO;
import org.dspace.harvest.service.HarvestedItemService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;

/**
 * Service implementation for the HarvestedItem object.
 * This class is responsible for all business logic calls for the HarvestedItem object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HarvestedItemServiceImpl implements HarvestedItemService {

    @Autowired(required = true)
    protected HarvestedItemDAO harvestedItemDAO;

    protected HarvestedItemServiceImpl()
    {

    }

    @Override
    public HarvestedItem find(Context context, Item item) throws SQLException {
        return harvestedItemDAO.findByItem(context, item);
    }

    @Override
    public Item getItemByOAIId(Context context, String itemOaiID, Collection collection) throws SQLException {
        HarvestedItem harvestedItem = harvestedItemDAO.findByOAIId(context, itemOaiID, collection);
        if(harvestedItem != null)
        {
            return harvestedItem.getItem();
        }
        else
        {
            return null;
        }
    }

    @Override
    public HarvestedItem create(Context context, Item item, String itemOAIid) throws SQLException {
        HarvestedItem harvestedItem = harvestedItemDAO.create(context, new HarvestedItem());
        harvestedItem.setItem(item);
        harvestedItem.setOaiId(itemOAIid);
        update(context, harvestedItem);
        return harvestedItem;
    }

    @Override
    public void update(Context context, HarvestedItem harvestedItem) throws SQLException {
        harvestedItemDAO.save(context, harvestedItem);
    }

    @Override
    public void delete(Context context, HarvestedItem harvestedItem) throws SQLException {
        harvestedItemDAO.delete(context, harvestedItem);
    }


}
