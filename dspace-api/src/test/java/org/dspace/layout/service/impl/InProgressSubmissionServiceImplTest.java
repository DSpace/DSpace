package org.dspace.layout.service.impl;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.junit.Before;
import org.junit.Test;

/**
 * Class with unit tests for {@link InProgressSubmissionServiceImpl}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class InProgressSubmissionServiceImplTest {

    private InProgressSubmissionServiceImpl workspaceItemSubmissionService;
    private DCInputsReader dcInputsReader = mock(DCInputsReader.class);

    @Before
    public void setUp() throws Exception {
        workspaceItemSubmissionService = new InProgressSubmissionServiceImpl(dcInputsReader);
    }

    /**
     * checks that an entry is put into service's internal list and removed properly
     */
    @Test
    public void workspaceItemInCacheIsRemoved() {
        final WorkspaceItem workspaceItem = mock(WorkspaceItem.class);
        final Item item = mock(Item.class);
        UUID uuid = UUID.randomUUID();

        when(workspaceItem.getItem()).thenReturn(item);
        when(item.getID()).thenReturn(uuid);

        workspaceItemSubmissionService.add(workspaceItem);

        workspaceItemSubmissionService.remove(uuid);

        assertThat(workspaceItemSubmissionService.contains(uuid), is(false));
    }

    @Test
    public void submissionDefinitionCheck() throws DCInputsReaderException {
        final WorkspaceItem workspaceItem = mock(WorkspaceItem.class);
        final Item item = mock(Item.class);
        final Collection collection = mock(Collection.class);
        UUID uuid = UUID.randomUUID();
        String metadataFieldInSubmission = "foo.bar.baz";
        String metadataFieldNotInSubmission = "foo.bar.not";

        when(workspaceItem.getItem()).thenReturn(item);
        when(workspaceItem.getCollection()).thenReturn(collection);
        when(item.getID()).thenReturn(uuid);


        final DCInputSet dcInputSet = mock(DCInputSet.class);

        when(dcInputSet.isFieldPresent(metadataFieldInSubmission)).thenReturn(true);
        when(dcInputSet.isFieldPresent(metadataFieldNotInSubmission)).thenReturn(false);
        when(dcInputsReader.getInputsByCollection(collection)).thenReturn(Collections.singletonList(dcInputSet));

        workspaceItemSubmissionService.add(workspaceItem);
        boolean dataInSubmissionDefinition = workspaceItemSubmissionService
                                                 .hasSubmissionRights(uuid,
                                                                      metadataValue(metadataFieldInSubmission));

        boolean dataNotInSubmissionDefinition = workspaceItemSubmissionService
                                                    .hasSubmissionRights(uuid,
                                                                         metadataValue(metadataFieldNotInSubmission));
        assertThat(dataInSubmissionDefinition, is(true));
        assertThat(dataNotInSubmissionDefinition, is(false));
    }

    private MetadataValue metadataValue(final String metadataFieldString) {
        final MetadataField metadataField = mock(MetadataField.class);
        when(metadataField.toString('.')).thenReturn(metadataFieldString);
        final MetadataValue metadataValue = mock(MetadataValue.class);
        when(metadataValue.getMetadataField()).thenReturn(metadataField);

        return metadataValue;
    }
}
