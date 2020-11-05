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

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
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
            Collections.singletonList(fakeTemplateItemValue(false, "value")));

        final String value = templateItemValueService.value(context, item, templateItem, metadataValue("aValue"));

        assertThat(value, is("aValue"));

    }

    @Test
    public void templateItemValueFoundAndActualValueReturned() {

        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final Item templateItem = mock(Item.class);

        final TemplateItemValueService templateItemValueService = new TemplateItemValueService(
            Arrays.asList(
                fakeTemplateItemValue(false, "NOTReturnedValue"),
                fakeTemplateItemValue(true, "RETURNED Value")));

        final String valueFromService = templateItemValueService.value(context, item, templateItem,
                                                                              metadataValue("aValue"));

        assertThat(valueFromService, is("RETURNED Value"));

    }


    private TemplateItemValue fakeTemplateItemValue(final boolean appliesTo, final String returnedValue) {
        return new TemplateItemValue() {

            @Override
            public String value(final Context context, final Item targetItem,
                                       final Item templateItem, final MetadataValue metadataValue) {
                return returnedValue;
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
}
