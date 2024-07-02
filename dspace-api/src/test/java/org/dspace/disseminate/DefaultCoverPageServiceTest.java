package org.dspace.disseminate;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.pdfbox.text.PDFTextStripper;
import org.dspace.AbstractDSpaceTest;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class DefaultCoverPageServiceTest extends AbstractDSpaceTest {

    @Mock
    ConfigurationService configurationService;

    @Mock
    ItemService itemService;

    @InjectMocks
    DefaultCoverPageService sut;

    @Mock
    Context context;

    @Test
    public void canRenderDefaultCoverPage() throws Exception {
        sut.afterPropertiesSet();

        var item = givenItem("MyCollection");

        try (var coverPage = sut.renderCoverDocument(context, item)) {

            assertThat(coverPage.getNumberOfPages(), equalTo(1));

            var text = new PDFTextStripper().getText(coverPage);
            assertThat(text, containsString("MyCollection"));
        }
    }

    private Item givenItem(String withCollectionName) {
        var item = Mockito.mock(Item.class);

        var collection = Mockito.mock(Collection.class);
        Mockito.doReturn(collection).when(item).getOwningCollection();
        Mockito.doReturn(withCollectionName).when(collection).getName();

        return item;
    }
}