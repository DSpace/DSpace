/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/* Created for LINDAT/CLARIAH-CZ (UFAL) */
/**
 * Tests for PID configuration.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class PIDConfigurationTest extends AbstractUnitTest {
    private static final String AUTHOR = "Test author name";

    private Collection col;
    private Community com;
    private Community subCom;
    private Item publicItem;

    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

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

    @Ignore("Ignore unless the epic consortium will be configured")
    @Test
    public void testItemHandle() {
        String handle = publicItem.getHandle();
        String[] prefixAndSuffix = handle.split("/");
        if (prefixAndSuffix.length != 2) {
            fail("Wrong handle format.");
        }

        String[] customSuffix = prefixAndSuffix[1].split("-");
        if (customSuffix.length != 2) {
            fail("Wrong custom suffix format.");
        }

        assertEquals("123456789/1-" + customSuffix[1], handle);
    }
    @Test
    public void testCollectionHandle() {
        String handle = col.getHandle();
        assertEquals("123456789/" + (handle.split("/"))[1], handle);
    }
//
//    /*
//    This is the config from test local.cfg:
//    lr.pid.community.configurations = community=47501cdc-e2eb-44e5-85e0-89a31dc8ceee, prefix=123456789, type=epic, canonical_prefix=http://hdl.handle.net/, subprefix=1
//    lr.pid.community.configurations = community=09f09b11-cba1-4c43-9e01-29fe919991ab, prefix=123456789, type=epic, canonical_prefix=http://hdl.handle.net/, subprefix=2
//    lr.pid.community.configurations = community=*, prefix=123456789, type=epic, canonical_prefix=http://hdl.handle.net/, subprefix=2
//*/
    @Test
    public void testInitMultipleCommunityConfigs() {
        PIDConfiguration pidConfiguration = PIDConfiguration.getInstance();
        // now check that we have 2 community configurations in the test local.cfg
        assertEquals(2, pidConfiguration.getPIDCommunityConfigurations().size());
    }
//
    @Test
    public  void testInitCommunityConfigSubprefix() {
        PIDConfiguration pidConfiguration = PIDConfiguration.getInstance();
        // get the first one and check the subprefix is 1
        PIDCommunityConfiguration pidCommunityConfiguration = pidConfiguration.getPIDCommunityConfiguration(
                UUID.fromString("47501cdc-e2eb-44e5-85e0-89a31dc8ceee"));
        assertEquals("Subprefix should be 1", "1", pidCommunityConfiguration.getSubprefix());
    }

    @Test
    public  void testInitCommunityConfigMapShouldNotBeShared() throws NoSuchFieldException, IllegalAccessException {
        PIDConfiguration pidConfiguration = PIDConfiguration.getInstance();
        PIDCommunityConfiguration pidCommunityConfiguration1 =
                pidConfiguration.getPIDCommunityConfiguration(
                        UUID.fromString("47501cdc-e2eb-44e5-85e0-89a31dc8ceee"));
        PIDCommunityConfiguration pidCommunityConfiguration2 =
                pidConfiguration.getPIDCommunityConfiguration(null);
        assertEquals("Com2 should have local type", "local", pidCommunityConfiguration2.getType());
        // get the private PIDCommunityConfiguration.configMap via reflection
        Field field = PIDCommunityConfiguration.class.getDeclaredField("configMap");
        field.setAccessible(true);
        Map<String, String> configMap = (Map<String, String>) field.get(pidCommunityConfiguration1);
        configMap.put("type", "epic");
        assertEquals("Com2 should still have local type", "local", pidCommunityConfiguration2.getType());
    }
}
