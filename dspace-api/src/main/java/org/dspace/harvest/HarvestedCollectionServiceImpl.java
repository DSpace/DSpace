/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.harvest.dao.HarvestedCollectionDAO;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Service implementation for the HarvestedCollection object.
 * This class is responsible for all business logic calls for the HarvestedCollection object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HarvestedCollectionServiceImpl implements HarvestedCollectionService
{
    @Autowired(required = true)
    protected HarvestedCollectionDAO harvestedCollectionDAO;

    protected HarvestedCollectionServiceImpl()
    {
    }

    @Override
    public HarvestedCollection find(Context context, Collection collection) throws SQLException {
        return harvestedCollectionDAO.findByCollection(context, collection);
    }

    @Override
    public HarvestedCollection create(Context context, Collection collection) throws SQLException {
        HarvestedCollection harvestedCollection = harvestedCollectionDAO.create(context, new HarvestedCollection());
        harvestedCollection.setCollection(collection);
        harvestedCollection.setHarvestType(HarvestedCollection.TYPE_NONE);
        update(context, harvestedCollection);
        return harvestedCollection;    }

    @Override
    public boolean isHarvestable(Context context, Collection collection) throws SQLException {
        HarvestedCollection hc = find(context, collection);
        if (hc != null && hc.getHarvestType() > 0 && hc.getOaiSource() != null && hc.getOaiSetId() != null &&
                hc.getHarvestStatus() != HarvestedCollection.STATUS_UNKNOWN_ERROR) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isHarvestable(HarvestedCollection harvestedCollection) throws SQLException {
        if (harvestedCollection.getHarvestType() > 0 && harvestedCollection.getOaiSource() != null && harvestedCollection.getOaiSetId() != null &&
                harvestedCollection.getHarvestStatus() != HarvestedCollection.STATUS_UNKNOWN_ERROR) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isReady(Context context, Collection collection) throws SQLException {
        HarvestedCollection hc = find(context, collection);
        return isReady(hc);
    }

    @Override
    public boolean isReady(HarvestedCollection harvestedCollection) throws SQLException {
        if (isHarvestable(harvestedCollection) &&	(harvestedCollection.getHarvestStatus() == HarvestedCollection.STATUS_READY || harvestedCollection.getHarvestStatus() == HarvestedCollection.STATUS_OAI_ERROR))
        {
            return true;
        }

        return false;
    }

    @Override
    public List<HarvestedCollection> findAll(Context context) throws SQLException {
        return harvestedCollectionDAO.findAll(context, HarvestedCollection.class);
    }

    @Override
    public List<HarvestedCollection> findReady(Context context) throws SQLException {
        int harvestInterval = ConfigurationManager.getIntProperty("oai", "harvester.harvestFrequency");
        if (harvestInterval == 0)
        {
            harvestInterval = 720;
        }

        int expirationInterval = ConfigurationManager.getIntProperty("oai", "harvester.threadTimeout");
        if (expirationInterval == 0)
        {
            expirationInterval = 24;
        }

        Date startTime;
        Date expirationTime;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, -1 * harvestInterval);
        startTime = calendar.getTime();

        calendar.setTime(startTime);
        calendar.add(Calendar.HOUR, -2 * expirationInterval);
        expirationTime = calendar.getTime();

        int[] statuses = new int[]{HarvestedCollection.STATUS_READY, HarvestedCollection.STATUS_OAI_ERROR};
        return harvestedCollectionDAO.findByLastHarvestedAndHarvestTypeAndHarvestStatusesAndHarvestTime(context, startTime, HarvestedCollection.TYPE_NONE, statuses, HarvestedCollection.STATUS_BUSY, expirationTime);
    }

    @Override
    public List<HarvestedCollection> findByStatus(Context context, int status) throws SQLException {
        return harvestedCollectionDAO.findByStatus(context, status);
    }

    @Override
    public HarvestedCollection findOldestHarvest(Context context) throws SQLException {
        return harvestedCollectionDAO.findByStatusAndMinimalTypeOrderByLastHarvestedAsc(context, HarvestedCollection.STATUS_READY, HarvestedCollection.TYPE_NONE, 1);
    }

    @Override
    public HarvestedCollection findNewestHarvest(Context context) throws SQLException {
        return harvestedCollectionDAO.findByStatusAndMinimalTypeOrderByLastHarvestedDesc(context, HarvestedCollection.STATUS_READY, HarvestedCollection.TYPE_NONE, 1);
    }

    @Override
    public void delete(Context context, HarvestedCollection harvestedCollection) throws SQLException {
        harvestedCollectionDAO.delete(context, harvestedCollection);
    }

    @Override
    public void update(Context context, HarvestedCollection harvestedCollection) throws SQLException {
        harvestedCollectionDAO.save(context, harvestedCollection);
    }

    @Override
    public boolean exists(Context context) throws SQLException {
        return 0 < harvestedCollectionDAO.count(context);
    }


}
