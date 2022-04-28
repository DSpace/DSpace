/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dspace.AbstractUnitTest;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.junit.Test;

public class MetadataFieldServiceTest extends AbstractUnitTest {

    private MetadataFieldService metadataFieldService =
            ContentServiceFactory.getInstance().getMetadataFieldService();
    private MetadataSchemaService metadataSchemaService =
            ContentServiceFactory.getInstance().getMetadataSchemaService();

    @Test
    public void testMetadataFieldCaching() throws Exception {

        MetadataField subjectField = metadataFieldService.findByElement(context, "dc", "subject", null);
        MetadataField issnField = metadataFieldService.findByElement(context, "dc", "identifier", "issn");

        MetadataSchema dspaceSchema = metadataSchemaService.find(context, "dspace");

        subjectField.setMetadataSchema(dspaceSchema);
        issnField.setMetadataSchema(dspaceSchema);

        // Searching for dspace.subject and dspace.identifier.issn should return the already stored metadatafields
        assertEquals(
                subjectField,
                metadataFieldService.findByElement(context, "dspace", "subject", null)
        );
        assertEquals(
                issnField,
                metadataFieldService.findByElement(context, "dspace", "identifier", "issn")
        );

        // The dspace.subject and dspace.identifier.issn metadatafields should now reference the 'dspace' metadataschema
        assertEquals(
                "dspace",
                metadataFieldService
                        .findByElement(context, "dspace", "subject", null)
                        .getMetadataSchema()
                        .getName()
        );
        assertEquals(
                "dspace",
                metadataFieldService
                        .findByElement(context, "dspace", "identifier", "issn")
                        .getMetadataSchema()
                        .getName()
        );

        // Metadatafields dc.subject and dc.identifier.issn should no longer be found
        assertNull(
                metadataFieldService.findByElement(context, "dc", "subject", null)
        );
        assertNull(
                metadataFieldService.findByElement(context, "dc", "identifier", "issn")
        );

        // Same tests, new context

        context.complete();
        context = new Context();

        assertEquals(
                subjectField,
                metadataFieldService.findByElement(context, "dspace", "subject", null)
        );
        assertEquals(
                issnField,
                metadataFieldService.findByElement(context, "dspace", "identifier", "issn")
        );
        assertEquals(
                "dspace",
                metadataFieldService
                        .findByElement(context, "dspace", "subject", null)
                        .getMetadataSchema()
                        .getName()
        );
        assertEquals(
                "dspace",
                metadataFieldService
                        .findByElement(context, "dspace", "identifier", "issn")
                        .getMetadataSchema()
                        .getName()
        );
        assertNull(
                metadataFieldService.findByElement(context, "dc", "subject", null)
        );
        assertNull(
                metadataFieldService.findByElement(context, "dc", "identifier", "issn")
        );
    }
}
