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

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;

/**
 * Builder to construct {@link HarvestedCollection} objects.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class HarvestedCollectionBuilder extends AbstractBuilder<HarvestedCollection, HarvestedCollectionService> {

    private HarvestedCollection harvestedCollection;

    protected HarvestedCollectionBuilder(Context context) {
        super(context);
    }

    public static HarvestedCollectionBuilder create(Context context, Collection collection) {
        HarvestedCollectionBuilder builder = new HarvestedCollectionBuilder(context);
        return builder.createHarvestedCollection(context, collection);
    }

    private HarvestedCollectionBuilder createHarvestedCollection(Context context, Collection collection) {
        try {
            this.harvestedCollection = getService().create(context, collection);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
        return this;
    }

    public HarvestedCollectionBuilder withOaiSource(String oaiSource) {
        this.harvestedCollection.setOaiSource(oaiSource);
        return this;
    }

    public HarvestedCollectionBuilder withOaiSetId(String oaiSetId) {
        this.harvestedCollection.setOaiSetId(oaiSetId);
        return this;
    }

    public HarvestedCollectionBuilder withMetadataConfigId(String metadataConfigId) {
        this.harvestedCollection.setHarvestMetadataConfig(metadataConfigId);
        return this;
    }

    public HarvestedCollectionBuilder withHarvestType(int type) {
        this.harvestedCollection.setHarvestType(type);
        return this;
    }

    public HarvestedCollectionBuilder withLastHarvested(Date lastHarvested) {
        this.harvestedCollection.setLastHarvested(lastHarvested);
        return this;
    }

    public HarvestedCollectionBuilder withHarvestStatus(int status) {
        this.harvestedCollection.setHarvestStatus(status);
        return this;
    }

    @Override
    public void cleanup() throws Exception {
        delete(context, harvestedCollection);
    }

    @Override
    public HarvestedCollection build() {
        try {
            getService().update(context, harvestedCollection);
            context.commit();
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
        return harvestedCollection;
    }

    @Override
    public void delete(Context c, HarvestedCollection harvestedCollection) throws Exception {
        harvestedCollection = c.reloadEntity(harvestedCollection);
        if (harvestedCollection != null) {
            getService().delete(c, harvestedCollection);
        }
    }

    @Override
    protected HarvestedCollectionService getService() {
        return HarvestServiceFactory.getInstance().getHarvestedCollectionService();
    }

}
