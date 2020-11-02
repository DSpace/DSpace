/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.junit.Test;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DateValueGeneratorTest {

    @Test
    public void dateInStandardFormat() {
        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final Item templateItem = mock(Item.class);
        final MetadataValue metadataValue = metadataValue("original value");
        final String extraParams = "";

        final String generatedValue =
            new DateValueGenerator().generator(context, item, templateItem, extraParams);

        assertThat(generatedValue, is(new Date().toString()));
    }

    @Test
    public void dateInCustomFormat() {
        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final Item templateItem = mock(Item.class);
        final MetadataValue metadataValue = metadataValue("original value");
        final String extraParams = "yyyy-MM-dd";

        final String generatedValue =
            new DateValueGenerator().generator(context, item, templateItem, extraParams);

        assertThat(generatedValue, is(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())));
    }

    private MetadataValue metadataValue(final String originalValue) {
        MetadataValue metadataValue = mock(MetadataValue.class);

        doCallRealMethod().when(metadataValue).setValue(anyString());
        doCallRealMethod().when(metadataValue).getValue();

        return metadataValue;
    }
}
