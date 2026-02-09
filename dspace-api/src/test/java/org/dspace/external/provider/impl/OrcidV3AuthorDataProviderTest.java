/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;

import org.dspace.AbstractDSpaceTest;
import org.dspace.external.OrcidRestConnector;
import org.dspace.external.model.ExternalDataObject;
import org.junit.Before;
import org.junit.Test;


/**
 * Unit tests for {@link OrcidV3AuthorDataProvider}.
 *
 * @author Jesiel Viana (jesielviana at proton.me)
 *
 */
public class OrcidV3AuthorDataProviderTest extends AbstractDSpaceTest {

    private static final String SEARCH_XML_PATH = "org/dspace/external/provider/orcid-v3-author/search.xml";
    private static final String PERSON1_XML_PATH = "org/dspace/external/provider/orcid-v3-author/person1.xml";
    private static final String PERSON2_XML_PATH = "org/dspace/external/provider/orcid-v3-author/person2.xml";
    private static final String PERSON3_XML_PATH = "org/dspace/external/provider/orcid-v3-author/person3.xml";

    public static final String ORCID_SEARCH_QUERY = "search?q=0000-0000-0000-0000";

    private OrcidV3AuthorDataProvider dataProvider;

    @Before
    public void setup() throws Exception {
        dataProvider = new OrcidV3AuthorDataProvider();

        OrcidRestConnector mockRestConnector = mock(OrcidRestConnector.class);

        dataProvider.setOrcidRestConnector(mockRestConnector);
        dataProvider.setSourceIdentifier("orcid");
        dataProvider.setOrcidUrl("https://orcid.org");

        dataProvider.setClientId("client-id");
        dataProvider.setClientSecret("client-secret");
        dataProvider.setOAUTHUrl("https://orcid.org/oauth");

        InputStream searchXmlStream = getClass().getClassLoader().getResourceAsStream(SEARCH_XML_PATH);
        InputStream person1XmlStream = getClass().getClassLoader().getResourceAsStream(PERSON1_XML_PATH);
        InputStream person2XmlStream = getClass().getClassLoader().getResourceAsStream(PERSON2_XML_PATH);
        InputStream person3XmlStream = getClass().getClassLoader().getResourceAsStream(PERSON3_XML_PATH);

        when(mockRestConnector.get("search?q=search%3Fq%3D0000-0000-0000-0000&start=0&rows=10", null))
                .thenReturn(searchXmlStream);
        when(mockRestConnector.get("0000-0000-0000-0001/person", null)).thenReturn(person1XmlStream);
        when(mockRestConnector.get("0000-0000-0000-0002/person", null)).thenReturn(person2XmlStream);
        when(mockRestConnector.get("0000-0000-0000-0003/person", null)).thenReturn(person3XmlStream);

    }

    @Test
    public void testGetExternalDataObjectSizeIsCorrect() {
        List<ExternalDataObject> optional = dataProvider.searchExternalDataObjects(ORCID_SEARCH_QUERY, 0, 10);
        assertThat(optional, hasSize(3));
    }

