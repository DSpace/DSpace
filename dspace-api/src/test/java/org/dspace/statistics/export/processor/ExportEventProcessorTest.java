/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.processor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.AbstractDSpaceTest;
import org.dspace.content.Entity;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * Test for the ExportEventProcessor class
 */
public class ExportEventProcessorTest extends AbstractDSpaceTest {

    @Mock
    private Context context = mock(Context.class);
    @Mock
    private HttpServletRequest request = mock(HttpServletRequest.class);
    @Mock
    private Item item = mock(Item.class);
    @Mock
    private ConfigurationService configurationService = mock(ConfigurationService.class);
    @Mock
    private ItemService itemService = mock(ItemService.class);
    @Mock
    private EntityService entityService = mock(EntityService.class);


    @InjectMocks
    ExportEventProcessor exportEventProcessor = mock(ExportEventProcessor.class);


    @Test
    /**
     * Test the getBaseParameters method
     */
    public void testGetBaseParameters() throws UnsupportedEncodingException {
        exportEventProcessor.context = context;
        exportEventProcessor.request = request;
        exportEventProcessor.configurationService = configurationService;
        exportEventProcessor.trackerUrlVersion = "Z39.88-2004";

        when(exportEventProcessor.getCurrentDateString()).thenReturn("2020-01-24T13:24:33Z");

        when(request.getRemoteAddr()).thenReturn("test-client-ip");
        when(request.getHeader("USER-AGENT")).thenReturn("test-user-agent");
        when(request.getHeader("referer")).thenReturn("test-referer");
        when(configurationService.getBooleanProperty("useProxies", false)).thenReturn(false);
        when(configurationService.getProperty("dspace.hostname")).thenReturn("localhost");

        when(item.getHandle()).thenReturn("123456/1");

        when(exportEventProcessor.getBaseParameters(item)).thenCallRealMethod();
        String result = exportEventProcessor.getBaseParameters(item);
        String expected = "url_ver=Z39.88-2004&req_id=test-client-ip&req_dat=test-user-agent&rft.artnum=" +
                "oai%3Alocalhost%3A123456%2F1&rfr_dat=test-referer&rfr_id=localhost&url_tim=2020-01-24T13%3A24%3A33Z";

        assertThat(result, is(expected));


    }

    @Test
    /**
     * Test the ShouldProcessItem method where the item is null
     */
    public void testShouldProcessItemWhenNull() throws SQLException {
        exportEventProcessor.itemService = itemService;
        exportEventProcessor.context = context;

        when(item.isArchived()).thenReturn(true);
        when(itemService.canEdit(context, item)).thenReturn(false);
        when(exportEventProcessor.shouldProcessItemType(item)).thenReturn(true);
        when(exportEventProcessor.shouldProcessEntityType(item)).thenReturn(true);

        when(exportEventProcessor.shouldProcessItem(null)).thenCallRealMethod();
        boolean result = exportEventProcessor.shouldProcessItem(null);
        assertThat(result, is(false));
    }

    @Test
    /**
     * Test the ShouldProcessItem method where the item is not archived
     */
    public void testShouldProcessItemWhenNotArchived() throws SQLException {
        exportEventProcessor.itemService = itemService;
        exportEventProcessor.context = context;

        when(itemService.canEdit(context, item)).thenReturn(false);
        when(exportEventProcessor.shouldProcessItemType(item)).thenReturn(true);
        when(exportEventProcessor.shouldProcessEntityType(item)).thenReturn(true);

        when(item.isArchived()).thenReturn(false);

        when(exportEventProcessor.shouldProcessItem(item)).thenCallRealMethod();
        boolean result = exportEventProcessor.shouldProcessItem(item);
        assertThat(result, is(false));
    }

    @Test
    /**
     * Test the ShouldProcessItem method where the item can be edit by the current user
     */
    public void testShouldProcessItemWhenCanEdit() throws SQLException {
        exportEventProcessor.itemService = itemService;
        exportEventProcessor.context = context;

        when(item.isArchived()).thenReturn(true);
        when(exportEventProcessor.shouldProcessItemType(item)).thenReturn(true);
        when(exportEventProcessor.shouldProcessEntityType(item)).thenReturn(true);

        when(itemService.canEdit(context, item)).thenReturn(true);

        when(exportEventProcessor.shouldProcessItem(item)).thenCallRealMethod();
        boolean result = exportEventProcessor.shouldProcessItem(item);
        assertThat(result, is(false));

    }

    @Test
    /**
     * Test the ShouldProcessItem method where the item type should be excluded
     */
    public void testShouldProcessItemWhenShouldNotProcessType() throws SQLException {
        exportEventProcessor.itemService = itemService;
        exportEventProcessor.context = context;

        when(item.isArchived()).thenReturn(true);
        when(itemService.canEdit(context, item)).thenReturn(false);
        when(exportEventProcessor.shouldProcessEntityType(item)).thenReturn(true);

        when(exportEventProcessor.shouldProcessItemType(item)).thenReturn(false);

        when(exportEventProcessor.shouldProcessItem(item)).thenCallRealMethod();
        boolean result = exportEventProcessor.shouldProcessItem(item);
        assertThat(result, is(false));

    }

    @Test
    /**
     * Test the ShouldProcessItem method where the item entity type should not be processed
     */
    public void testShouldProcessItemWhenShouldNotProcessEntity() throws SQLException {
        exportEventProcessor.itemService = itemService;
        exportEventProcessor.context = context;

        when(item.isArchived()).thenReturn(true);
        when(itemService.canEdit(context, item)).thenReturn(false);
        when(exportEventProcessor.shouldProcessItemType(item)).thenReturn(true);

        when(exportEventProcessor.shouldProcessEntityType(item)).thenReturn(false);

        when(exportEventProcessor.shouldProcessItem(item)).thenCallRealMethod();
        boolean result = exportEventProcessor.shouldProcessItem(item);
        assertThat(result, is(false));

    }

