/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;
import java.util.Date;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.harvest.HarvestedItem;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedItemService;

/**
 * Builder to construct {@link HarvestedItem} objects.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class HarvestedItemBuilder extends AbstractBuilder<HarvestedItem, HarvestedItemService> {

    private HarvestedItem harvestedItem;

    protected HarvestedItemBuilder(Context context) {
        super(context);
    }

    public static HarvestedItemBuilder create(Context context, Item item, String itemOAIid) {
        HarvestedItemBuilder builder = new HarvestedItemBuilder(context);
        return builder.createHarvestedItem(context, item, itemOAIid);
    }

    private HarvestedItemBuilder createHarvestedItem(Context context, Item item, String itemOAIid) {
        try {
            this.harvestedItem = getService().create(context, item, itemOAIid);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
        return this;
    }

    public HarvestedItemBuilder withHarvestDate(Date harvestDate) {
        this.harvestedItem.setHarvestDate(harvestDate);
        return this;
    }

    @Override
    public void cleanup() throws Exception {
        delete(context, this.harvestedItem);
    }

    @Override
    public HarvestedItem build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, harvestedItem);
            context.commit();
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
        return harvestedItem;
    }

    @Override
    public void delete(Context c, HarvestedItem dso) throws Exception {
        harvestedItem = c.reloadEntity(harvestedItem);
        if (harvestedItem != null) {
            getService().delete(c, harvestedItem);
        }
    }

    @Override
    protected HarvestedItemService getService() {
        return HarvestServiceFactory.getInstance().getHarvestedItemService();
    }

}
