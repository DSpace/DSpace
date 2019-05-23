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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class UUIDValueTest {

    @InjectMocks
    private UUIDValue UUIDValue;

    @Mock
    private Context context;

    @Test
    public void testGetValues() throws Exception {
        List<String> list = new LinkedList<>();
        Item item = mock(Item.class);
        UUID uuid = UUID.randomUUID();
        when(item.getID()).thenReturn(uuid);
        list.add(String.valueOf(uuid));
        assertEquals("TestGetValues 0", list, UUIDValue.getValues(context, item));
    }

    @Test
    public void testSetUseForPlace() {
        UUIDValue.setUseForPlace(true);
        assertEquals("TestSetUseForPlace 0", true, UUIDValue.getUseForPlace());

    }

    @Test
    public void testGetUseForPlace() {
        UUIDValue.setUseForPlace(true);
        assertEquals("TestGetUseForPlace 0", true, UUIDValue.getUseForPlace());
    }
}
