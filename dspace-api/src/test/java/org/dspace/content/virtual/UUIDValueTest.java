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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class UUIDValueTest {

    @InjectMocks
    private UUIDValue uuidValue;

    @Mock
    private Context context;

    @Test
    public void testGetValues() throws Exception {
        // Setup objects utilized in unit test
        List<String> list = new ArrayList<>();
        Item item = mock(Item.class);
        UUID uuid = UUID.randomUUID();
        when(item.getID()).thenReturn(uuid);
        list.add(String.valueOf(uuid));

        // The reported value(s) should match our defined list
        assertEquals(list, uuidValue.getValues(context, item), "TestGetValues 0");
    }

    @Test
    public void testSetUseForPlace() {
        // Setup objects utilized in unit test
        uuidValue.setUseForPlace(true);

        // The reported boolean should return true
        assertEquals(true, uuidValue.getUseForPlace(), "TestSetUseForPlace 0");

    }

    @Test
    public void testGetUseForPlace() {
        // Setup objects utilized in unit test
        uuidValue.setUseForPlace(true);

        // The reported boolean should return true
        assertEquals(true, uuidValue.getUseForPlace(), "TestGetUseForPlace 0");
    }
}
