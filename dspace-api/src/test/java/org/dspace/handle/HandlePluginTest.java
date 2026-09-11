/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.handle.hdllib.Util;
import org.dspace.AbstractDSpaceTest;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link HandlePlugin#haveNA(byte[])}.
 */
public class HandlePluginTest extends AbstractDSpaceTest {
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected ConfigurationService configurationService = new DSpace().getConfigurationService();
    private HandlePlugin plugin;

    @Before
    public void setUpPlugin() {
        configurationService.setProperty("handle.prefix", "123456789");
        configurationService.setProperty("handle.additional.prefixes", "987654321, 654321987");
        configurationService.setProperty("handle.plugin.checknameauthority", true);

        plugin = new HandlePlugin();
        plugin.handleService = handleService;
        plugin.configurationService = configurationService;
    }

    @Test
    public void haveNAAcceptsPrimaryPrefix() throws Exception {
        assertTrue(plugin.haveNA(Util.encodeString("0.NA/123456789")));
    }

    @Test
    public void haveNARejectsUnknownPrefix() throws Exception {
        assertFalse(plugin.haveNA(Util.encodeString("0.NA/999999999")));
    }

    @Test
    public void haveNAAcceptsAdditionalPrefixes() throws Exception {
        assertTrue(plugin.haveNA(Util.encodeString("0.NA/987654321")));
        assertTrue(plugin.haveNA(Util.encodeString("0.NA/654321987")));
    }

    @Test
    public void haveNAAlwaysTrueWhenCheckDisabled() throws Exception {
        configurationService.setProperty("handle.plugin.checknameauthority", false);
        assertTrue(plugin.haveNA(Util.encodeString("0.NA/999999999")));
    }
}
