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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link CurrentUserValueGenerator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CurrentUserValueGeneratorTest {

    @Mock
    private Context context;

    @Mock
    private Item targetItem;

    @Mock
    private Item templateItem;

    private String extraParams = "";

    private CurrentUserValueGenerator generator = new CurrentUserValueGenerator();

    @Test
    public void testWithoutUserInTheContext() {

        List<MetadataValueVO> metadataValueList = generator.generator(context, targetItem, templateItem, extraParams);
        MetadataValueVO metadataValue = metadataValueList.get(0);

        assertThat(metadataValueList.size(), is(1));
        assertThat(metadataValue, notNullValue());
        assertThat(metadataValue.getValue(), is(""));
        assertThat(metadataValue.getAuthority(), nullValue());
        assertThat(metadataValue.getConfidence(), is(-1));

    }

    @Test
    public void testWithUserInTheContext() {

        EPerson currentUser = buildEPersonMock("25ad8d1a-e00f-4077-b2a2-326822d6aea4", "User");
        when(context.getCurrentUser()).thenReturn(currentUser);

        List<MetadataValueVO> metadataValueList = generator.generator(context, targetItem, templateItem, extraParams);
        MetadataValueVO metadataValue = metadataValueList.get(0);

        assertThat(metadataValueList.size(), is(1));
        assertThat(metadataValue, notNullValue());
        assertThat(metadataValue.getValue(), is("User"));
        assertThat(metadataValue.getAuthority(), is("25ad8d1a-e00f-4077-b2a2-326822d6aea4"));
        assertThat(metadataValue.getConfidence(), is(600));

    }

    private EPerson buildEPersonMock(String uuid, String name) {
        EPerson ePerson = mock(EPerson.class);
        when(ePerson.getID()).thenReturn(UUID.fromString(uuid));
        when(ePerson.getName()).thenReturn(name);
        return ePerson;
    }

}
