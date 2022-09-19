/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.service.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.SQLException;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DspaceObjectClarinServiceImpl;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for DspaceObjectClarinServiceImpl.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class DspaceObjectClarinImplTest extends AbstractUnitTest {

    private Collection col;
    private Community com;
    private Community subCom;
    private Item publicItem;

    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private DspaceObjectClarinServiceImpl doClarinService = new DspaceObjectClarinServiceImpl();

    @Before
    public void setup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        // 1. A community-collection structure with one parent community and one collection
        com = communityService.create(null, context);
        communityService.createSubcommunity(context, com);
        subCom = com.getSubcommunities().get(0);
        col = collectionService.create(context, subCom);
        WorkspaceItem workspaceItem = workspaceItemService.create(context, col, true);
        // 2. Create item and add it to the collection
        publicItem = installItemService.installItem(context, workspaceItem);
        context.restoreAuthSystemState();
    }

    @Test
    public void principalCommunityTestOneCommunity() throws SQLException {
        assertThat("principal community",
                doClarinService.getPrincipalCommunity(context, publicItem).getID(), is(equalTo(subCom.getID())));
    }
}
