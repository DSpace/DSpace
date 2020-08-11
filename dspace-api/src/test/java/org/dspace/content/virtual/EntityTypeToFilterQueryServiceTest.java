/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EntityTypeToFilterQueryServiceTest {

    @InjectMocks
    private EntityTypeToFilterQueryService entityTypeToFilterQueryService;

    @Test
    public void testSetMap() {
        // Setup objects utilized in unit test
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        entityTypeToFilterQueryService.setMap(map);

        // The reported map should match our defined map
        assertEquals("TestSetMap 0", map, entityTypeToFilterQueryService.getMap());
    }

    @Test
    public void testGetMap() {
        // Setup objects utilized in unit test
        Map<String, String> map = Collections.emptyMap();
        entityTypeToFilterQueryService.setMap(map);

        // The reported map should match our defined map
        assertEquals("TestGetFields 0", map, entityTypeToFilterQueryService.getMap());
    }

    @Test
    public void testHasKey() {
        // Setup objects utilized in unit test
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        entityTypeToFilterQueryService.setMap(map);

        // The mocked entityTypeToFilterQueryService should report true for hasKey("key")
        assertEquals("TestHasKey 0", true, entityTypeToFilterQueryService.hasKey("key"));
    }

    @Test
    public void testGetFilterQueryForKey() {
        // Setup objects utilized in unit test
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        entityTypeToFilterQueryService.setMap(map);

        // The reported value for our defined key should match our defined value
        assertEquals("TestGetFilterQueryForKey 0", "value",
                entityTypeToFilterQueryService.getFilterQueryForKey("key"));
    }
}
