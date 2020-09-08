/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertTrue;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.AbstractUnitTest;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.discovery.indexobject.IndexableMetadataField;
import org.dspace.discovery.indexobject.MetadataFieldIndexFactoryImpl;
import org.junit.Test;

/**
 * Test class for {@link MetadataFieldIndexFactoryImpl}
 *
 * @author Maria Verdonck (Atmire) on 23/07/2020
 */
public class MetadataFieldIndexFactoryImplTest extends AbstractUnitTest {
    private MetadataSchemaService metadataSchemaService =
        ContentServiceFactory.getInstance().getMetadataSchemaService();
    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

    private String schemaName = "schema1";
    private String elemName1 = "elem1";
    private String elemName2 = "elem2";
    private String qualName1 = "qual1";

    private MetadataSchema schema;
    private MetadataField field1;
    private MetadataField field2;

    @Test
    public void test_buildDocument_withQualifier() throws Exception {
        context.turnOffAuthorisationSystem();
        schema = metadataSchemaService.create(context, schemaName, "htpp://test/schema/");
        field1 = metadataFieldService.create(context, schema, elemName1, qualName1, "note 1");

        MetadataFieldIndexFactoryImpl fieldIndexFactory = new MetadataFieldIndexFactoryImpl();
        IndexableMetadataField indexableMetadataField = new IndexableMetadataField(this.field1);
        SolrInputDocument solrInputDocument = fieldIndexFactory.buildDocument(context, indexableMetadataField);

        assertTrue(solrInputDocument.getFieldValues(MetadataFieldIndexFactoryImpl.SCHEMA_FIELD_NAME + "_keyword")
                                    .contains(this.field1.getMetadataSchema().getName()));
        assertTrue(solrInputDocument.getFieldValues(MetadataFieldIndexFactoryImpl.ELEMENT_FIELD_NAME + "_keyword")
                                    .contains(this.field1.getElement()));
        assertTrue(solrInputDocument.getFieldValues(MetadataFieldIndexFactoryImpl.QUALIFIER_FIELD_NAME + "_keyword")
                                    .contains(this.field1.getQualifier()));

        assertTrue(solrInputDocument.getFieldValues(MetadataFieldIndexFactoryImpl.FIELD_NAME_VARIATIONS + "_keyword")
                                    .contains(this.field1.getQualifier()));
        assertTrue(solrInputDocument.getFieldValues(MetadataFieldIndexFactoryImpl.FIELD_NAME_VARIATIONS + "_keyword")
                                    .contains(this.field1.getElement() + "." + this.field1.getQualifier()));
        assertTrue(solrInputDocument.getFieldValues(MetadataFieldIndexFactoryImpl.FIELD_NAME_VARIATIONS + "_keyword")
                                    .contains(this.field1.toString('.')));

        metadataSchemaService.delete(context, schema);
        metadataFieldService.delete(context, field1);
        context.restoreAuthSystemState();
    }

    @Test
    public void test_buildDocument_noQualifier() throws Exception {
        context.turnOffAuthorisationSystem();
        schema = metadataSchemaService.create(context, schemaName, "htpp://test/schema/");
        field2 = metadataFieldService.create(context, schema, elemName2, null, "note 2");
        MetadataFieldIndexFactoryImpl fieldIndexFactory = new MetadataFieldIndexFactoryImpl();
        IndexableMetadataField indexableMetadataField = new IndexableMetadataField(this.field2);
        SolrInputDocument solrInputDocument = fieldIndexFactory.buildDocument(context, indexableMetadataField);
        assertTrue(solrInputDocument.getFieldValues(MetadataFieldIndexFactoryImpl.SCHEMA_FIELD_NAME + "_keyword")
                                    .contains(this.field2.getMetadataSchema().getName()));
        assertTrue(solrInputDocument.getFieldValues(MetadataFieldIndexFactoryImpl.ELEMENT_FIELD_NAME + "_keyword")
                                    .contains(this.field2.getElement()));

        assertTrue(solrInputDocument.getFieldValues(MetadataFieldIndexFactoryImpl.FIELD_NAME_VARIATIONS + "_keyword")
                                    .contains(this.field2.getElement()));
        assertTrue(solrInputDocument.getFieldValues(MetadataFieldIndexFactoryImpl.FIELD_NAME_VARIATIONS + "_keyword")
                                    .contains(this.field2.toString('.')));

        metadataSchemaService.delete(context, schema);
        metadataFieldService.delete(context, field2);
        context.restoreAuthSystemState();
    }
}
