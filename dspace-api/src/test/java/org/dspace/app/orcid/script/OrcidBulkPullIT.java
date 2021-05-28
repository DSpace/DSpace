/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.script;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.app.matcher.LambdaMatcher.has;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.orcid.exception.OrcidClientException;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.app.suggestion.Suggestion;
import org.dspace.app.suggestion.orcid.OrcidPublicationLoader;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link OrcidBulkPull}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidBulkPullIT extends AbstractIntegrationTestWithDatabase {

    private OrcidPublicationLoader orcidPublicationLoader;

    private ConfigurationService configurationService;

    private ExternalDataProvider originalExternalDataProvider;

    private ExternalDataProvider externalDataProviderMock = mock(ExternalDataProvider.class);

    private Collection profileCollection;

    @Before
    public void before() {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent community")
            .build();

        profileCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Persons")
            .withEntityType("Person")
            .build();

        context.restoreAuthSystemState();

        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        orcidPublicationLoader = new DSpace().getServiceManager()
            .getServicesByType(OrcidPublicationLoader.class).get(0);

        originalExternalDataProvider = orcidPublicationLoader.getProvider();

        orcidPublicationLoader.setProvider(externalDataProviderMock);

        when(externalDataProviderMock.getSourceIdentifier())
            .thenReturn(originalExternalDataProvider.getSourceIdentifier());

    }

    @After
    public void after() {
        orcidPublicationLoader.setProvider(originalExternalDataProvider);
    }

    @Test
    public void testPull() throws Exception {

        context.turnOffAuthorisationSystem();

        String orcid1 = "1234-1234-1234-1234";
        String orcid2 = "8888-8888-2222-2222";
        String orcid3 = "0000-0000-1234-5678";

        Item profile1 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("First profile")
            .withOrcidIdentifier(orcid1)
            .build();

        Item profile2 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Second profile")
            .build();

        Item profile3 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Third profile")
            .withOrcidIdentifier(orcid2)
            .build();

        Item profile4 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Fourth profile")
            .withOrcidIdentifier(orcid3)
            .withOrcidAccessToken("ccf31b21-d8f8-403a-8bb9-8e1835b8ebcc")
            .build();

        context.restoreAuthSystemState();

        when(externalDataProviderMock.searchExternalDataObjects(orcid1, 0, -1))
            .thenReturn(Collections.emptyList());

        ExternalDataObject firstExternalDataObject = externalDataObject(orcid2,
            "12345", "EDO 1", "2020-02-01", "Description 1", List.of("Walter White"));
        ExternalDataObject secondExternalDataObject = externalDataObject(orcid2,
            "11111", "EDO 2", "2021-02-01", "Description 2", List.of("Walter White"));

        when(externalDataProviderMock.searchExternalDataObjects(orcid2, 0, -1))
            .thenReturn(List.of(firstExternalDataObject, secondExternalDataObject));

        ExternalDataObject thirdExternalDataObject = externalDataObject(orcid3,
            "23456", "EDO 3", "2022-02-01", "Description 3", List.of("Walter White", "Jesse Pinkman"));

        when(externalDataProviderMock.searchExternalDataObjects(orcid3, 0, -1))
            .thenReturn(List.of(thirdExternalDataObject));

        TestDSpaceRunnableHandler handler = runWebhook(false);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());
        assertThat(handler.getInfoMessages(), hasSize(4));
        assertThat(handler.getInfoMessages(), hasItem("Processed profile with orcid id " + orcid1 + " with success"));
        assertThat(handler.getInfoMessages(), hasItem("Processed profile with orcid id " + orcid2 + " with success"));
        assertThat(handler.getInfoMessages(), hasItem("Processed profile with orcid id " + orcid3 + " with success"));
        assertThat(handler.getInfoMessages(), hasItem("Processed 3 profiles"));

        assertThat(findAllUnprocessedSuggestions(profile1), empty());
        assertThat(findAllUnprocessedSuggestions(profile2), empty());

        List<Suggestion> suggestions = findAllUnprocessedSuggestions(profile3);
        assertThat(suggestions, hasSize(2));

        assertThat(suggestions, has(suggestion(profile3, orcid2 + "::12345", "EDO 1",
            "2020-02-01", "Description 1", List.of("Walter White"))));
        assertThat(suggestions, has(suggestion(profile3, orcid2 + "::11111", "EDO 2",
            "2021-02-01", "Description 2", List.of("Walter White"))));

        suggestions = findAllUnprocessedSuggestions(profile4);
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions, has(suggestion(profile4, orcid3 + "::23456", "EDO 3",
            "2022-02-01", "Description 3", List.of("Walter White", "Jesse Pinkman"))));

        verify(externalDataProviderMock).searchExternalDataObjects(orcid1, 0, -1);
        verify(externalDataProviderMock).searchExternalDataObjects(orcid2, 0, -1);
        verify(externalDataProviderMock).searchExternalDataObjects(orcid3, 0, -1);
        verify(externalDataProviderMock, times(3)).getSourceIdentifier();
        verifyNoMoreInteractions(externalDataProviderMock);
    }

    @Test
    public void testPullWithError() throws Exception {

        context.turnOffAuthorisationSystem();

        String orcid1 = "1234-1234-1234-1234";
        String orcid2 = "8888-8888-2222-2222";
        String orcid3 = "0000-0000-1234-5678";

        Item profile1 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("First profile")
            .withOrcidIdentifier(orcid1)
            .build();

        Item profile2 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Second profile")
            .build();

        Item profile3 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Third profile")
            .withOrcidIdentifier(orcid2)
            .build();

        Item profile4 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Fourth profile")
            .withOrcidIdentifier(orcid3)
            .withOrcidAccessToken("ccf31b21-d8f8-403a-8bb9-8e1835b8ebcc")
            .build();

        context.restoreAuthSystemState();

        ExternalDataObject firstExternalDataObject = externalDataObject(orcid1,
            "12345", "EDO 1", "2022-02-01", "Description 1", List.of("Walter White"));

        when(externalDataProviderMock.searchExternalDataObjects(orcid1, 0, -1))
            .thenReturn(List.of(firstExternalDataObject));

        when(externalDataProviderMock.searchExternalDataObjects(orcid2, 0, -1))
            .thenThrow(new OrcidClientException(404, "Orcid not found"));

        ExternalDataObject secondExternalDataObject = externalDataObject(orcid3,
            "23456", "EDO 2", "2021-02-01", "Description 2", List.of("Walter White", "Jesse Pinkman"));

        when(externalDataProviderMock.searchExternalDataObjects(orcid3, 0, -1))
            .thenReturn(List.of(secondExternalDataObject));

        TestDSpaceRunnableHandler handler = runWebhook(false);
        assertThat(handler.getErrorMessages(), hasSize(1));
        assertThat(handler.getErrorMessages().get(0),
            containsString("An error occurs processing profile with orcid id " + orcid2));

        assertThat(handler.getWarningMessages(), empty());

        assertThat(handler.getInfoMessages(), hasSize(3));
        assertThat(handler.getInfoMessages(), hasItem("Processed profile with orcid id " + orcid1 + " with success"));
        assertThat(handler.getInfoMessages(), hasItem("Processed profile with orcid id " + orcid3 + " with success"));
        assertThat(handler.getInfoMessages(), hasItem("Processed 3 profiles"));

        List<Suggestion> suggestions = findAllUnprocessedSuggestions(profile1);
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions, has(suggestion(profile1, orcid1 + "::12345", "EDO 1",
            "2022-02-01", "Description 1", List.of("Walter White"))));

        assertThat(findAllUnprocessedSuggestions(profile2), empty());
        assertThat(findAllUnprocessedSuggestions(profile3), empty());

        suggestions = findAllUnprocessedSuggestions(profile4);
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions, has(suggestion(profile4, orcid3 + "::23456", "EDO 2",
            "2021-02-01", "Description 2", List.of("Walter White", "Jesse Pinkman"))));

        verify(externalDataProviderMock).searchExternalDataObjects(orcid1, 0, -1);
        verify(externalDataProviderMock).searchExternalDataObjects(orcid2, 0, -1);
        verify(externalDataProviderMock).searchExternalDataObjects(orcid3, 0, -1);
        verify(externalDataProviderMock, times(2)).getSourceIdentifier();
        verifyNoMoreInteractions(externalDataProviderMock);
    }

    @Test
    public void testPullWithOnlyLinkedOption() throws Exception {

        context.turnOffAuthorisationSystem();

        String orcid1 = "1234-1234-1234-1234";
        String orcid2 = "8888-8888-2222-2222";
        String orcid3 = "0000-0000-1234-5678";

        Item profile1 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("First profile")
            .withOrcidIdentifier(orcid1)
            .build();

        Item profile2 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Second profile")
            .build();

        Item profile3 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Third profile")
            .withOrcidIdentifier(orcid2)
            .build();

        Item profile4 = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Fourth profile")
            .withOrcidIdentifier(orcid3)
            .withOrcidAccessToken("ccf31b21-d8f8-403a-8bb9-8e1835b8ebcc")
            .build();

        context.restoreAuthSystemState();

        ExternalDataObject externalDataObject = externalDataObject(orcid3,
            "23456", "EDO 3", "2022-02-01", "Description 3", List.of("Walter White", "Jesse Pinkman"));

        when(externalDataProviderMock.searchExternalDataObjects(orcid3, 0, -1)).thenReturn(List.of(externalDataObject));

        TestDSpaceRunnableHandler handler = runWebhook(true);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());
        assertThat(handler.getInfoMessages(), hasSize(2));
        assertThat(handler.getInfoMessages(), hasItem("Processed profile with orcid id " + orcid3 + " with success"));
        assertThat(handler.getInfoMessages(), hasItem("Processed 1 profiles"));

        assertThat(findAllUnprocessedSuggestions(profile1), empty());
        assertThat(findAllUnprocessedSuggestions(profile2), empty());
        assertThat(findAllUnprocessedSuggestions(profile3), empty());

        List<Suggestion> suggestions = findAllUnprocessedSuggestions(profile4);
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions, has(suggestion(profile4, orcid3 + "::23456", "EDO 3",
            "2022-02-01", "Description 3", List.of("Walter White", "Jesse Pinkman"))));

        verify(externalDataProviderMock).searchExternalDataObjects(orcid3, 0, -1);
        verify(externalDataProviderMock).getSourceIdentifier();
        verifyNoMoreInteractions(externalDataProviderMock);
    }

    private TestDSpaceRunnableHandler runWebhook(boolean onlyLinkedProfiles) throws Exception {
        String[] args = new String[] { "orcid-bulk-pull" };
        args = onlyLinkedProfiles ? ArrayUtils.add(args, "-l") : args;
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        return handler;
    }

    private ExternalDataObject externalDataObject(String orcid, String putCode, String title, String date,
        String description, List<String> authors) {

        String sourceIdentifier = originalExternalDataProvider.getSourceIdentifier();
        ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
        externalDataObject.setId(orcid + "::" + putCode);
        externalDataObject.setDisplayValue(title);
        externalDataObject.setValue(title);
        externalDataObject.addMetadata(new MetadataValueDTO("dc", "title", null, null, title));
        externalDataObject.addMetadata(new MetadataValueDTO("dc", "date", "issued", null, date));
        externalDataObject.addMetadata(new MetadataValueDTO("dc", "description", "abstract", null, description));
        authors.forEach(author -> externalDataObject
            .addMetadata(new MetadataValueDTO("dc", "contributor", "author", null, author)));

        return externalDataObject;
    }

    private Predicate<Suggestion> suggestion(Item target, String id, String title, String date,
        String description, List<String> authors) {

        String source = originalExternalDataProvider.getSourceIdentifier();

        return suggestion -> suggestion.getScore().equals(100d)
            && suggestion.getID().equals(source + ":" + target.getID().toString() + ":" + id)
            && suggestion.getEvidences().size() == 1
            && suggestion.getTarget().equals(target)
            && suggestion.getSource().equals(source)
            && suggestion.getDisplay().equals(title)
            && suggestion.getExternalSourceUri().equals(expectedExternalSourceUri(source, id))
            && contains(suggestion, "dc", "title", null, title)
            && contains(suggestion, "dc", "date", "issued", date)
            && contains(suggestion, "dc", "description", "abstract", description)
            && authors.stream().allMatch(author -> contains(suggestion, "dc", "contributor", "author", author));

    }

    private boolean contains(Suggestion suggestion, String schema, String element, String qualifier, String value) {
        return suggestion.getMetadata().stream()
            .filter(metadataValue -> StringUtils.equals(schema, metadataValue.getSchema()))
            .filter(metadataValue -> StringUtils.equals(element, metadataValue.getElement()))
            .filter(metadataValue -> StringUtils.equals(qualifier, metadataValue.getQualifier()))
            .anyMatch(metadataValue -> StringUtils.equals(value, metadataValue.getValue()));
    }

    private String expectedExternalSourceUri(String sourceIdentifier, String recordId) {
        String serverUrl = configurationService.getProperty("dspace.server.url");
        return serverUrl + "/api/integration/externalsources/" + sourceIdentifier + "/entryValues/" + recordId;
    }

    private List<Suggestion> findAllUnprocessedSuggestions(Item profile) {
        return orcidPublicationLoader.findAllUnprocessedSuggestions(context, profile.getID(), 10, 0, true);
    }
}
