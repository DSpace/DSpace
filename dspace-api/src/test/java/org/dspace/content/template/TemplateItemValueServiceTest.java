/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class TemplateItemValueServiceTest {


    @Test(expected = UnsupportedOperationException.class)
    public void emptyValueListThrowsException() {
        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final Item templateItem = mock(Item.class);

        new TemplateItemValueService(Collections.emptyList()).value(context, item, templateItem,
                                                                    metadataValue("aValue"));
    }

    @Test
    public void templateItemValueNotFoundReturnsSimpleValue() {

        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final Item templateItem = mock(Item.class);

        final TemplateItemValueService templateItemValueService = new TemplateItemValueService(
            Collections.singletonList(fakeTemplateItemValue(false, "value", null)));

        final List<MetadataValueVO> valueList = templateItemValueService.value(context, item, templateItem,
            metadataValue("aValue", "authority"));

        final MetadataValueVO value = valueList.get(0);

        assertThat(valueList.size(), is(1));
        assertThat(value.getValue(), is("aValue"));
        assertThat(value.getAuthority(), is("authority"));

    }

    @Test
    public void templateItemValueFoundAndActualValueReturned() {

        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final Item templateItem = mock(Item.class);

        final TemplateItemValueService templateItemValueService = new TemplateItemValueService(
            Arrays.asList(
                fakeTemplateItemValue(false, "NOTReturnedValue", null),
                fakeTemplateItemValue(true, "RETURNED Value", "7fefea3a-757b-4bf4-a8c5-bc478214700f")));

        final List<MetadataValueVO> valueFromServiceList = templateItemValueService.value(context, item, templateItem,
            metadataValue("aValue"));

        final MetadataValueVO valueFromService = valueFromServiceList.get(0);

        assertThat(valueFromServiceList.size(), is(1));
        assertThat(valueFromService.getValue(), is("RETURNED Value"));
        assertThat(valueFromService.getAuthority(), is("7fefea3a-757b-4bf4-a8c5-bc478214700f"));

    }


    private TemplateItemValue fakeTemplateItemValue(boolean appliesTo, String returnedValue, String returnedAuthority) {
        return new TemplateItemValue() {

            @Override
            public List<MetadataValueVO> values(final Context context, final Item targetItem,
                                       final Item templateItem, final MetadataValue metadataValue) {
                return Arrays.asList(new MetadataValueVO(returnedValue, returnedAuthority));
            }

            @Override
            public boolean appliesTo(final String metadataValue) {
                return appliesTo;
            }
        };
    }

    private MetadataValue metadataValue(final String aValue) {
        final MetadataValue metadataValue = Mockito.mock(MetadataValue.class);
        when(metadataValue.getValue()).thenReturn(aValue);
        return metadataValue;
    }

    private MetadataValue metadataValue(final String aValue, final String authority) {
        final MetadataValue metadataValue = Mockito.mock(MetadataValue.class);
        when(metadataValue.getValue()).thenReturn(aValue);
        when(metadataValue.getAuthority()).thenReturn(authority);
        return metadataValue;
    }
}
