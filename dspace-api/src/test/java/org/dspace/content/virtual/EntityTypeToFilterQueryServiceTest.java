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
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EntityTypeToFilterQueryServiceTest {

    @InjectMocks
    private EntityTypeToFilterQueryService entityTypeToFilterQueryService;

    @Test
    public void testSetMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        entityTypeToFilterQueryService.setMap(map);
        assertEquals("TestSetMap 0", map, entityTypeToFilterQueryService.getMap());
    }

    @Test
    public void testGetMap() {
        Map<String, String> map = Collections.emptyMap();
        entityTypeToFilterQueryService.setMap(map);
        assertEquals("TestGetFields 0", map, entityTypeToFilterQueryService.getMap());
    }

    @Test
    public void testHasKey() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        entityTypeToFilterQueryService.setMap(map);
        assertEquals("TestHasKey 0", true, entityTypeToFilterQueryService.hasKey("key"));
    }

    @Test
    public void testGetFilterQueryForKey() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        entityTypeToFilterQueryService.setMap(map);
        assertEquals("TestGetFilterQueryForKey 0", "value",
                entityTypeToFilterQueryService.getFilterQueryForKey("key"));
    }
}
