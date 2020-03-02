/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
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
        assertEquals("TestGetValues 0", list, uuidValue.getValues(context, item));
    }

    @Test
    public void testSetUseForPlace() {
        // Setup objects utilized in unit test
        uuidValue.setUseForPlace(true);

        // The reported boolean should return true
        assertEquals("TestSetUseForPlace 0", true, uuidValue.getUseForPlace());

    }

    @Test
    public void testGetUseForPlace() {
        // Setup objects utilized in unit test
        uuidValue.setUseForPlace(true);

        // The reported boolean should return true
        assertEquals("TestGetUseForPlace 0", true, uuidValue.getUseForPlace());
    }
}
