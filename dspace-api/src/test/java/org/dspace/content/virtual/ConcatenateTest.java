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

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConcatenateTest {

    @InjectMocks
    private Concatenate concatenate;

    /**
     * The fields for which the metadata will be retrieved
     */
    @Mock
    private List<String> fields;

    @Mock
    private ItemService itemService;

    @Mock
    private Context context;


    @Test
    public void testGetFields() {
        assertEquals("TestGetFields 0", fields.getClass(), concatenate.getFields().getClass());
    }

    @Test
    public void testSetFields() {
        concatenate.setFields(fields);
        assertEquals("TestSetFields 0", fields, concatenate.getFields());
    }

    @Test
    public void testGetSeperator() {
        String seperator = ",";
        concatenate.setSeparator(",");
        assertEquals("TestGetSeperator 0", seperator, concatenate.getSeparator());
    }

    @Test
    public void testSetSeperator() {
        concatenate.setSeparator(",");
        assertEquals("TestSetSeperator 0", ",", concatenate.getSeparator());
    }

    @Test
    public void testSetUseForPlace() {
        concatenate.setUseForPlace(true);
        assertEquals("TestSetUseForPlace 0", true, concatenate.getUseForPlace());

    }

    @Test
    public void testGetUseForPlace() {
        boolean bool = true;
        concatenate.setUseForPlace(true);
        assertEquals("TestGetUseForPlace 0", bool, concatenate.getUseForPlace());
    }

    @Test
    public void testGetValues() {
        List<String> list = new ArrayList<>();
        List<String> valueList = new ArrayList<>();
        List<MetadataValue> metadataValueList = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        Item item = mock(Item.class);
        metadataValueList.add(metadataValue);
        String s = "dc.title";
        list.add(s);
        String[] splittedString = s.split("\\.");
        concatenate.setFields(list);
        when(itemService.getMetadata(item, splittedString.length > 0 ? splittedString[0] :
                        null,
                splittedString.length > 1 ? splittedString[1] :
                        null,
                splittedString.length > 2 ? splittedString[2] :
                        null,
                Item.ANY, false)).thenReturn(metadataValueList);
        when(metadataValue.getValue()).thenReturn("TestValue");
        valueList.add("TestValue");
        assertEquals("TestGetValues 0", valueList, concatenate.getValues(context, item));
    }
}
