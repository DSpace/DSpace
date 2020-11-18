/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;


import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.collections4.IteratorUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.HarvestedCollectionBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link OAIHarvester}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterIT extends AbstractIntegrationTestWithDatabase {

    private OAIHarvester harvester = HarvestServiceFactory.getInstance().getOAIHarvester();

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private Community community;

    private Collection collection;

    @Before
    public void beforeTests() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testRunHarvest() throws Exception {

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource("https://www.openstarts.units.it/dspace-oai/openairecris")
            .withOaiSetId("openaire_cris_projects")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        System.out.println(items);
        assertThat(items, not(empty()));

    }
}
