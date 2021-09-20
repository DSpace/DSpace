/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.junit.Test;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DateValueGeneratorTest {

    private DateValueGenerator dateGenerator = new DateValueGenerator();

    @Test
    public void dateInStandardFormat() {
        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final Item templateItem = mock(Item.class);
        final String extraParams = "";

        final List<MetadataValueVO> generatedValueList =
                dateGenerator.generator(context, item, templateItem, extraParams);
        final MetadataValueVO generatedValue = generatedValueList.get(0);

        assertThat(generatedValueList.size(), is(1));
        assertThat(generatedValue.getValue(), is(new Date().toString()));
        assertThat(generatedValue.getAuthority(), nullValue());
        assertThat(generatedValue.getConfidence(), is(-1));
    }

    @Test
    public void dateInCustomFormat() {
        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final Item templateItem = mock(Item.class);
        final String extraParams = "yyyy-MM-dd";

        final List<MetadataValueVO> generatedValueList =
                dateGenerator.generator(context, item, templateItem, extraParams);
        final MetadataValueVO generatedValue = generatedValueList.get(0);

        assertThat(generatedValueList.size(), is(1));
        assertThat(generatedValue.getValue(), is(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())));
        assertThat(generatedValue.getAuthority(), nullValue());
        assertThat(generatedValue.getConfidence(), is(-1));
    }
}