    @Test
    /**
     * Test the ShouldProcessItem method where all conditions are met
     */
    public void testShouldProcessItem() throws SQLException {
        exportEventProcessor.itemService = itemService;
        exportEventProcessor.context = context;

        when(item.isArchived()).thenReturn(true);
        when(itemService.canEdit(context, item)).thenReturn(false);
        when(exportEventProcessor.shouldProcessItemType(item)).thenReturn(true);
        when(exportEventProcessor.shouldProcessEntityType(item)).thenReturn(true);


        when(exportEventProcessor.shouldProcessItem(item)).thenCallRealMethod();
        boolean result = exportEventProcessor.shouldProcessItem(item);
        assertThat(result, is(true));

    }


    @Test
    /**
     * Test the ShouldProcessEntityType method where all conditions are met
     */
    public void testShouldProcessEntityType() throws SQLException {
        exportEventProcessor.entityService = entityService;


        String entityType1 = "entityType1";
        String entityType2 = "entityType2";

        Entity entity = mock(Entity.class);
        EntityType itemEntityType = mock(EntityType.class);

        List<String> entityTypeList = new ArrayList<>();
        entityTypeList.add(entityType1);
        entityTypeList.add(entityType2);

        exportEventProcessor.entityTypes = entityTypeList;

        when(item.getID()).thenReturn(UUID.fromString("e22a97f0-f320-4277-aff6-fdb254a751ce"));
        when(entityService.findByItemId(any(Context.class), any(UUID.class)))
                .thenReturn(entity);
        when(entityService.getType(any(Context.class), any(Entity.class))).thenReturn(itemEntityType);
        when(itemEntityType.getLabel()).thenReturn(entityType1);

        when(exportEventProcessor.shouldProcessEntityType(item)).thenCallRealMethod();
        boolean result = exportEventProcessor.shouldProcessEntityType(item);

        assertThat(result, is(true));
    }

    @Test
    /**
     * Test the ShouldProcessEntityType method where the item entity type is not present in the configured list
     */
    public void testShouldProcessEntityTypeWhenNotInList() throws SQLException {
        exportEventProcessor.entityService = entityService;


        String entityType1 = "entityType1";
        String entityType2 = "entityType2";
        String entityType3 = "entityType3";

        Entity entity = mock(Entity.class);
        EntityType itemEntityType = mock(EntityType.class);

        List<String> entityTypeList = new ArrayList<>();
        entityTypeList.add(entityType1);
        entityTypeList.add(entityType2);

        exportEventProcessor.entityTypes = entityTypeList;

        when(item.getID()).thenReturn(UUID.fromString("e22a97f0-f320-4277-aff6-fdb254a751ce"));
        when(entityService.findByItemId(any(Context.class), any(UUID.class)))
                .thenReturn(entity);
        when(entityService.getType(any(Context.class), any(Entity.class))).thenReturn(itemEntityType);
        when(itemEntityType.getLabel()).thenReturn(entityType3);

        when(exportEventProcessor.shouldProcessEntityType(item)).thenCallRealMethod();
        boolean result = exportEventProcessor.shouldProcessEntityType(item);

        assertThat(result, is(false));

    }


    @Test
    /**
     * Test the shouldProcessItemType method where the item type is present in the list of excluded types
     */
    public void testShouldProcessItemTypeInExcludeTrackerTypeList() {
        exportEventProcessor.itemService = itemService;

        String itemField = "dc.type";

        exportEventProcessor.trackerType = itemField;

        List<String> typeList = new ArrayList<>();
        typeList.add("type1");
        typeList.add("type2");
        exportEventProcessor.trackerValues = typeList;

        MetadataValue metadataValue = mock(MetadataValue.class);
        when(metadataValue.getValue()).thenReturn("type2");
        List<MetadataValue> values = new ArrayList<>();
        values.add(metadataValue);

        when(itemService.getMetadata(any(Item.class), any(String.class), any(String.class), any(String.class),
                                     any(String.class))).thenReturn(values);


        when(exportEventProcessor.shouldProcessItemType(item)).thenCallRealMethod();
        boolean result = exportEventProcessor.shouldProcessItemType(item);
        assertThat(result, is(false));

    }

    @Test
    /**
     * Test the shouldProcessItemType method where the item type is not present in the list of excluded types
     */
    public void testShouldProcessItemTypeNotInExcludeTrackerTypeList() {
        exportEventProcessor.itemService = itemService;

        String itemField = "dc.type";

        exportEventProcessor.trackerType = itemField;

        List<String> typeList = new ArrayList<>();
        typeList.add("type1");
        typeList.add("type2");
        exportEventProcessor.trackerValues = typeList;

        MetadataValue metadataValue = mock(MetadataValue.class);
        when(metadataValue.getValue()).thenReturn("type3");
        List<MetadataValue> values = new ArrayList<>();
        values.add(metadataValue);

        when(itemService.getMetadata(any(Item.class), any(String.class), any(String.class), any(String.class),
                                     any(String.class))).thenReturn(values);


        when(exportEventProcessor.shouldProcessItemType(item)).thenCallRealMethod();
        boolean result = exportEventProcessor.shouldProcessItemType(item);
        assertThat(result, is(true));

    }

}
