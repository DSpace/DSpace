/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dspace.AbstractDSpaceTest;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.junit.jupiter.api.Test;

/**
 * Tests for Plugin Service.
 * <P>
 * NOTE: Plugin definitions/configurations which are used for this test are
 * defined in /src/test/data/dspaceFolder/config/local.cfg
 *
 * @author Tim Donohue
 */
public class PluginServiceTest extends AbstractDSpaceTest {
    // Get our enabled pluginService
    private PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    /**
     * Test of getAllPluginNames() method
     */
    @Test
    public void testGetAllPluginNames() {
        // Get all plugins defined from List interface (see test local.cfg)
        String[] names = pluginService.getAllPluginNames(java.util.List.class);

        // There should be exactly 3 List plugins
        assertEquals(3, names.length, "Plugin count");
    }

    /**
     * Test of getNamedPlugin() method
     */
    @Test
    public void testGetNamedPlugin() {
        // Get the plugin named "MyArrayList"
        Object plugin = pluginService.getNamedPlugin(java.util.List.class, "MyArrayList");

        assertNotNull(plugin, "Plugin exists");
        assertTrue(plugin instanceof java.util.List, "Plugin is List");
        assertTrue(plugin instanceof java.util.ArrayList, "Plugin is ArrayList");

        // Get a plugin that doesn't exist
        plugin = pluginService.getNamedPlugin(java.util.List.class, "MyOtherList");
        assertNull(plugin, "Plugin 2 doesn't exist");

        // Test for one plugin that is "selfnamed"
        // The DCInputAuthority plugin enabled in test local.cfg reads all <form-value-pairs> in submission-forms.xml
        // and defines a self named plugin for each. So, we SHOULD have a "common_types" plugin.
        plugin = pluginService.getNamedPlugin(org.dspace.content.authority.ChoiceAuthority.class, "common_types");
        assertNotNull(plugin, "Plugin 3 exists");
        assertTrue(plugin instanceof org.dspace.content.authority.ChoiceAuthority, "Plugin 3 is ChoiceAuthority");
        assertTrue(plugin instanceof org.dspace.content.authority.DCInputAuthority, "Plugin 3 is DCInputAuthority");
        // NOTE: Additional "selfnamed" plugins are tested in DSpaceControlledVocabularyTest
    }

    /**
     * Test of hasNamedPlugin() method
     */
    @Test
    public void testHasNamedPlugin() {
        // Assert there is a plugin named "MyLinkedList"
        assertTrue(pluginService.hasNamedPlugin(java.util.List.class, "MyLinkedList"));

        // Assert there is NOT a plugin named "MyList"
        assertFalse(pluginService.hasNamedPlugin(java.util.List.class, "MyList"));

        // Assert existence of a self named plugin
        assertTrue(pluginService.hasNamedPlugin(org.dspace.content.authority.ChoiceAuthority.class, "common_types"));
    }

    /**
     * Test of getSinglePlugin() method
     */
    @Test
    public void testGetSinglePlugin() {
        // There should be a SINGLE Map plugin (unnamed)
        Object plugin = pluginService.getSinglePlugin(java.util.Map.class);
        assertNotNull(plugin, "Plugin exists");
        assertTrue(plugin instanceof java.util.Map, "Plugin is Map");
        assertTrue(plugin instanceof java.util.HashMap, "Plugin is HashMap");
    }

    /**
     * Test of getSinglePlugin() method
     */
    @Test
    public void testGetPluginSequence() {
        // There should be a sequence of Collection plugins
        Object[] plugins = pluginService.getPluginSequence(java.util.Collection.class);

        // There should be four of them
        assertEquals(4, plugins.length, "Plugin count");

        // They should be in an EXACT ORDER (as defined in test local.cfg)
        assertTrue(plugins[0] instanceof java.util.ArrayList, "Plugin 0 is ArrayList");
        assertTrue(plugins[1] instanceof java.util.LinkedList, "Plugin 1 is LinkedList");
        assertTrue(plugins[2] instanceof java.util.Stack, "Plugin 2 is Stack");
        assertTrue(plugins[3] instanceof java.util.TreeSet, "Plugin 3 is TreeSet");
    }


}
