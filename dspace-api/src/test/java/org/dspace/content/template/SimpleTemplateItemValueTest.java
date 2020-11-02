package org.dspace.content.template;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.junit.Test;

/**
 * Unit tests for {@link SimpleTemplateItemValue}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class SimpleTemplateItemValueTest {

    @Test
    public void valueWithoutPlaceholderReturned() {
        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final Item templateItem = mock(Item.class);
        final MetadataValue metadataValue = metadataValue("Simple value");

        final SimpleTemplateItemValue simpleTemplateItemValue = new SimpleTemplateItemValue();

        final boolean applies = simpleTemplateItemValue.appliesTo(metadataValue.getValue());
        final String actualValue = simpleTemplateItemValue.value(context, item, templateItem, metadataValue).getValue();

        assertThat(applies, is(true));
        assertThat(actualValue, is("Simple value"));
    }

    @Test
    public void valueWithPlaceholderNotReturned() {


        final String metadataValue = "###DATE###";

        final SimpleTemplateItemValue simpleTemplateItemValue = new SimpleTemplateItemValue();

        final boolean applies = simpleTemplateItemValue.appliesTo(metadataValue);

        assertThat(applies, is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void directCallWithPlaceholderThrowsException() {

        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final Item templateItem = mock(Item.class);
        final MetadataValue metadataValue = metadataValue("###DATE###");

        final SimpleTemplateItemValue simpleTemplateItemValue = new SimpleTemplateItemValue();

        simpleTemplateItemValue.value(context, item, templateItem, metadataValue);
    }

    private MetadataValue metadataValue(final String value) {
        final MetadataValue metadataValue = mock(MetadataValue.class);
        when(metadataValue.getValue()).thenReturn(value);
        return metadataValue;
    }
}