    @Test
    public void testGetExternalDataObjectGetPersonWithAllFieldsPopulated() {
        List<ExternalDataObject> optional = dataProvider.searchExternalDataObjects(ORCID_SEARCH_QUERY, 0, 10);

        assertThat(optional, hasSize(3));

        ExternalDataObject externalDataObject1 = optional.get(0);

        // Basic field assertions
        assertThat(externalDataObject1.getId(), equalTo("0000-0000-0000-0001"));
        assertThat(externalDataObject1.getValue(), equalTo("FamilyName1, GivenNames1"));
        assertThat(externalDataObject1.getSource(), equalTo("orcid"));
        assertThat(externalDataObject1.getDisplayValue(), equalTo("FamilyName1, GivenNames1"));

        // Metadata assertions
        assertThat(externalDataObject1.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("person")),
                        hasProperty("element", equalTo("familyName")),
                        hasProperty("value", equalTo("FamilyName1"))
                )
        ));
        assertThat(externalDataObject1.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("person")),
                        hasProperty("element", equalTo("givenName")),
                        hasProperty("value", equalTo("GivenNames1"))
                )
        ));
        assertThat(externalDataObject1.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("person")),
                        hasProperty("element", equalTo("email")),
                        hasProperty("value", equalTo("person1@email.com"))
                )
        ));
        assertThat(externalDataObject1.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("person")),
                        hasProperty("element", equalTo("identifier")),
                        hasProperty("qualifier", equalTo("orcid")),
                        hasProperty("value", equalTo("0000-0000-0000-0001"))
                )
        ));
        assertThat(externalDataObject1.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("dc")),
                        hasProperty("element", equalTo("identifier")),
                        hasProperty("qualifier", equalTo("uri")),
                        hasProperty("value", equalTo("https://orcid.org/0000-0000-0000-0001"))
                )
        ));
    }

    @Test
    public void testGetExternalDataObjectGetPrimaryEmailFromPersonWithTwoEmails() {
        List<ExternalDataObject> optional = dataProvider.searchExternalDataObjects(ORCID_SEARCH_QUERY, 0, 10);

        assertThat(optional, hasSize(3));

        ExternalDataObject externalDataObject2 = optional.get(1); // Test person2 (with two emails)

        // Basic field assertions
        assertThat(externalDataObject2.getId(), equalTo("0000-0000-0000-0002"));
        assertThat(externalDataObject2.getValue(), equalTo("FamilyName2, GivenNames2"));
        assertThat(externalDataObject2.getSource(), equalTo("orcid"));
        assertThat(externalDataObject2.getDisplayValue(), equalTo("FamilyName2, GivenNames2"));

        // Metadata assertions
        assertThat(externalDataObject2.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("person")),
                        hasProperty("element", equalTo("familyName")),
                        hasProperty("value", equalTo("FamilyName2"))
                )
        ));
        assertThat(externalDataObject2.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("person")),
                        hasProperty("element", equalTo("givenName")),
                        hasProperty("value", equalTo("GivenNames2"))
                )
        ));
        assertThat(externalDataObject2.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("person")),
                        hasProperty("element", equalTo("email")),
                        hasProperty("value", equalTo("person2primary@email.com"))  // Primary email
                )
        ));
        assertThat(externalDataObject2.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("person")),
                        hasProperty("element", equalTo("identifier")),
                        hasProperty("qualifier", equalTo("orcid")),
                        hasProperty("value", equalTo("0000-0000-0000-0002"))
                )
        ));
        assertThat(externalDataObject2.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("dc")),
                        hasProperty("element", equalTo("identifier")),
                        hasProperty("qualifier", equalTo("uri")),
                        hasProperty("value", equalTo("https://orcid.org/0000-0000-0000-0002"))
                )
        ));
    }


    @Test
    public void testGetExternalDataObjectGetPersonOnlyWithNameFilled() {
        List<ExternalDataObject> optional = dataProvider.searchExternalDataObjects(ORCID_SEARCH_QUERY, 0, 10);

        assertThat(optional, hasSize(3));

        ExternalDataObject externalDataObject2 = optional.get(2); // Test person2 (with two emails)

        // Basic field assertions
        assertThat(externalDataObject2.getId(), equalTo("0000-0000-0000-0003"));
        assertThat(externalDataObject2.getValue(), equalTo("FamilyName3, GivenNames3"));
        assertThat(externalDataObject2.getSource(), equalTo("orcid"));
        assertThat(externalDataObject2.getDisplayValue(), equalTo("FamilyName3, GivenNames3"));

        // Metadata assertions
        assertThat(externalDataObject2.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("person")),
                        hasProperty("element", equalTo("familyName")),
                        hasProperty("value", equalTo("FamilyName3"))
                )
        ));
        assertThat(externalDataObject2.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("person")),
                        hasProperty("element", equalTo("givenName")),
                        hasProperty("value", equalTo("GivenNames3"))
                )
        ));
        assertThat(externalDataObject2.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("person")),
                        hasProperty("element", equalTo("identifier")),
                        hasProperty("qualifier", equalTo("orcid")),
                        hasProperty("value", equalTo("0000-0000-0000-0003"))
                )
        ));
        assertThat(externalDataObject2.getMetadata(), hasItem(
                allOf(
                        hasProperty("schema", equalTo("dc")),
                        hasProperty("element", equalTo("identifier")),
                        hasProperty("qualifier", equalTo("uri")),
                        hasProperty("value", equalTo("https://orcid.org/0000-0000-0000-0003"))
                )
        ));
    }
}
