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

import com.google.common.base.Splitter;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
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
        // The reported Class should match our mocked fields class
        assertEquals(fields.getClass(), concatenate.getFields().getClass(), "TestGetFields 0");
    }

    @Test
    public void testSetFields() {
        // Setup objects utilized in unit test
        concatenate.setFields(fields);
        // The reported Class should match our mocked fields class
        assertEquals(fields, concatenate.getFields(), "TestSetFields 0");
    }

    @Test
    public void testGetSeperator() {
        // Setup objects utilized in unit test
        String separator = ",";
        concatenate.setSeparator(",");

        // The reported separator should match our defined separator
        assertEquals(separator, concatenate.getSeparator(), "TestGetSeperator 0");
    }

    @Test
    public void testSetSeperator() {
        // Setup objects utilized in unit test
        concatenate.setSeparator(",");

        // The reported separator should match our defined separator
        assertEquals(",", concatenate.getSeparator(), "TestSetSeperator 0");
    }

    @Test
    public void testSetUseForPlace() {
        // Setup objects utilized in unit test
        concatenate.setUseForPlace(true);

        // The reported separator should match our defined separator
        assertEquals(true, concatenate.getUseForPlace(), "TestSetUseForPlace 0");

    }

    @Test
    public void testGetUseForPlace() {
        // Setup objects utilized in unit test
        boolean bool = true;
        concatenate.setUseForPlace(true);

        // The reported boolean should match our defined bool
        assertEquals(bool, concatenate.getUseForPlace(), "TestGetUseForPlace 0");
    }

    @Test
    public void testGetValues() {
        // Setup objects utilized in unit test
        List<String> list = new ArrayList<>();
        List<String> valueList = new ArrayList<>();
        List<MetadataValue> metadataValueList = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        Item item = mock(Item.class);
        metadataValueList.add(metadataValue);
        String s = "dc.title";
        list.add(s);
        List<String> splittedString = Splitter.on(".").splitToList(s);
        concatenate.setFields(list);
        valueList.add("TestValue");

        // Mock the state of objects utilized in getValues() to meet the success criteria of an invocation
        when(itemService.getMetadata(item, splittedString.size() > 0 ? splittedString.get(0) : null,
            splittedString.size() > 1 ? splittedString.get(1) : null,
            splittedString.size() > 2 ? splittedString.get(2) : null,
            Item.ANY, false)).thenReturn(metadataValueList);
        when(metadataValue.getValue()).thenReturn("TestValue");


        // The reported values should match our defined valueList
        assertEquals(valueList, concatenate.getValues(context, item), "TestGetValues 0");
    }
}
