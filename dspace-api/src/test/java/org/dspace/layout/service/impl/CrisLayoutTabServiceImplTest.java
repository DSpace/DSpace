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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.CrisLayoutTab2Box;
import org.dspace.layout.dao.CrisLayoutTabDAO;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.layout.service.CrisLayoutTabAccessService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private CrisLayoutBoxService boxService;
    @Mock
    private CrisLayoutBoxAccessService crisLayoutBoxAccessService;

    @Mock
    private CrisLayoutTabAccessService crisLayoutTabAccessService;

    private CrisLayoutTabServiceImpl crisLayoutTabService;

    @Before
    public void setUp() throws Exception {
        crisLayoutTabService = new CrisLayoutTabServiceImpl(tabDao, authorizeService, itemService, boxService,
                                                            crisLayoutBoxAccessService, crisLayoutTabAccessService);
    }

    @Test
    public void onlyGrantedTabsContainingGrantedBoxesAreReturned() throws SQLException {
        String itemUuid = UUID.randomUUID().toString();
        Item item = mock(Item.class);
        String entityType = "relationshipEntity";

        List<MetadataValue> itemMetadata = Arrays.asList(metadataValue(), metadataValue());

        CrisLayoutTab tabOne = grantedAccessTab("tab1",
                                                boxWithContent(itemMetadata),
                                                boxWithoutContent(itemMetadata),
                                                restrictedBox(itemMetadata));
        CrisLayoutTab tabTwo = grantedAccessTab("tab2",
                                                restrictedBox(itemMetadata),
                                                boxWithContent(itemMetadata),
                                                boxWithContent(itemMetadata));
        CrisLayoutTab tabThree = grantedAccessTab("tab3",
                                                  boxWithoutContent(itemMetadata),
                                                  boxWithoutContent(itemMetadata));

        CrisLayoutTab tabWithoutBoxes = grantedAccessTab("empty");
        CrisLayoutTab tabWithOnlyForbiddenBoxes = grantedAccessTab("forbidden",
                                                                   restrictedBox(itemMetadata),
                                                                   restrictedBox(itemMetadata),
                                                                   restrictedBox(itemMetadata),
                                                                   restrictedBox(itemMetadata));

        forbiddenAccessTab("forbidden-tab",
                           boxWithContent(itemMetadata),
                           boxWithContent(itemMetadata),
                           boxWithoutContent(itemMetadata));

        when(itemService.find(context, UUID.fromString(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn(entityType);

        when(item.getMetadata()).thenReturn(itemMetadata);

        when(tabDao.findByEntityType(context, entityType))
            .thenReturn(Arrays.asList(tabOne, tabTwo, tabThree, tabWithoutBoxes, tabWithOnlyForbiddenBoxes));

        List<CrisLayoutTab> tabs = crisLayoutTabService.findByItem(context, itemUuid);

        assertThat(tabs.stream().map(CrisLayoutTab::getShortName).collect(toList()),
                   containsInAnyOrder("tab1", "tab2"));

    }

    @Test
    public void noTabsFoundForEntityType() throws SQLException {
        String itemUuid = UUID.randomUUID().toString();
        String entityType = UUID.randomUUID().toString();

        Item item = mock(Item.class);

        when(itemService.find(context, UUID.fromString(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn(entityType);

        when(tabDao.findByEntityType(context, entityType)).thenReturn(emptyList());

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

        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn(entityType);

        when(tabDao.findByEntityType(context, entityType)).thenReturn(null);

        List<CrisLayoutTab> tabs = crisLayoutTabService.findByItem(context, itemUuid);

        assertThat(tabs, is(emptyList()));
    }

    @Test
    public void emptyItemMetadataReturnsEmptyList() throws SQLException {
        String itemUuid = UUID.randomUUID().toString();
        Item item = mock(Item.class);
        String entityType = UUID.randomUUID().toString();

        List<MetadataValue> itemMetadata = Collections.emptyList();

        CrisLayoutTab tabOne = grantedAccessTab("tab1", boxWithContent(itemMetadata), boxWithoutContent(itemMetadata));
        CrisLayoutTab tabTwo = grantedAccessTab("tab2", boxWithContent(itemMetadata), boxWithContent(itemMetadata));
        CrisLayoutTab tabThree = grantedAccessTab("tab3", boxWithoutContent(itemMetadata),
            boxWithoutContent(itemMetadata));

        when(itemService.find(context, UUID.fromString(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn(entityType);

        when(item.getMetadata()).thenReturn(itemMetadata);

        when(tabDao.findByEntityType(context, entityType))
            .thenReturn(Arrays.asList(tabOne, tabTwo, tabThree));

        List<CrisLayoutTab> tabs = crisLayoutTabService.findByItem(context, itemUuid);

        assertThat(tabs, is(emptyList()));
    }

    @Test
    public void nullItemMetadataReturnsEmptyList() throws SQLException {
        String itemUuid = UUID.randomUUID().toString();
        Item item = mock(Item.class);
        String entityType = UUID.randomUUID().toString();

        List<MetadataValue> itemMetadata = null;

        CrisLayoutTab tabOne = grantedAccessTab("tab1", boxWithContent(itemMetadata), boxWithoutContent(itemMetadata));
        CrisLayoutTab tabTwo = grantedAccessTab("tab2", boxWithContent(itemMetadata), boxWithContent(itemMetadata));
        CrisLayoutTab tabThree = grantedAccessTab("tab3", boxWithoutContent(itemMetadata),
            boxWithoutContent(itemMetadata));

        when(itemService.find(context, UUID.fromString(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn(entityType);

        when(item.getMetadata()).thenReturn(itemMetadata);

        when(tabDao.findByEntityType(context, entityType))
            .thenReturn(Arrays.asList(tabOne, tabTwo, tabThree));

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
        tab.setTab2Box(Arrays.stream(boxes).map(this::tabToBox).collect(toList()));

        when(crisLayoutTabAccessService.hasAccess(eq(context), any(), eq(tab), any()))
            .thenReturn(grantedAccess);

        return tab;
    }

    private CrisLayoutTab2Box tabToBox(CrisLayoutBox box) {
        CrisLayoutTab2Box boxOne = new CrisLayoutTab2Box();
        boxOne.setBox(box);
        return boxOne;
    }

    private MetadataValue metadataValue() {
        return mock(MetadataValue.class);
    }

    private CrisLayoutBox boxWithContent(List<MetadataValue> itemMetadata) throws SQLException {
        return box(itemMetadata, true, true);
    }

    private CrisLayoutBox boxWithoutContent(List<MetadataValue> itemMetadata) throws SQLException {
        return box(itemMetadata, false, true);
    }

    private CrisLayoutBox restrictedBox(List<MetadataValue> itemMetadata) throws SQLException {
        return box(itemMetadata, true, false);
    }

    private CrisLayoutBox box(List<MetadataValue> itemMetadata, boolean hasContent, boolean grantedAccess)
        throws SQLException {
        CrisLayoutBox box = new CrisLayoutBox();
        box.setId(new Random().nextInt(10000));
        when(boxService.hasContent(context, box, itemMetadata))
            .thenReturn(hasContent);
        when(crisLayoutBoxAccessService.hasAccess(any(), any(), eq(box), any()))
            .thenReturn(grantedAccess);
        return box;
    }
}
