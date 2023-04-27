/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Tests for class MetadataValue
 *
 * @author pvillega
 */
public class MetadataValueTest extends AbstractIntegrationTestWithDatabase {

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataValueTest.class);

    /**
     * MetadataValue instance for the tests
     */
    private MetadataValue mv = null;

    private Collection collection;
    private Community owningCommunity;
    private Item it;


    /**
     * MetadataField instance for the tests
     */
    private MetadataField mf;

    /**
     * Element of the metadata element
     */
    private String element = "contributor";

    /**
     * Qualifier of the metadata element
     */
    private String qualifier = "author";

    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    private MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
            context.turnOffAuthorisationSystem();
            this.owningCommunity = CommunityBuilder.createCommunity(context).build();
            this.collection = CollectionBuilder.createCollection(context, owningCommunity).build();
            this.it = ItemBuilder.createItem(context, collection).build();

            this.mf = metadataFieldService.findByElement(context,
                                                         MetadataSchemaEnum.DC.getName(), element, qualifier);
            this.mv = metadataValueService.create(context, it, mf);
            context.restoreAuthSystemState();
    }

    /**
     * Test of getFieldId method, of class MetadataValue.
     */
    @Test
    public void testGetFieldId() {
        MetadataValue instance = new MetadataValue();
        assertThat("testGetFieldId 0", instance.getID(), equalTo(0));

        assertThat("testGetFieldId 1", mv.getMetadataField().getID(), equalTo(mf.getID()));
    }

    /**
     * Test of getItemId method, of class MetadataValue.
     */
    @Test
    public void testGetDSpaceObject() {
        assertTrue("testGetItemId 0", mv.getDSpaceObject().equals(it));
    }

    /**
     * Test of getLanguage method, of class MetadataValue.
     */
    @Test
    public void testGetLanguage() {
        assertThat("testGetLanguage 0", mv.getLanguage(), nullValue());
    }

    /**
     * Test of setLanguage method, of class MetadataValue.
     */
    @Test
    public void testSetLanguage() {
        String language = "eng";
        mv.setLanguage(language);
        assertThat("testSetLanguage 0", mv.getLanguage(), equalTo(language));
    }

    /**
     * Test of getPlace method, of class MetadataValue.
     */
    @Test
    public void testGetPlace() {
        assertThat("testGetPlace 0", mv.getPlace(), equalTo(1));
    }

    /**
     * Test of setPlace method, of class MetadataValue.
     */
    @Test
    public void testSetPlace() {
        int place = 5;
        mv.setPlace(place);
        assertThat("testSetPlace 0", mv.getPlace(), equalTo(place));
    }

    /**
     * Test of getValueId method, of class MetadataValue.
     */
    @Test
    public void testGetValueId() {
        assertThat("testGetValueId 0", mv.getID(), notNullValue());
    }

    /**
     * Test of getValue method, of class MetadataValue.
     */
    @Test
    public void testGetValue() {
        assertThat("testGetValue 0", mv.getValue(), nullValue());
    }

    /**
     * Test of setValue method, of class MetadataValue.
     */
    @Test
    public void testSetValue() {
        String value = "value";
        mv.setValue(value);
        assertThat("testSetValue 0", mv.getValue(), equalTo(value));
    }

    /**
     * Test of getAuthority method, of class MetadataValue.
     */
    @Test
    public void testGetAuthority() {
        assertThat("testGetAuthority 0", mv.getAuthority(), nullValue());
    }

    /**
     * Test of setAuthority method, of class MetadataValue.
     */
    @Test
    public void testSetAuthority() {
        String value = "auth_val";
        mv.setAuthority(value);
        assertThat("testSetAuthority 0", mv.getAuthority(), equalTo(value));
    }

    /**
     * Test of getConfidence method, of class MetadataValue.
     */
    @Test
    public void testGetConfidence() {
        assertThat("testGetConfidence 0", mv.getConfidence(), equalTo(-1));
    }

    /**
     * Test of setConfidence method, of class MetadataValue.
     */
    @Test
    public void testSetConfidence() {
        int value = 5;
        mv.setConfidence(value);
        assertThat("testSetConfidence 0", mv.getConfidence(), equalTo(value));
    }

    /**
     * Test of create method, of class MetadataValue.
     */
    @Test
    public void testCreate() throws Exception {
        metadataValueService.create(context, it, mf);
    }

    /**
     * Test of find method, of class MetadataValue.
     */
    @Test
    public void testFind() throws Exception {
        metadataValueService.create(context, it, mf);
        int id = mv.getID();
        MetadataValue found = metadataValueService.find(context, id);
        assertThat("testFind 0", found, notNullValue());
        assertThat("testFind 1", found.getID(), equalTo(id));
    }

    /**
     * Test of findByField method, of class MetadataValue.
     */
    @Test
    public void testFindByField() throws Exception {
        metadataValueService.create(context, it, mf);
        List<MetadataValue> found = metadataValueService.findByField(context, mf);
        assertThat("testFind 0", found, notNullValue());
        assertTrue("testFind 1", found.size() >= 1);
    }

    /**
     * Test of update method, of class MetadataValue.
     */
    @Test
    public void testUpdate() throws Exception {
        metadataValueService.create(context, it, mf);
        metadataValueService.update(context, mv);
    }


}
