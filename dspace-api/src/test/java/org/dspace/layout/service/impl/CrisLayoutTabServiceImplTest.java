
/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutCell;
import org.dspace.layout.CrisLayoutRow;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.dao.CrisLayoutTabDAO;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link CrisLayoutTabServiceImpl} class.
 * Temporarily, only findByItem method is covered
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
@RunWith(MockitoJUnitRunner.class)
public class CrisLayoutTabServiceImplTest {

    @Mock
    private Context context;
    @Mock
    private CrisLayoutTabDAO tabDao;
    @Mock
    private AuthorizeService authorizeService;
    @Mock
    private ItemService itemService;
    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private CrisLayoutTabServiceImpl crisLayoutTabService;

    @Test
    public void allTabsAreReturned() throws SQLException {
        String itemUuid = UUID.randomUUID().toString();
        Item item = mock(Item.class);
        String entityType = "relationshipEntity";

        List<MetadataValue> itemMetadata = Arrays.asList(metadataValue(), metadataValue());

        CrisLayoutTab tabOne = grantedAccessTab("tab1",
                                                boxWithContent(item, itemMetadata),
                                                boxWithoutContent(item, itemMetadata),
                                                restrictedBox(item, itemMetadata));
        CrisLayoutTab tabTwo = grantedAccessTab("tab2",
                                                restrictedBox(item, itemMetadata),
                                                boxWithContent(item, itemMetadata),
                                                boxWithContent(item, itemMetadata));
        CrisLayoutTab tabThree = grantedAccessTab("tab3",
                                                  boxWithoutContent(item, itemMetadata),
                                                  boxWithoutContent(item, itemMetadata));

        CrisLayoutTab tabWithoutBoxes = grantedAccessTab("empty");
        CrisLayoutTab tabWithOnlyForbiddenBoxes = grantedAccessTab("forbidden",
                                                                   restrictedBox(item, itemMetadata),
                                                                   restrictedBox(item, itemMetadata),
                                                                   restrictedBox(item, itemMetadata),
                                                                   restrictedBox(item, itemMetadata));

        forbiddenAccessTab("forbidden-tab",
                           boxWithContent(item, itemMetadata),
                           boxWithContent(item, itemMetadata),
                           boxWithoutContent(item, itemMetadata));

        when(itemService.find(context, UUID.fromString(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "dspace.entity.type"))
            .thenReturn(entityType);

        when(tabDao.findByEntityTypeAndEagerlyFetchBoxes(context, entityType, null))
            .thenReturn(Arrays.asList(tabOne, tabTwo, tabThree, tabWithoutBoxes, tabWithOnlyForbiddenBoxes));

        List<CrisLayoutTab> tabs = crisLayoutTabService.findByItem(context, itemUuid);

        assertThat(tabs.stream().map(CrisLayoutTab::getShortName).collect(toList()),
            containsInAnyOrder("tab1", "tab2", "tab3", "empty", "forbidden"));

    }

    @Test
    public void noTabsFoundForEntityType() throws SQLException {
        String itemUuid = UUID.randomUUID().toString();
        String entityType = UUID.randomUUID().toString();

        Item item = mock(Item.class);

        when(itemService.find(context, UUID.fromString(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "dspace.entity.type"))
            .thenReturn(entityType);

        when(tabDao.findByEntityTypeAndEagerlyFetchBoxes(context, entityType, null)).thenReturn(emptyList());

        List<CrisLayoutTab> tabs = crisLayoutTabService.findByItem(context, itemUuid);

        assertThat(tabs, is(emptyList()));
    }

    @Test
    public void nullTabsFoundForEntityType() throws SQLException {
        String itemUuid = UUID.randomUUID().toString();
        String entityType = UUID.randomUUID().toString();

        Item item = mock(Item.class);

        when(itemService.find(context, UUID.fromString(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "dspace.entity.type"))
            .thenReturn(entityType);

        when(tabDao.findByEntityTypeAndEagerlyFetchBoxes(context, entityType, null)).thenReturn(List.of());

        List<CrisLayoutTab> tabs = crisLayoutTabService.findByItem(context, itemUuid);

        assertThat(tabs, is(emptyList()));
    }

    @Test(expected = NullPointerException.class)
    public void nullItemThrowsNullPointerException() throws SQLException {
        UUID itemUuid = UUID.randomUUID();

        when(itemService.find(context, itemUuid)).thenReturn(null);

        crisLayoutTabService.findByItem(context, itemUuid.toString());
    }


    private CrisLayoutTab grantedAccessTab(String shortName, CrisLayoutBox... boxes) throws SQLException {
        return tab(shortName, true, boxes);
    }

    private CrisLayoutTab forbiddenAccessTab(String shortName, CrisLayoutBox... boxes) throws SQLException {
        return tab(shortName, false, boxes);
    }

    private CrisLayoutTab tab(String shortName, boolean grantedAccess, CrisLayoutBox...boxes) throws SQLException {
        CrisLayoutTab tab = new CrisLayoutTab();
        tab.setShortName(shortName);

        for (CrisLayoutBox box : boxes) {
            CrisLayoutRow row = new CrisLayoutRow();
            CrisLayoutCell cell = new CrisLayoutCell();
            tab.addRow(row);
            row.addCell(cell);
            cell.addBox(box);
        }

        return tab;
    }

    private MetadataValue metadataValue() {
        return mock(MetadataValue.class);
    }

    private CrisLayoutBox boxWithContent(Item item, List<MetadataValue> itemMetadata) throws SQLException {
        return box(item, itemMetadata, true, true);
    }

    private CrisLayoutBox boxWithoutContent(Item item, List<MetadataValue> itemMetadata) throws SQLException {
        return box(item, itemMetadata, false, true);
    }

    private CrisLayoutBox restrictedBox(Item item, List<MetadataValue> itemMetadata) throws SQLException {
        return box(item, itemMetadata, true, false);
    }

    private CrisLayoutBox box(Item item, List<MetadataValue> itemMetadata, boolean hasContent, boolean grantedAccess)
        throws SQLException {
        CrisLayoutBox box = new CrisLayoutBox();
        box.setId(new Random().nextInt(10000));
        return box;
    }
}
