
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
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutCell;
import org.dspace.layout.DynamicLayoutRow;
import org.dspace.layout.DynamicLayoutTab;
import org.dspace.layout.dao.DynamicLayoutTabDAO;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link DynamicLayoutTabServiceImpl} class.
 * Temporarily, only findByItem method is covered
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicLayoutTabServiceImplTest {

    @Mock
    private Context context;
    @Mock
    private DynamicLayoutTabDAO tabDao;
    @Mock
    private AuthorizeService authorizeService;
    @Mock
    private ItemService itemService;
    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private DynamicLayoutTabServiceImpl dynamicLayoutTabService;

    @Test
    public void allTabsAreReturned() throws SQLException {
        String itemUuid = UUID.randomUUID().toString();
        Item item = mock(Item.class);
        String entityType = "relationshipEntity";

        List<MetadataValue> itemMetadata = Arrays.asList(metadataValue(), metadataValue());

        DynamicLayoutTab tabOne = grantedAccessTab("tab1",
                                                boxWithContent(item, itemMetadata),
                                                boxWithoutContent(item, itemMetadata),
                                                restrictedBox(item, itemMetadata));
        DynamicLayoutTab tabTwo = grantedAccessTab("tab2",
                                                restrictedBox(item, itemMetadata),
                                                boxWithContent(item, itemMetadata),
                                                boxWithContent(item, itemMetadata));
        DynamicLayoutTab tabThree = grantedAccessTab("tab3",
                                                  boxWithoutContent(item, itemMetadata),
                                                  boxWithoutContent(item, itemMetadata));

        DynamicLayoutTab tabWithoutBoxes = grantedAccessTab("empty");
        DynamicLayoutTab tabWithOnlyForbiddenBoxes = grantedAccessTab("forbidden",
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

        List<DynamicLayoutTab> tabs = dynamicLayoutTabService.findByItem(context, itemUuid);

        assertThat(tabs.stream().map(DynamicLayoutTab::getShortName).collect(toList()),
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

        List<DynamicLayoutTab> tabs = dynamicLayoutTabService.findByItem(context, itemUuid);

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

        List<DynamicLayoutTab> tabs = dynamicLayoutTabService.findByItem(context, itemUuid);

        assertThat(tabs, is(emptyList()));
    }

    @Test(expected = NullPointerException.class)
    public void nullItemThrowsNullPointerException() throws SQLException {
        UUID itemUuid = UUID.randomUUID();

        when(itemService.find(context, itemUuid)).thenReturn(null);

        dynamicLayoutTabService.findByItem(context, itemUuid.toString());
    }


    private DynamicLayoutTab grantedAccessTab(String shortName, DynamicLayoutBox... boxes) throws SQLException {
        return tab(shortName, true, boxes);
    }

    private DynamicLayoutTab forbiddenAccessTab(String shortName, DynamicLayoutBox... boxes) throws SQLException {
        return tab(shortName, false, boxes);
    }

    private DynamicLayoutTab tab(String shortName, boolean grantedAccess, DynamicLayoutBox...boxes)
            throws SQLException {
        DynamicLayoutTab tab = new DynamicLayoutTab();
        tab.setShortName(shortName);

        for (DynamicLayoutBox box : boxes) {
            DynamicLayoutRow row = new DynamicLayoutRow();
            DynamicLayoutCell cell = new DynamicLayoutCell();
            tab.addRow(row);
            row.addCell(cell);
            cell.addBox(box);
        }

        return tab;
    }

    private MetadataValue metadataValue() {
        return mock(MetadataValue.class);
    }

    private DynamicLayoutBox boxWithContent(Item item, List<MetadataValue> itemMetadata) throws SQLException {
        return box(item, itemMetadata, true, true);
    }

    private DynamicLayoutBox boxWithoutContent(Item item, List<MetadataValue> itemMetadata) throws SQLException {
        return box(item, itemMetadata, false, true);
    }

    private DynamicLayoutBox restrictedBox(Item item, List<MetadataValue> itemMetadata) throws SQLException {
        return box(item, itemMetadata, true, false);
    }

    private DynamicLayoutBox box(Item item, List<MetadataValue> itemMetadata, boolean hasContent, boolean grantedAccess)
        throws SQLException {
        DynamicLayoutBox box = new DynamicLayoutBox();
        box.setId(new Random().nextInt(10000));
        return box;
    }
}
