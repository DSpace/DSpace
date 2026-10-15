/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.filler;

import static org.dspace.content.Item.ANY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test suite with Mockito for the {@link ExternalDataProviderImportFiller}, focused on the ROR
 * OrgUnit use case: an item auto-created from a {@code will be generated::ROR-ID::<id>} authority
 * must be populated from the full ROR record (e.g. {@code organization.legalName}), not only with
 * {@code dc.title}.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ExternalDataProviderImportFillerTest {

    private static final String ROR_ID = "https://ror.org/03vb2cr34";
    private static final String AUTHORITY = "will be generated::ROR-ID::" + ROR_ID;
    private static final String LEGAL_NAME = "4Science";

    @Mock
    private ItemService itemService;

    @Mock
    private ExternalDataProvider externalDataProvider;

    @Mock
    private Context context;

    private ExternalDataProviderImportFiller filler;

    @Before
    public void setUp() {
        filler = new ExternalDataProviderImportFiller(externalDataProvider, "ROR-ID");
        ReflectionTestUtils.setField(filler, "itemService", itemService);
    }

    /**
     * The created OrgUnit must be enriched with the metadata coming from the ROR record
     * (organization.legalName, organization.identifier.ror, dc.type) and, since no title is set,
     * the value is also stored as dc.title as a fallback.
     */
    @Test
    public void testFillItemEnrichesOrgUnitWithRorRecord() throws SQLException {
        Item orgUnit = mock(Item.class);
        MetadataValue sourceMetadata = mockSourceMetadata(AUTHORITY, LEGAL_NAME);

        when(externalDataProvider.getExternalDataObject(ROR_ID))
            .thenReturn(Optional.of(rorExternalDataObject()));
        // no title yet on the created item -> the fallback title must be applied
        when(itemService.getMetadataFirstValue(orgUnit, "dc", "title", null, ANY)).thenReturn(null);

        filler.fillItem(context, sourceMetadata, orgUnit);

        verify(externalDataProvider).getExternalDataObject(ROR_ID);
        verify(itemService).addMetadata(context, orgUnit, "organization", "legalName", null, null, LEGAL_NAME);
        verify(itemService).addMetadata(context, orgUnit, "organization", "identifier", "ror", null, ROR_ID);
        verify(itemService).addMetadata(context, orgUnit, "dc", "type", null, null, "Company");
        verify(itemService).setMetadataSingleValue(context, orgUnit, "dc", "title", null, null, LEGAL_NAME);
    }

    /**
     * Metadata already present on the item must not be duplicated.
     */
    @Test
    public void testFillItemDoesNotDuplicateExistingMetadata() throws SQLException {
        Item orgUnit = mock(Item.class);
        MetadataValue sourceMetadata = mockSourceMetadata(AUTHORITY, LEGAL_NAME);

        when(externalDataProvider.getExternalDataObject(ROR_ID))
            .thenReturn(Optional.of(rorExternalDataObject()));

        MetadataValue existingLegalName = mock(MetadataValue.class);
        when(existingLegalName.getValue()).thenReturn(LEGAL_NAME);
        when(itemService.getMetadata(orgUnit, "organization", "legalName", null, ANY))
            .thenReturn(List.of(existingLegalName));

        filler.fillItem(context, sourceMetadata, orgUnit);

        verify(itemService, never())
            .addMetadata(context, orgUnit, "organization", "legalName", null, null, LEGAL_NAME);
        // the other, not-yet-present values are still added
        verify(itemService).addMetadata(context, orgUnit, "organization", "identifier", "ror", null, ROR_ID);
        verify(itemService).addMetadata(context, orgUnit, "dc", "type", null, null, "Company");
    }

    /**
     * If a title is already present on the item, the dc.title fallback must not overwrite it.
     */
    @Test
    public void testFillItemDoesNotOverrideExistingTitle() throws SQLException {
        Item orgUnit = mock(Item.class);
        MetadataValue sourceMetadata = mockSourceMetadata(AUTHORITY, LEGAL_NAME);

        when(externalDataProvider.getExternalDataObject(ROR_ID))
            .thenReturn(Optional.of(rorExternalDataObject()));
        when(itemService.getMetadataFirstValue(orgUnit, "dc", "title", null, ANY)).thenReturn("Existing title");

        filler.fillItem(context, sourceMetadata, orgUnit);

        verify(itemService).addMetadata(context, orgUnit, "organization", "legalName", null, null, LEGAL_NAME);
        verify(itemService, never())
            .setMetadataSingleValue(any(), any(), any(), any(), any(), any(), any());
    }

    /**
     * When the authority is not a "will be generated::ROR-ID::" value, the external provider must
     * not be queried (nothing to enrich from).
     */
    @Test
    public void testFillItemWithoutGeneratedAuthorityDoesNotCallProvider() throws SQLException {
        Item orgUnit = mock(Item.class);
        MetadataValue sourceMetadata = mockSourceMetadata("will be referenced::ROR-ID::" + ROR_ID, LEGAL_NAME);

        filler.fillItem(context, sourceMetadata, orgUnit);

        verify(externalDataProvider, never()).getExternalDataObject(any());
    }

    private MetadataValue mockSourceMetadata(String authority, String value) {
        MetadataValue metadata = mock(MetadataValue.class);
        when(metadata.getAuthority()).thenReturn(authority);
        when(metadata.getValue()).thenReturn(value);
        return metadata;
    }

    private ExternalDataObject rorExternalDataObject() {
        ExternalDataObject externalData = new ExternalDataObject("ror");
        externalData.setId(ROR_ID);
        externalData.setValue(LEGAL_NAME);
        externalData.setDisplayValue(LEGAL_NAME);
        externalData.addMetadata(new MetadataValueDTO("organization", "legalName", null, LEGAL_NAME));
        externalData.addMetadata(new MetadataValueDTO("organization", "identifier", "ror", ROR_ID));
        externalData.addMetadata(new MetadataValueDTO("dc", "type", null, "Company"));
        return externalData;
    }

}
