/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
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
        assertEquals(map, entityTypeToFilterQueryService.getMap(), "TestSetMap 0");
    }

    @Test
    public void testGetMap() {
        // Setup objects utilized in unit test
        Map<String, String> map = Collections.emptyMap();
        entityTypeToFilterQueryService.setMap(map);

        // The reported map should match our defined map
        assertEquals(map, entityTypeToFilterQueryService.getMap(), "TestGetFields 0");
    }

    @Test
    public void testHasKey() {
        // Setup objects utilized in unit test
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        entityTypeToFilterQueryService.setMap(map);

        // The mocked entityTypeToFilterQueryService should report true for hasKey("key")
        assertEquals(true, entityTypeToFilterQueryService.hasKey("key"), "TestHasKey 0");
    }

    @Test
    public void testGetFilterQueryForKey() {
        // Setup objects utilized in unit test
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        entityTypeToFilterQueryService.setMap(map);

        // The reported value for our defined key should match our defined value
        assertEquals("value",
            entityTypeToFilterQueryService.getFilterQueryForKey("key"),
            "TestGetFilterQueryForKey 0");
    }
}
