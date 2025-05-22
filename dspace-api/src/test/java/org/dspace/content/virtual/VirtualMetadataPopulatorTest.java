/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.RelationshipType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class VirtualMetadataPopulatorTest {

    @InjectMocks
    private VirtualMetadataPopulator virtualMetadataPopulator;

    @Test
    public void testSetMap() {
        // Setup objects utilized in unit test
        Map<String, HashMap<String, VirtualMetadataConfiguration>> map = new HashMap<>();
        HashMap<String, VirtualMetadataConfiguration> mapExt = new HashMap<>();
        VirtualMetadataConfiguration virtualMetadataConfiguration = mock(VirtualMetadataConfiguration.class);
        mapExt.put("hashKey", virtualMetadataConfiguration);
        map.put("key", mapExt);
        virtualMetadataPopulator.setMap(map);

        // The returned map should match our defined map
        assertEquals(map, virtualMetadataPopulator.getMap(), "TestSetMap 0");
    }

    @Test
    public void testGetMap() {
        // Setup objects utilized in unit test
        Map<String, HashMap<String, VirtualMetadataConfiguration>> map = new HashMap<>();
        HashMap<String, VirtualMetadataConfiguration> mapExt = new HashMap<>();
        VirtualMetadataConfiguration virtualMetadataConfiguration = mock(VirtualMetadataConfiguration.class);
        mapExt.put("hashKey", virtualMetadataConfiguration);
        map.put("key", mapExt);
        virtualMetadataPopulator.setMap(map);

        // The returned map should match our defined map
        assertEquals(map, virtualMetadataPopulator.getMap(), "TestGetMap 0");
    }

    @Test
    public void testIsUseForPlaceTrueForRelationshipType() {
        // Setup objects utilized in unit test
        RelationshipType relationshipType = mock(RelationshipType.class);
        Map<String, HashMap<String, VirtualMetadataConfiguration>> map = new HashMap<>();
        HashMap<String, VirtualMetadataConfiguration> mapExt = new HashMap<>();
        VirtualMetadataConfiguration virtualMetadataConfiguration = mock(VirtualMetadataConfiguration.class);
        mapExt.put("hashKey", virtualMetadataConfiguration);
        map.put("LeftwardType", mapExt);
        map.put("NotRightwardType", mapExt);
        virtualMetadataPopulator.setMap(map);

        // Mock the state of objects utilized in isUseForPlaceTrueForRelationshipType()
        // to meet the success criteria of an invocation
        when(virtualMetadataConfiguration.getUseForPlace()).thenReturn(true);
        when(relationshipType.getLeftwardType()).thenReturn("LeftwardType");
        when(relationshipType.getRightwardType()).thenReturn("RightwardType");

        // Assert that the useForPlace for our mocked relationshipType is false
        assertEquals(false,
            virtualMetadataPopulator.isUseForPlaceTrueForRelationshipType(relationshipType, false),
            "TestGetFields 0");
        // Assert that the useForPlace for our mocked relationshipType is true
        assertEquals(true,
            virtualMetadataPopulator.isUseForPlaceTrueForRelationshipType(relationshipType, true),
            "TestGetFields 1");
    }
}
